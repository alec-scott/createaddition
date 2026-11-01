package com.mrh0.createaddition.blocks.tesla_coil;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.energy.AbstractElectricBlockEntity;
import com.mrh0.createaddition.index.*;
import com.mrh0.createaddition.network.IObserveBlockEntity;
import com.mrh0.createaddition.network.ObservePacketPayload;
import com.mrh0.createaddition.network.TimeRemainingPacketPayload;
import com.mrh0.createaddition.recipe.charging.ChargingRecipe;
import com.mrh0.createaddition.sound.CASoundScapes;
import com.mrh0.createaddition.util.Util;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.zurrtum.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.zurrtum.create.content.kinetics.belt.transport.TransportedItemStack;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.mrh0.createaddition.port.neoforge.capabilities.Capabilities;
import com.mrh0.createaddition.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TeslaCoilBlockEntity extends AbstractElectricBlockEntity implements IHaveGoggleInformation, IObserveBlockEntity {

	private Optional<RecipeHolder<ChargingRecipe>> recipeCache = Optional.empty();

	// PORT-NOTE(26.1): the last stack looked up (was a one-slot ItemStackHandler wrapped for recipe lookup)
	private ItemStack lastInput = ItemStack.EMPTY;
	private int chargeAccumulator;
	protected int poweredTimer = 0;

	public TeslaCoilBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
		super(tileEntityTypeIn, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.EnergyStorage.BLOCK,
				CABlockEntities.TESLA_COIL.get(),
				(be, context) -> be.localEnergy
		);
	}

	@Override
	public int getCapacity() {
		return Util.max(CommonConfig.TESLA_COIL_CAPACITY.get(), CommonConfig.TESLA_COIL_CHARGE_RATE.get(), CommonConfig.TESLA_COIL_RECIPE_CHARGE_RATE.get());
	}

	@Override
	public int getMaxIn() {
		return CommonConfig.TESLA_COIL_MAX_INPUT.get();
	}

	@Override
	public int getMaxOut() {
		return 0;
	}

	public BeltProcessingBehaviour processingBehaviour;

	@Override
	public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
		super.addBehaviours(behaviours);
		processingBehaviour =
			new BeltProcessingBehaviour(this).whenItemEnters((s, i) -> TeslaCoilBeltCallbacks.onItemReceived(s, i, this))
				.whileItemHeld((s, i) -> TeslaCoilBeltCallbacks.whenItemHeld(s, i, this));
		behaviours.add(processingBehaviour);
	}

	@Override
	public boolean isEnergyInput(Direction side) {
		return side != getBlockState().getValue(TeslaCoilBlock.FACING).getOpposite();
	}

	@Override
	public boolean isEnergyOutput(Direction side) {
		return false;
	}

	public int getConsumption() {
		return CommonConfig.TESLA_COIL_CHARGE_RATE.get();
	}

	protected float getItemCharge(IEnergyStorage energy) {
		if (energy == null) return 0f;
		return (float) energy.getEnergyStored() / (float) energy.getMaxEnergyStored();
	}

	protected BeltProcessingBehaviour.ProcessingResult onCharge(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return chargeCompoundAndStack(transported, handler);
	}

	private void doDmg() {
		localEnergy.internalConsumeEnergy(CommonConfig.TESLA_COIL_HURT_ENERGY_REQUIRED.get());
		BlockPos origin = getBlockPos().relative(getBlockState().getValue(TeslaCoilBlock.FACING).getOpposite());
		List<LivingEntity> ents = getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(origin).inflate(CommonConfig.TESLA_COIL_HURT_RANGE.get()));
		boolean zapped = false;
		for(LivingEntity e : ents) {
			if(e == null) return;

			boolean allChain = true;
			for(EquipmentSlot slot : EquipmentSlot.values()) {
				if(slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
				ItemStack armor = e.getItemBySlot(slot);
				if(armor.is(Items.CHAINMAIL_BOOTS)) continue;
				if(armor.is(Items.CHAINMAIL_LEGGINGS)) continue;
				if(armor.is(Items.CHAINMAIL_CHESTPLATE)) continue;
				if(armor.is(Items.CHAINMAIL_HELMET)) continue;
				allChain = false;
				break;
			}
			if(allChain) continue;

			int dmg = CommonConfig.TESLA_COIL_HURT_DMG_MOB.get();
			int time = CommonConfig.TESLA_COIL_HURT_EFFECT_TIME_MOB.get();
			if(e instanceof Player) {
				dmg = CommonConfig.TESLA_COIL_HURT_DMG_PLAYER.get();
				time = CommonConfig.TESLA_COIL_HURT_EFFECT_TIME_PLAYER.get();
			}

			if(dmg > 0) {
				if (level instanceof ServerLevel serverLevel) e.hurtServer(serverLevel, CADamageTypes.teslaCoil(level), dmg);
				if (!zapped) {
					if (CommonConfig.AUDIO_ENABLED.get()) level.playSound(null, worldPosition, CASounds.LOUD_ZAP.get(), SoundSource.BLOCKS, 0.6f, 1f);
					zapped = true;
				}
			}
			if(time > 0) e.addEffect(new MobEffectInstance(CAEffects.SHOCKING, time));
		}
	}

	int dmgTick = 0;
	int zapTimer = 200;

	@Override
	public void tick() {
		super.tick();
		if(level == null) return;

		// PORT-NOTE(26.1): client audio is a client behaviour (CABlockEntityBehaviours) calling tickAudio
		if (level.isClientSide()) return;
		int signal = level.getBestNeighborSignal(getBlockPos());
		if(signal > 0 && localEnergy.getEnergyStored() >= CommonConfig.TESLA_COIL_HURT_ENERGY_REQUIRED.get()) poweredTimer = 10;

		dmgTick++;
		if((dmgTick%= CommonConfig.TESLA_COIL_HURT_FIRE_COOLDOWN.get()) == 0 && localEnergy.getEnergyStored() >= CommonConfig.TESLA_COIL_HURT_ENERGY_REQUIRED.get() && signal > 0) doDmg();

		if(poweredTimer > 0) {
			if (zapTimer == 0) {
				if (CommonConfig.AUDIO_ENABLED.get()) level.playSound(null, worldPosition, CASounds.LITTLE_ZAP.get(), SoundSource.BLOCKS, 0.1f, 1f);
				zapTimer = level.getRandom().nextInt(100, 300);
			}
			zapTimer--;

			if(!isPoweredState()) CABlocks.TESLA_COIL.get().setPowered(level, getBlockPos(), true);
			poweredTimer--;
		}
		else if(isPoweredState()) CABlocks.TESLA_COIL.get().setPowered(level, getBlockPos(), false);
	}
	public void tickAudio() {
		if (!isPoweredState()) return;
		if (CommonConfig.AUDIO_ENABLED.get()) CASoundScapes.play(CASoundScapes.AmbienceGroup.TESLA, worldPosition, 1f);
	}

	public boolean isPoweredState() {
		return getBlockState().getValue(TeslaCoilBlock.POWERED);
	}

	protected BeltProcessingBehaviour.ProcessingResult chargeCompoundAndStack(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {

		ItemStack stack = transported.stack;
		if(stack == null) return BeltProcessingBehaviour.ProcessingResult.PASS;
		if(chargeStack(stack, transported, handler)) {
			poweredTimer = 10;
			return BeltProcessingBehaviour.ProcessingResult.HOLD;
		}
		else if(chargeRecipe(stack, transported, handler)) {
			if (energyRemoved > 0) poweredTimer = 10;
			return BeltProcessingBehaviour.ProcessingResult.HOLD;
		}
		return BeltProcessingBehaviour.ProcessingResult.PASS;
	}

	protected boolean chargeStack(ItemStack stack, TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
		IEnergyStorage es = Capabilities.getCapability(stack, Capabilities.EnergyStorage.ITEM);
		if (es == null) return false;
		if(es.receiveEnergy(1, true) != 1) return false;
		if(localEnergy.getEnergyStored() < stack.getCount()) return false;
		localEnergy.internalConsumeEnergy(es.receiveEnergy(Math.min(getConsumption(), localEnergy.getEnergyStored()), false));
		return true;
	}

	private int energyRemoved = 0;
	private final int[] chargeRateHistory = new int[20];
	private int chargeRateIndex = 0;
	private int chargeRateSamples = 0;

	private boolean chargeRecipe(ItemStack stack, TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
		if(this.getLevel() == null) return false;
		// Compare components too, every sequenced assembly step uses the same transitional item
		if(!ItemStack.isSameItemSameComponents(lastInput, stack)) {
			lastInput = stack.copy();
			recipeCache = find(stack, this.getLevel());
			chargeAccumulator = 0;
			Arrays.fill(chargeRateHistory, 0);
			chargeRateIndex = 0;
			chargeRateSamples = 0;
		}
		if(recipeCache.isPresent()) {
			ChargingRecipe recipe = recipeCache.get().value();
			energyRemoved = localEnergy.internalConsumeEnergy(Util.min(CommonConfig.TESLA_COIL_RECIPE_CHARGE_RATE.get(), recipe.getEnergy() - chargeAccumulator, recipe.getMaxChargeRate()));
			chargeRateHistory[chargeRateIndex] = energyRemoved;
			chargeRateIndex = (chargeRateIndex + 1) % 20;
			if (chargeRateSamples < 20) chargeRateSamples++;
			chargeAccumulator += energyRemoved;
			if(chargeAccumulator >= recipe.getEnergy()) {
				// Sequenced assembly steps are shared recipe instances whose next result is bound to the
				// last stack looked up, possibly by another machine, so look it up again for this stack.
				recipe = find(stack, this.getLevel()).map(RecipeHolder::value).orElse(recipe);
				TransportedItemStack remainingStack = transported.copy();
				remainingStack.stack.shrink(1);
				List<TransportedItemStack> outList = new ArrayList<>();
				for(ItemStack out : recipe.assemble(new SingleRecipeInput(stack), this.getLevel().getRandom())) {
					TransportedItemStack result = transported.copy();
					result.stack = out;
					outList.add(result);
				}
				handler.handleProcessingOnItem(transported, TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(outList, remainingStack));
				chargeAccumulator = 0;

				if (CommonConfig.AUDIO_ENABLED.get()) level.playSound(null, worldPosition, CASounds.LITTLE_ZAP.get(), SoundSource.BLOCKS, 0.1f, 1f);
			}
			return true;
		}
		return false;
	}

	// PORT-NOTE(26.1): Create Fly registers sequenced assembly steps as ordinary recipes of their type
	public Optional<RecipeHolder<ChargingRecipe>> find(ItemStack stack, Level level) {
		if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();
		return serverLevel.recipeAccess().getRecipeFor(CARecipes.CHARGING_TYPE, new SingleRecipeInput(stack), level);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if (level == null) return false;
		ObservePacketPayload.send(worldPosition, 0);
		// TODO Add networking
		/*
		CALang.builder().add(Component.translatable(CreateAddition.MODID + ".tooltip.energy.consumption").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
		CALang.builder().add(Component.literal(" " + Util.format(energyRemoved) + "⚡/t ").withStyle(ChatFormatting.AQUA)).forGoggles(tooltip);
		if (recipeCache.isPresent()) {
			ChargingRecipe recipe = recipeCache.get().value();
			CALang.builder().add(Component.translatable(CreateAddition.MODID + ".tooltip.energy.consumption").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
			CALang.builder().add(Component.literal(" " + Util.format(chargeAccumulator) + " / " + Util.format(recipe.getEnergy()) + "⚡").withStyle(ChatFormatting.AQUA)).forGoggles(tooltip);
		}
		*/
		int tr = TimeRemainingPacketPayload.clientTimeRemaining;
		if (tr == 0 || tr > 0 && tr <= 20) return false;
		String timeStr = tr == -1 ? "∞" : Util.formatTime(tr);
		CALang.builder().add(Component.translatable(CreateAddition.MODID + ".tooltip.charging.info").withStyle(ChatFormatting.WHITE)).forGoggles(tooltip);
		CALang.builder().add(Component.literal(" ").append(Component.translatable(CreateAddition.MODID + ".tooltip.charging.time_remaining").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(" " + timeStr).withStyle(ChatFormatting.AQUA))).forGoggles(tooltip);
		return true;
	}

	@Override
	public void onObserved(ServerPlayer player, ObservePacketPayload pkt) {
		int timeRemaining = 0;
		if(recipeCache.isPresent() && chargeRateSamples > 0) {
			ChargingRecipe recipe = recipeCache.get().value();
			int totalRate = 0;
			for (int rate : chargeRateHistory) totalRate += rate;
			int avgChargeRate = totalRate / chargeRateSamples;
			if (avgChargeRate == 0) {
				TimeRemainingPacketPayload.send(-1, player);
				return;
			}
			timeRemaining = (recipe.getEnergy() - chargeAccumulator) / avgChargeRate;
		}
		TimeRemainingPacketPayload.send(timeRemaining, player);
	}
}
