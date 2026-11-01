package com.mrh0.createaddition.blocks.servo_motor;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.mrh0.createaddition.port.minecraft.CAValueIO;

import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.blocks.electric_motor.ElectricMotorBlockEntity;
import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.energy.InternalEnergyStorage;
import com.mrh0.createaddition.index.CABlockEntities;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.index.CALang;
import com.mrh0.createaddition.sound.CASoundScapes;
import com.mrh0.createaddition.util.Util;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.content.contraptions.AssemblyException;
import com.zurrtum.create.content.contraptions.ControlledContraptionEntity;
import com.zurrtum.create.content.contraptions.bearing.BearingContraption;
import com.zurrtum.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.zurrtum.create.AllAdvancements;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.BehaviourType;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettingsHandleBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerKineticScrollValueBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollOptionBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import com.mrh0.createaddition.util.IMultiValueSettings;
import org.jetbrains.annotations.Nullable;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;

import net.minecraft.core.Direction.Axis;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.mrh0.createaddition.port.neoforge.capabilities.Capabilities;
import com.mrh0.createaddition.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;

public class ServoMotorBlockEntity extends MechanicalBearingBlockEntity implements IHaveGoggleInformation, IMultiValueSettings {

	static final BehaviourType<ServoMovementMode> MOVEMENT_MODE_TYPE = new BehaviourType<>();

	// PORT-NOTE(26.1): server halves of the four value settings (Create Fly splits scroll behaviours into a server
	// behaviour and a client value box, see the client CABlockEntityBehaviours). Each needs its own BehaviourType (the
	// SmartBlockEntity map is keyed by type), NBT key and netId. Create Fly only routes ValueSettingsPackets to the
	// ServerScrollValueBehaviour.TYPE behaviour; IMultiValueSettings + AllHandleMixin route the others by netId.
	public static class ServoMovementMode extends ServerScrollOptionBehaviour<RotationMode> {
		ServoMovementMode(SmartBlockEntity be) {
			super(RotationMode.class, be);
		}

		@Override
		public BehaviourType<?> getType() {
			return MOVEMENT_MODE_TYPE;
		}

		@Override
		public int netId() {
			return 1;
		}

		@Override
		public void write(ValueOutput nbt, boolean clientPacket) {
			nbt.putInt("RotationMode", value);
		}

		@Override
		public void read(ValueInput nbt, boolean clientPacket) {
			value = nbt.getIntOr("RotationMode", 0);
		}
	}

	static final BehaviourType<ServoAngleLimit> MIN_ANGLE_TYPE = new BehaviourType<>();
	static final BehaviourType<ServoAngleLimit> MAX_ANGLE_TYPE = new BehaviourType<>();

	// The negated flag controls whether the angle is treated as negative (min side, 0 to -180) or positive
	// (max side, 0 to +180).
	public static class ServoAngleLimit extends ServerScrollValueBehaviour {
		private final BehaviourType<ServoAngleLimit> myType;
		private final int myNetId;
		private final String nbtKey;
		private final int defaultVal;
		public final boolean negated;

		ServoAngleLimit(SmartBlockEntity be, BehaviourType<ServoAngleLimit> type, int netId, String nbtKey, int defaultVal, boolean negated) {
			super(be);
			this.myType = type;
			this.myNetId = netId;
			this.nbtKey = nbtKey;
			this.defaultVal = defaultVal;
			this.negated = negated;
			between(0, 180);
			value = defaultVal;
		}

		/** Returns the signed angle in degrees this limit represents. */
		public float getAngle() {
			return negated ? -value : value;
		}

		void setRaw(int value) {
			this.value = value;
		}

		@Override
		public BehaviourType<?> getType() { return myType; }

		@Override
		public int netId() { return myNetId; }

		@Override
		public void write(ValueOutput nbt, boolean clientPacket) {
			nbt.putInt(nbtKey, value);
		}

		@Override
		public void read(ValueInput nbt, boolean clientPacket) {
			value = nbt.getIntOr(nbtKey, defaultVal);
		}
	}

	@Override
	public @Nullable ValueSettingsHandleBehaviour getValueSettingsBehaviour(int netId) {
		return switch (netId) {
			case 0 -> generatedSpeed;
			case 1 -> (ServoMovementMode) movementMode;
			case 2 -> maxAngle;
			case 3 -> minAngle;
			default -> null;
		};
	}

	protected float motorSpeed;
	protected ServerScrollValueBehaviour generatedSpeed;
	protected ServoAngleLimit minAngle;
	protected ServoAngleLimit maxAngle;
	protected final InternalEnergyStorage energy;
	private final IEnergyStorage energyCapability;

	private boolean active = false;
	private boolean firstTick = true;
	private boolean allowAssemble = false;
	private int lastNetSignal = Integer.MIN_VALUE;
	private float targetAngle = 0;

	public ServoMotorBlockEntity(BlockEntityType<? extends ServoMotorBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		energy = new InternalEnergyStorage(CommonConfig.ELECTRIC_MOTOR_CAPACITY.get(),
				CommonConfig.ELECTRIC_MOTOR_MAX_INPUT.get(), 0);
		energyCapability = energy;
		setLazyTickRate(20);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.EnergyStorage.BLOCK,
				CABlockEntities.SERVO_MOTOR.get(),
				(be, context) -> be.energyCapability);
	}

	// Max angle on the top side, min angle opposite it, movement mode on the remaining two sides.
	// Which side is the top depends on FACING and ROLL, see ServoMotorBlock.getTop.
	public static boolean isModeSide(BlockState state, Direction d) {
		Axis dAxis = d.getAxis();
		return dAxis != state.getValue(ServoMotorBlock.FACING).getAxis()
				&& dAxis != ServoMotorBlock.getTop(state).getAxis();
	}

	public static boolean isMaxAngleSide(BlockState state, Direction d) {
		return d == ServoMotorBlock.getTop(state);
	}

	public static boolean isMinAngleSide(BlockState state, Direction d) {
		return d == ServoMotorBlock.getTop(state).getOpposite();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
		super.addBehaviours(behaviours);

		// super's movementMode uses ScrollValueBehaviour.TYPE same as generatedSpeed's type,
		// so one would silently overwrite the other in SmartBlockEntity's type→behaviour map.
		// Replace it in-place with our ServoMovementMode that has its own distinct type.
		for (int i = 0; i < behaviours.size(); i++) {
			if (behaviours.get(i) == movementMode) {
				movementMode = new ServoMovementMode(this);
				behaviours.set(i, movementMode);
				break;
			}
		}

		generatedSpeed = new ServerKineticScrollValueBehaviour(this);
		generatedSpeed.between(-CommonConfig.ELECTRIC_MOTOR_RPM_RANGE.get(),
				CommonConfig.ELECTRIC_MOTOR_RPM_RANGE.get());
		generatedSpeed.setValue(32);
		generatedSpeed.withCallback(rpm -> {
			motorSpeed = rpm;
			updateGeneratedRotation();
		});
		behaviours.add(generatedSpeed);

		// Max angle: 0 to +180°, shown on the top side. Default 90°.
		maxAngle = new ServoAngleLimit(this, MAX_ANGLE_TYPE, 2, "MaxAngle", 90, false);
		behaviours.add(maxAngle);

		// Min angle: 0 to -180°, shown opposite the top side. Default -90° (stored as 90).
		minAngle = new ServoAngleLimit(this, MIN_ANGLE_TYPE, 3, "MinAngle", 90, true);
		behaviours.add(minAngle);
	}

	// When assembled, delegate directly to the contraption entity's own lerp so the disc
	// is always in perfect sync with the rotating contraption blocks.
	// Returns the static angle when not assembled disc stays fixed before assembly.
	@Override
	public float getInterpolatedAngle(float partialTicks) {
		if (!running) return toContraptionAngle(angle);
		if (movedContraption != null)
			return movedContraption.getAngle(partialTicks + 1f);
		return toContraptionAngle(angle);
	}

	// Contraptions and the disc rotate around the positive direction of the axis, while the servo's
	// angles are relative to its front, so they are flipped for negative facings (north, west, down).
	private float toContraptionAngle(float servoAngle) {
		BlockState state = getBlockState();
		if (!state.hasProperty(ServoMotorBlock.FACING)) return servoAngle;
		return state.getValue(ServoMotorBlock.FACING).getAxisDirection() == Direction.AxisDirection.POSITIVE
				? servoAngle : -servoAngle;
	}

	@Override
	protected void applyRotation() {
		if (movedContraption == null) return;
		movedContraption.setAngle(toContraptionAngle(angle));
		BlockState state = getBlockState();
		if (state.hasProperty(ServoMotorBlock.FACING))
			movedContraption.setRotationAxis(state.getValue(ServoMotorBlock.FACING).getAxis());
	}

	// When the contraption is assembled, the angle is set directly by the redstone
	// signal every tick continuous spinning is not used.
	@Override
	public float getAngularSpeed() {
		if (running) return 0;
		return super.getAngularSpeed();
	}

	@Override
	public float getGeneratedSpeed() {
		if (!CABlocks.SERVO_MOTOR.has(getBlockState())) return 0;
		return convertToDirection(active ? motorSpeed : 0, getBlockState().getValue(ServoMotorBlock.FACING));
	}

	@Override
	public float calculateAddedStressCapacity() {
		float capacity = CommonConfig.MAX_STRESS.get() / 256f;
		this.lastCapacityProvided = capacity;
		return capacity;
	}

	@Override
	protected Block getStressConfigKey() {
		return CABlocks.SERVO_MOTOR.get();
	}

	@Override
	public void assemble() {
		// MechanicalBearingBlockEntity.tick() calls assemble() whenever assembleNextTick is set,
		// which happens on every kinetic speed change. Guard here so the contraption only builds
		// on an explicit right-click (triggerAssemble sets allowAssemble before setting the flag).
		if (!allowAssemble) return;
		allowAssemble = false;

		if (!(level.getBlockState(worldPosition).getBlock() instanceof ServoMotorBlock))
			return;

		Direction direction = getBlockState().getValue(ServoMotorBlock.FACING);
		BearingContraption contraption = new BearingContraption(false, direction);
		try {
			if (!contraption.assemble(level, worldPosition))
				return;
			lastException = null;
		} catch (AssemblyException e) {
			lastException = e;
			sendData();
			return;
		}

		contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
		movedContraption = ControlledContraptionEntity.create(level, this, contraption);
		BlockPos anchor = worldPosition.relative(direction);
		movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
		movedContraption.setRotationAxis(direction.getAxis());
		level.addFreshEntity(movedContraption);

		AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

		if (contraption.containsBlockBreakers())
			award(AllAdvancements.CONTRAPTION_ACTORS);

		running = true;
		angle = 0;
		lastNetSignal = Integer.MIN_VALUE; // force tick() to re-evaluate redstone on next tick
		sendData();
		updateGeneratedRotation();
	}

	@Override
	public void tick() {
		if (firstTick) {
			motorSpeed = generatedSpeed.getValue();
			firstTick = false;
		}

		if (!level.isClientSide()) {
			int con = ElectricMotorBlockEntity.getEnergyConsumptionRate(motorSpeed);
			if (!running) {
				// No energy consumed when not assembled; activate whenever the buffer is sufficient
				boolean hasEnergy = energy.getEnergyStored() >= con;
				if (active != hasEnergy) {
					active = hasEnergy;
					updateGeneratedRotation();
				}
			} else {
				// Assembled: full rate when the angle is stepping, 10% when stationary at target
				boolean moving = Math.abs(targetAngle - angle) > 0.001f && Math.abs(motorSpeed) > 0f;
				int consumption = moving ? con : con / 10;
				if (!active) {
					if (energy.getEnergyStored() >= con) {
						active = true;
						updateGeneratedRotation();
					}
				} else {
					if (energy.internalConsumeEnergy(consumption) < consumption) {
						active = false;
						updateGeneratedRotation();
					}
				}
			}
		}

		super.tick();

		// Net redstone = maxFaceSignal - minFaceSignal, range -15 to +15.
		// Net 0 → center (angle 0), positive → toward maxAngle, negative → toward minAngle.
		if (!level.isClientSide() && active) {
			Direction maxFace = ServoMotorBlock.getTop(getBlockState());
			Direction minFace = maxFace.getOpposite();
			int maxSignal = level.getSignal(worldPosition.relative(maxFace), maxFace);
			int minSignal = level.getSignal(worldPosition.relative(minFace), minFace);
			int net = maxSignal - minSignal;
			if (net != lastNetSignal) {
				lastNetSignal = net;
				// minAngle.getAngle() already returns a negative value (negated=true)
				targetAngle = net >= 0
					? (net / 15.0f) * maxAngle.getAngle()
					: (-net / 15.0f) * minAngle.getAngle();
				// Auto-assemble on non-zero signal, auto-disassemble to center handled below
				if (net != 0 && !running)
					triggerAssemble();
				sendData();
			}

			// Step angle and drive contraption only when assembled
			if (running) {
				float diff = targetAngle - angle;
				float degreesPerTick = Math.abs(motorSpeed) * 360f / 1200f;
				if (Math.abs(diff) > 0.001f && degreesPerTick > 0f) {
					angle += Math.signum(diff) * Math.min(Math.abs(diff), degreesPerTick);
					applyRotation();
					sendData();
				}
				int mode = movementMode.getValue();
				if (mode == RotationMode.ROTATE_PLACE.ordinal()) {
					// "Place when stopped": snap to nearest 90° and disassemble at min/max limit
					if (Math.abs(angle - maxAngle.getAngle()) < 0.1f
							|| Math.abs(angle - minAngle.getAngle()) < 0.1f) {
						angle = Math.round(angle / 90f) * 90f;
						applyRotation();
						disassemble();
					}
				} else if (mode == RotationMode.ROTATE_PLACE_RETURNED.ordinal()) {
					// "Place near initial angle": disassemble only when back near 0°
					if (Math.abs(angle) < 0.1f)
						disassemble();
				}
			}
		}
	}

	// PORT-NOTE(26.1): called by the client audio behaviour (CABlockEntityBehaviours) after Create Fly's kinetic audio
	public void tickAudio() {
		if (!active || !running || getAngularSpeed() == 0) return;
		if (CommonConfig.AUDIO_ENABLED.get())
			CASoundScapes.play(CASoundScapes.AmbienceGroup.DYNAMO, worldPosition, 1);
	}

	// PORT-NOTE(26.1): called by the client tooltip behaviour (CABlockEntityBehaviours) after Create Fly's kinetic stats
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CALang.builder()
				.add(Component.translatable(CreateAddition.MODID + ".tooltip.energy.consumption")
						.withStyle(ChatFormatting.GRAY))
				.forGoggles(tooltip);
		CALang.builder()
				.add(Component.literal(
						" " + Util.format(ElectricMotorBlockEntity.getEnergyConsumptionRate(generatedSpeed.getValue()))
								+ "⚡/t ")
						.withStyle(ChatFormatting.AQUA)
						.append(CreateLang.translateDirect("gui.goggles.at_current_speed")
								.withStyle(ChatFormatting.DARK_GRAY)))
				.forGoggles(tooltip);
		return true;
	}

	@Override
	public void write(ValueOutput compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		if (!clientPacket)
			energy.write(compound);
		compound.putBoolean("servo_active", active);
		compound.putFloat("TargetAngle", targetAngle);
	}

	@Override
	protected void read(ValueInput compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (!clientPacket)
			energy.read(compound);
		active = compound.getBooleanOr("servo_active", false);
		if (CAValueIO.contains(compound, "TargetAngle"))
			targetAngle = compound.getFloatOr("TargetAngle", 0f);
		// The parent's client-side read resets angle = angleBefore so kinetic-speed
		// extrapolation works for normal bearings. We use explicit server-sent angles, so
		// restore the actual server angle for correct applyRotation() calls each client tick.
		if (clientPacket && running)
			angle = compound.getFloatOr("Angle", 0f);
	}

	public void triggerAssemble() {
		allowAssemble = true;
		assembleNextTick = true;
	}

	public float getRPM() {
		return motorSpeed;
	}

	public void setRPM(float rpm) {
		if (!Float.isFinite(rpm)) return;
		int clamped = (int) Math.max(-CommonConfig.ELECTRIC_MOTOR_RPM_RANGE.get(),
				Math.min(CommonConfig.ELECTRIC_MOTOR_RPM_RANGE.get(), rpm));
		generatedSpeed.setValue(clamped);
		motorSpeed = clamped;
		updateGeneratedRotation();
		sendData();
	}

	public float getCurrentAngle() {
		return angle;
	}

	public float getTargetAngle() {
		return targetAngle;
	}

	public float getMinAngleDegrees() {
		return minAngle.getAngle();
	}

	public void setMinAngleDegrees(int degrees) {
		minAngle.setRaw(Math.max(0, Math.min(180, Math.abs(degrees))));
		sendData();
	}

	public float getMaxAngleDegrees() {
		return maxAngle.getAngle();
	}

	public void setMaxAngleDegrees(int degrees) {
		maxAngle.setRaw(Math.max(0, Math.min(180, Math.abs(degrees))));
		sendData();
	}

	public int getEnergyConsumption() {
		return ElectricMotorBlockEntity.getEnergyConsumptionRate(motorSpeed);
	}
}
