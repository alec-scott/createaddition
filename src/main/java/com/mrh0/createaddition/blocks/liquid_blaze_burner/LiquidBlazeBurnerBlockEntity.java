package com.mrh0.createaddition.blocks.liquid_blaze_burner;

import com.mrh0.createaddition.port.minecraft.CAValueIO;

import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import java.util.List;
import java.util.Optional;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.index.CABlockEntities;
import com.mrh0.createaddition.index.CALang;
import com.mrh0.createaddition.index.CARecipes;
import com.mrh0.createaddition.network.IObserveBlockEntity;
import com.mrh0.createaddition.network.ObservePacketPayload;
import com.mrh0.createaddition.network.TimeRemainingPacketPayload;
import com.mrh0.createaddition.recipe.FluidRecipeWrapper;
import com.mrh0.createaddition.recipe.liquid_burning.LiquidBurningRecipe;
import com.mrh0.createaddition.util.Util;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.AllItemTags;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.content.fluids.tank.FluidTankBlock;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.infrastructure.fluids.FluidItemInventory;
import net.minecraft.server.level.ServerLevel;

import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import com.mrh0.createaddition.port.neoforge.capabilities.Capabilities;
import com.mrh0.createaddition.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import static com.zurrtum.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public class LiquidBlazeBurnerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IObserveBlockEntity {
	public static int MAX_HEAT_CAPACITY = CommonConfig.LIQUID_BLAZE_BURNER_MAX_HEAT_CAPACITY.get();

	protected FuelType activeFuel;
	protected int remainingBurnTime;
	protected LerpedFloat headAnimation;
	protected LerpedFloat headAngle;
	protected boolean isCreative;
	protected boolean goggles;
	protected boolean hat;
	public final boolean stockKeeper = false;
	BlazeBurnerBlock.HeatLevel heatLevel;

	public LiquidBlazeBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		activeFuel = FuelType.NONE;
		remainingBurnTime = 0;
		headAnimation = LerpedFloat.linear();
		headAngle = LerpedFloat.angular();
		isCreative = false;
		goggles = false;

		headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(LiquidBlazeBurnerBlock.FACING)
			.orElse(Direction.SOUTH)) + 180) % 360);

		tankInventory = createInventory();
		heatLevel = getHeatLevelFromBlock();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour<?>> list) {

	}

	public BlazeBurnerBlock.HeatLevel getHeatLevelForRender() {
		return getHeatLevelFromBlock();
	}

	// Custom fluid handling
	protected FluidTank tankInventory;

	private Optional<RecipeHolder<LiquidBurningRecipe>> recipeCache = Optional.empty();
	private Fluid lastFluid = null;

	public static void registerCapability(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.FluidHandler.BLOCK,
				CABlockEntities.LIQUID_BLAZE_BURNER.get(),
				(be, direction) -> be.tankInventory
		);
	}

	// PORT-NOTE(26.1): Create Fly fluid tank, amounts in droplets (81 per mB); markDirty replaces SmartFluidTank's callback
	protected FluidTank createInventory() {
		return new FluidTank(CommonConfig.LIQUID_BLAZE_BURNER_MAX_LIQUID_CAPACITY.get() * MB) {
			@Override
			public void markDirty() {
				onFluidStackChanged(getFluid());
			}
		};
	}

	private static final int MB = 81;

	protected void onFluidStackChanged(FluidStack newFluidStack) {

		if (!hasLevel()) return;
		update(newFluidStack);
	}

	private void update(FluidStack stack) {
		if (level == null) return;
		if (level.isClientSide()) return;
		if (stack.getFluid() != lastFluid) recipeCache = find(stack, level);
		lastFluid = stack.getFluid();
	}

	public Optional<RecipeHolder<LiquidBurningRecipe>> find(@Nullable FluidStack stack,@Nullable Level level) {
		if (stack == null || level == null) return Optional.empty();
		if (CARecipes.LIQUID_BURNING_TYPE == null) return Optional.empty();
		FluidRecipeWrapper input = new FluidRecipeWrapper(new FluidStack(stack.getFluid(), 1000 * MB));
		if (level instanceof ServerLevel serverLevel) return serverLevel.recipeAccess().getRecipeFor(CARecipes.LIQUID_BURNING_TYPE, input, level);
		// The client has to agree when a bucket is clicked on the burner, or it goes on to place the bucket's fluid
		return level.recipeAccess().getSynchronizedRecipes().getFirstMatch(CARecipes.LIQUID_BURNING_TYPE, input, level);
	}

	public boolean first = true;
	public void burningTick() {
		if (level == null) return;
		if (level.isClientSide()) return;

		if (first) update(tankInventory.getFluid());
		first = false;

		if (remainingBurnTime < 1)
			if (recipeCache.isEmpty()) return;

		if (tankInventory.getFluid().getAmount() < 100 * MB) return;
		if (remainingBurnTime > MAX_HEAT_CAPACITY) return;
		if (recipeCache.isEmpty()) return;

		// Added try catch because this crashes for some reason for a minority of players, very strange.
		try {
			var recipe = recipeCache.get().value();
			var burnTime = recipe.getBurnTime() / 10;
			var fuelType = recipe.isSuperheated() ? FuelType.SPECIAL : FuelType.NORMAL;
			remainingBurnTime = activeFuel == fuelType ? remainingBurnTime + burnTime : burnTime;
			activeFuel = fuelType;
		}
		catch(Exception e) {
			e.printStackTrace();
			return;
		}
		tankInventory.extract(tankInventory.getFluid().copyWithAmount(100 * MB));

		BlazeBurnerBlock.HeatLevel prev = heatLevel;
		playSound();
		updateBlockState();

		if (prev != heatLevel) {
			level.playSound(null, worldPosition, SoundEvents.BLAZE_AMBIENT, SoundSource.BLOCKS,
				.125f + level.getRandom().nextFloat() * .125f, 1.15f - level.getRandom().nextFloat() * .25f);

			spawnParticleBurst(activeFuel == FuelType.SPECIAL);
		}
	}


	public FuelType getActiveFuel() {
		return activeFuel;
	}

	public int getRemainingBurnTime() {
		return remainingBurnTime;
	}

	public boolean isCreative() {
		return isCreative;
	}

	@Override
	public void tick() {
		super.tick();

		if (level == null) return;
		if (level.isClientSide()) {
			tickAnimation();
			if (!isVirtual()) spawnParticles(getHeatLevelForRender(), 1);
			return;
		}

		burningTick();

		if (isCreative) return;

		if (remainingBurnTime > 0) remainingBurnTime--;

		// if (activeFuel == FuelType.NORMAL) updateBlockState();
		if (remainingBurnTime > 0) return;

		if (activeFuel == FuelType.SPECIAL) {
			activeFuel = FuelType.NORMAL;
			remainingBurnTime = MAX_HEAT_CAPACITY / 2;
		} else activeFuel = FuelType.NONE;

		updateBlockState();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
	}
    void tickAnimation() {
		var serverHeatLevel = getHeatLevelForRender();
		boolean active = serverHeatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && isValidBlockAbove();

		if (!active) {
			float target = 0;
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && !player.isInvisible()) {
				double x;
				double z;
				if (isVirtual()) {
					x = -4;
					z = -10;
				} else {
					// On a Sable sub-level the burner sits in plot space: look at the player in that frame.
					net.minecraft.world.phys.Vec3 eye = com.mrh0.createaddition.port.sable.SableUtil
							.transformRelativeTo(level, getBlockPos(), player.position(), 1f);
					x = eye.x;
					z = eye.z;
				}
				double dx = x - (getBlockPos().getX() + 0.5);
				double dz = z - (getBlockPos().getZ() + 0.5);
				target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
			}
			target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
			headAngle.chase(target, .25f, LerpedFloat.Chaser.exp(5));
			headAngle.tickChaser();
		} else {
			headAngle.chase((AngleHelper.horizontalAngle(getBlockState().getOptionalValue(LiquidBlazeBurnerBlock.FACING)
				.orElse(Direction.SOUTH)) + 180) % 360, .125f, LerpedFloat.Chaser.EXP);
			headAngle.tickChaser();
		}

		headAnimation.chase(active ? 1 : 0, .25f, LerpedFloat.Chaser.exp(.25f));
		headAnimation.tickChaser();
	}

	@Override
	public void write(ValueOutput tag, boolean clientPacket) {
		if (!isCreative) {
			tag.putInt("fuelLevel", activeFuel.ordinal());
			tag.putInt("burnTimeRemaining", remainingBurnTime);
		} else tag.putBoolean("isCreative", true);
		if (goggles) tag.putBoolean("Goggles", true);
		if (hat) tag.putBoolean("TrainHat", true);
		tankInventory.write(tag.child("TankContent"));
		super.write(tag, clientPacket);
	}

	@Override
	protected void read(ValueInput tag, boolean clientPacket) {
		activeFuel = FuelType.values()[tag.getIntOr("fuelLevel", 0)];
		remainingBurnTime = tag.getIntOr("burnTimeRemaining", 0);
		isCreative = tag.getBooleanOr("isCreative", false);
		goggles = CAValueIO.contains(tag, "Goggles");
		hat = CAValueIO.contains(tag, "TrainHat");
		tankInventory.read(tag.childOrEmpty("TankContent"));
		super.read(tag, clientPacket);
	}

	public BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock() {
		return LiquidBlazeBurnerBlock.getHeatLevelOf(getBlockState());
	}

	public void updateBlockState() {
		setBlockHeat(getHeatLevelFromFuelType(activeFuel));
	}

	protected void setBlockHeat(BlazeBurnerBlock.HeatLevel heat) {
		if (level == null) return;
		if (heatLevel == heat) return;
		heatLevel = heat;
		level.setBlockAndUpdate(worldPosition, getBlockState().setValue(HEAT_LEVEL, heat));
		notifyUpdate();
	}

	/**
	 * PORT-NOTE(26.1): returns the emptied container, or null if nothing was accepted (was NeoForge's FluidActionResult).
	 * Uses Create Fly's item fluid inventory; amounts are droplets.
	 */
	@Nullable
	protected ItemStack tryEmptyFluidContainer(ItemStack itemStack, boolean simulate) {
		if (isCreative || level == null) return null;
		// Drained for real even when simulating, it is a copy and getContainer() needs to see the change
		try (FluidItemInventory itemHandler = FluidHelper.getFluidInventory(itemStack.copyWithCount(1))) {
			if (itemHandler == null) return null;
			FluidStack contained = itemHandler.countAny();
			if (contained.isEmpty() || find(contained, level).isEmpty()) return null;

			int accepted = tankInventory.countSpace(contained);
			if (accepted <= 0) return null;
			// Buckets can only be emptied whole, so this fails if the tank has less than a bucket of space
			int drainedAmount = itemHandler.extract(contained, accepted);
			if (drainedAmount <= 0) return null;
			FluidStack drained = contained.copyWithAmount(drainedAmount);

			if (!simulate && !level.isClientSide()) {
				tankInventory.insert(drained);
				level.playSound(null, getBlockPos(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, .125f + level.getRandom().nextFloat() * .125f, .75f - level.getRandom().nextFloat() * .25f);
			}
			return itemHandler.getContainer();
		}
	}

	protected boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate) {
		if (isCreative) return false;

		FuelType newFuel = FuelType.NONE;
		int newBurnTime;

		if (itemStack.is(AllItemTags.BLAZE_BURNER_FUEL_SPECIAL)) {
			newBurnTime = 3200;
			newFuel = FuelType.SPECIAL;
		} else {
			newBurnTime = level == null ? 0 : level.fuelValues().burnDuration(itemStack);
			if (newBurnTime > 0)
				newFuel = FuelType.NORMAL;
			else if (itemStack.is(AllItemTags.BLAZE_BURNER_FUEL_REGULAR)) {
				newBurnTime = 1600; // Same as coal
				newFuel = FuelType.NORMAL;
			}
		}

		if (newFuel == FuelType.NONE) return false;
		if (newFuel.ordinal() < activeFuel.ordinal()) return false;
		if (activeFuel == FuelType.SPECIAL && remainingBurnTime > 20) return false;

		if (newFuel == activeFuel) {
			if (remainingBurnTime + newBurnTime > MAX_HEAT_CAPACITY && !forceOverflow) return false;
			newBurnTime = Mth.clamp(remainingBurnTime + newBurnTime, 0, MAX_HEAT_CAPACITY);
		}

		if (simulate) return true;

		activeFuel = newFuel;
		remainingBurnTime = newBurnTime;

		if (level == null) return false;
		if (level.isClientSide()) {
			spawnParticleBurst(activeFuel == FuelType.SPECIAL);
			return true;
		}

		BlazeBurnerBlock.HeatLevel prev = heatLevel;
		playSound();
		updateBlockState();

		if (prev != heatLevel)
			level.playSound(null, worldPosition, SoundEvents.BLAZE_AMBIENT, SoundSource.BLOCKS,
				.125f + level.getRandom().nextFloat() * .125f, 1.15f - level.getRandom().nextFloat() * .25f);

		return true;
	}

	protected void applyCreativeFuel() {
		activeFuel = FuelType.NONE;
		remainingBurnTime = 0;
		isCreative = true;

		BlazeBurnerBlock.HeatLevel next = heatLevel.nextActiveLevel();

		if (level.isClientSide()) {
			spawnParticleBurst(next.isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING));
			return;
		}

		playSound();
		if (next == BlazeBurnerBlock.HeatLevel.FADING)
			next = next.nextActiveLevel();
		setBlockHeat(next);
	}

	public boolean isCreativeFuel(ItemStack stack) {
		return stack.is(AllItems.CREATIVE_BLAZE_CAKE);
	}

	public boolean isValidBlockAbove() {
		BlockState blockState = level.getBlockState(worldPosition.above());
		return blockState.is(AllBlocks.BASIN) || blockState.getBlock() instanceof FluidTankBlock;
	}

	protected void playSound() {
		level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS,
			.125f + level.getRandom().nextFloat() * .125f, .75f - level.getRandom().nextFloat() * .25f);
	}

	protected BlazeBurnerBlock.HeatLevel getHeatLevelFromFuelType(FuelType fuel) {
		BlazeBurnerBlock.HeatLevel level = BlazeBurnerBlock.HeatLevel.SMOULDERING;
		switch (activeFuel) {
			case SPECIAL:
				level = BlazeBurnerBlock.HeatLevel.SEETHING;
				break;
			case NORMAL:
				boolean lowPercent = (double) remainingBurnTime / MAX_HEAT_CAPACITY < 0.0125;
				level = lowPercent ? BlazeBurnerBlock.HeatLevel.FADING : BlazeBurnerBlock.HeatLevel.KINDLED;
				break;
			case NONE:
			default:
				break;
		}
		return level;
	}

	protected void spawnParticles(BlazeBurnerBlock.HeatLevel heatLevel, double burstMult) {
		if (level == null) return;
		if (heatLevel == BlazeBurnerBlock.HeatLevel.NONE) return;

		RandomSource r = level.getRandom();

		Vec3 c = VecHelper.getCenterOf(worldPosition);
		Vec3 v = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .125f)
			.multiply(1, 0, 1));

		if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, v.x, v.y, v.z, 0, 0, 0);
		if (r.nextInt(2) != 0) return;

		boolean empty = level.getBlockState(worldPosition.above())
			.getCollisionShape(level, worldPosition.above())
			.isEmpty();

		double yMotion = empty ? .0625f : r.nextDouble() * .0125f;
		Vec3 v2 = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .5f)
			.multiply(1, .25f, 1)
			.normalize()
			.scale((empty ? .25f : .5) + r.nextDouble() * .125f))
			.add(0, .5, 0);

		if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING)) {
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
		} else if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) {
			level.addParticle(ParticleTypes.FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
		}
    }

	public void spawnParticleBurst(boolean soulFlame) {
		Vec3 c = VecHelper.getCenterOf(worldPosition);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 20; i++) {
			Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, r, .5f)
				.multiply(1, .25f, 1)
				.normalize();
			Vec3 v = c.add(offset.scale(.5 + r.nextDouble() * .125f))
				.add(0, .125, 0);
			Vec3 m = offset.scale(1 / 32f);

			level.addParticle(soulFlame ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, v.x, v.y, v.z, m.x, m.y, m.z);
		}
	}

	public enum FuelType {
		NONE, NORMAL, SPECIAL
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if (level == null) return false;
		ObservePacketPayload.send(worldPosition, 0);
		containedFluidTooltip(tooltip, isPlayerSneaking, tankInventory);
		if (TimeRemainingPacketPayload.clientTimeRemaining > 20) CALang.builder().add(Component.literal(" ").append(Component.translatable(CreateAddition.MODID + ".tooltip.liquid_burning.time_remaining").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(" " + Util.formatTime(TimeRemainingPacketPayload.clientTimeRemaining)).withStyle(ChatFormatting.AQUA))).forGoggles(tooltip);
		return true;
	}

	@Override
	public void onObserved(ServerPlayer player, ObservePacketPayload pack) {
		TimeRemainingPacketPayload.send(remainingBurnTime, player);
	}
}
