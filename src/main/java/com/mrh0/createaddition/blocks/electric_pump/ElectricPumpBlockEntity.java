package com.mrh0.createaddition.blocks.electric_pump;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import java.util.List;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.energy.InternalEnergyStorage;
import com.mrh0.createaddition.index.CABlockEntities;
import com.mrh0.createaddition.index.CALang;
import com.mrh0.createaddition.sound.CASoundScapes;
import com.mrh0.createaddition.util.Util;
import com.zurrtum.create.content.fluids.pump.PumpBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.mrh0.createaddition.port.neoforge.capabilities.Capabilities;
import com.mrh0.createaddition.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;
import org.joml.Quaternionf;

public class ElectricPumpBlockEntity extends PumpBlockEntity implements IHaveGoggleInformation {
	public static final float[][] PARTIAL_OFFSETS = {
			{8 / 16f, 4 / 16f, 8 / 16f},  // bottom face at y=2
			{8 / 16f, 12 / 16f, 8 / 16f}  // bottom face at y=10
	};

	public static final float[] PARTIAL_PHASE_OFFSETS = {6f, 0f};

	protected float pumpSpeed;
	protected ScrollValueBehaviour speedBehaviour;
	protected final InternalEnergyStorage energy;
	private final IEnergyStorage capability;

	private boolean active = false;

	// PORT-NOTE(26.1): Create Fly's PumpBlockEntity hardwires its type; CABlockEntities passes ours through BETypeOverride
	public ElectricPumpBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
		energy = new InternalEnergyStorage(CommonConfig.ELECTRIC_PUMP_CAPACITY.get(), CommonConfig.ELECTRIC_PUMP_MAX_INPUT.get(), 0);
		capability = energy;
		setLazyTickRate(20);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.EnergyStorage.BLOCK,
				CABlockEntities.ELECTRIC_PUMP.get(),
				(be, context) -> be.capability
		);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
		super.addBehaviours(behaviours);

		CenteredSideValueBoxTransform slot =
				new CenteredSideValueBoxTransform((pump, side) -> side.getAxis() != pump.getValue(ElectricPumpBlock.FACING).getAxis());

		speedBehaviour = new ScrollValueBehaviour(CreateLang.translateDirect("generic.speed"), this, slot);
		speedBehaviour.between(0, CommonConfig.ELECTRIC_PUMP_RPM_RANGE.get());
		speedBehaviour.value = 32;
		behaviours.add(speedBehaviour);
	}

	protected void applySpeed() {
		float target = active ? pumpSpeed : 0;
		if (target == speed) return;
		float prevSpeed = speed;
		speed = target;
		onSpeedChanged(prevSpeed);

		sendData();
	}

	@Override
	public float getGeneratedSpeed() {
		return active ? pumpSpeed : 0;
	}

	@Override
	public void attachKinetics() {
		updateSpeed = false;
	}

	@Override
	public void setSource(BlockPos source) {
	}

	@Override
	public float calculateStressApplied() {
		return 0f;
	}

	@Override
	public float calculateAddedStressCapacity() {
		return 0f;
	}

	// PORT-NOTE(26.1): called by the client tooltip behaviour (CABlockEntityBehaviours) after Create Fly's kinetic stats
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CALang.builder().add(Component.translatable(CreateAddition.MODID + ".tooltip.energy.consumption").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
		CALang.builder().add(Component.literal(" " + Util.format(getEnergyConsumptionRate(pumpSpeed)) + "⚡/t ")
				.withStyle(ChatFormatting.AQUA).append(CreateLang.translateDirect("gui.goggles.at_current_speed").withStyle(ChatFormatting.DARK_GRAY))).forGoggles(tooltip);
		return true;
	}

	public static int getEnergyConsumptionRate(float speed) {
		float magnitude = Math.abs(speed);
		if (magnitude <= 0) return 0;
		float effectiveSpeed = Math.max(magnitude, 32);
		return (int) Math.max(1, Math.round(CommonConfig.ELECTRIC_PUMP_FE_RPM.get() * effectiveSpeed / 256d));
	}

	@Override
	protected void read(ValueInput tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		energy.read(tag);
		active = tag.getBooleanOr("Active", false);
	}

	@Override
	protected void write(ValueOutput tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		energy.write(tag);
		tag.putBoolean("Active", active);
	}

	@Override
	public void writeSafe(ValueOutput tag) {
		super.writeSafe(tag);
		energy.write(tag);
		tag.putBoolean("Active", active);
	}

	@Override
	public void tick() {
		super.tick();

		float behaviourSpeed = speedBehaviour.getValue();
		if (behaviourSpeed != pumpSpeed) {
			pumpSpeed = behaviourSpeed;
			applySpeed();
		}

		if (level.isClientSide()) return;

		int con = getEnergyConsumptionRate(pumpSpeed);
		boolean powered = getBlockState().getValue(ElectricPumpBlock.POWERED);
		boolean shouldRun = !powered && con > 0 && energy.getEnergyStored() >= con;

		if (shouldRun) energy.internalConsumeEnergy(con);

		if (shouldRun != active) {
			active = shouldRun;
			applySpeed();
		}
	}

	public float getPumpSpeed() {
		return pumpSpeed;
	}

	public int getEnergyConsumption() {
		return getEnergyConsumptionRate(pumpSpeed);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
		applySpeed();
	}

	@Override
	public void tickAudio() {
		if (!active) return;
		if (CommonConfig.AUDIO_ENABLED.get())
			CASoundScapes.play(CASoundScapes.AmbienceGroup.DYNAMO, worldPosition, 1);
	}

	public static float getPulseScale(boolean active, float pumpSpeed, float renderTime, float phaseOffsetTicks) {
		if (!active) return 1f;
		float rate = Math.max(32f, Math.abs(pumpSpeed)) / 32f;
		return 1f + Mth.sin((renderTime + phaseOffsetTicks) * 0.1f * rate) * 0.1f;
	}

	public static Quaternionf getFacingRotation(Direction facing) {
		return new Quaternionf().rotateTo(0, 1, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ());
	}
}