package com.mrh0.createaddition.blocks.rolling_mill;

import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.index.CABlockEntities;
import com.mrh0.createaddition.index.CARecipes;
import com.mrh0.createaddition.port.neoforge.capabilities.Capabilities;
import com.mrh0.createaddition.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.mrh0.createaddition.recipe.rolling.RollingRecipe;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.zurrtum.create.infrastructure.items.SidedItemInventory;
import com.zurrtum.create.infrastructure.transfer.SlotRangeCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * PORT-NOTE(26.1): follows Create Fly's MillstoneBlockEntity: one 10-slot sided vanilla inventory (slot 0 input,
 * 1-9 output) instead of NeoForge ItemStackHandlers + CombinedInvWrapper, single-stack recipe input.
 */
public class RollingMillBlockEntity extends KineticBlockEntity implements Clearable {
	public static final int SIZE = 10;
	public RollingMillInventory capability;
	public int timer;
	private @Nullable RollingRecipe lastRecipe;

	public RollingMillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		capability = new RollingMillInventory();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.ItemHandler.BLOCK,
				CABlockEntities.ROLLING_MILL.get(),
				(be, context) -> be.capability
		);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this));
		super.addBehaviours(behaviours);
	}

	public ItemStack getInput() {
		return capability.getItem(0);
	}

	@Override
	public void tick() {
		super.tick();

		if (getSpeed() == 0) return;
		for (int i = 1; i < SIZE; i++) {
			ItemStack stack = capability.getItem(i);
			if (!stack.isEmpty() && stack.getCount() == capability.getMaxStackSize(stack)) return;
		}

		if (timer > 0) {
			timer -= getProcessingSpeed();

			if (level.isClientSide()) {
				spawnParticles();
				return;
			}
			if (timer <= 0) process();
			return;
		} else if (level.isClientSide()) {
			return;
		}

		ItemStack stack = getInput();
		if (stack.isEmpty()) return;

		SingleRecipeInput input = new SingleRecipeInput(stack);
		if (lastRecipe == null || !lastRecipe.matches(input, level)) {
			Optional<RecipeHolder<RollingRecipe>> recipe = find(input, level);
			if (recipe.isEmpty()) {
				timer = 100;
				sendData();
			} else {
				lastRecipe = recipe.get().value();
				timer = CommonConfig.ROLLING_MILL_PROCESSING_DURATION.get();
				sendData();
			}
			return;
		}

		timer = CommonConfig.ROLLING_MILL_PROCESSING_DURATION.get();
		sendData();
	}

	@Override
	public void clearContent() {
		capability.clearContent();
	}

	@Override
	public void destroy() {
		super.destroy();
		Containers.dropContents(level, worldPosition, capability);
	}

	private void process() {
		ItemStack stack = getInput();
		SingleRecipeInput input = new SingleRecipeInput(stack);

		if (lastRecipe == null || !lastRecipe.matches(input, level)) {
			Optional<RecipeHolder<RollingRecipe>> recipe = find(input, level);
			if (recipe.isEmpty()) return;
			lastRecipe = recipe.get().value();
		}

		int count = stack.getCount();
		if (count == 1) {
			capability.setItem(0, ItemStack.EMPTY);
		} else {
			stack.setCount(count - 1);
			capability.setItem(0, stack);
		}
		capability.outputAllowInsertion();
		capability.insert(lastRecipe.getResultStack().copy());
		capability.outputForbidInsertion();

		sendData();
		setChanged();
	}

	public void spawnParticles() {
		ItemStack stackInSlot = getInput();
		if (stackInSlot.isEmpty()) return;

		ItemParticleOption data = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(stackInSlot));
		float angle = level.getRandom().nextFloat() * 360;
		Vec3 offset = new Vec3(0, 0, 0.5f);
		offset = VecHelper.rotate(offset, angle, Axis.Y);
		Vec3 target = VecHelper.rotate(offset, getSpeed() > 0 ? 25 : -25, Axis.Y);

		Vec3 center = offset.add(VecHelper.getCenterOf(worldPosition));
		target = VecHelper.offsetRandomly(target.subtract(offset), level.getRandom(), 1 / 128f);
		level.addParticle(data, center.x, center.y, center.z, target.x, target.y, target.z);
	}

	@Override
	public void write(ValueOutput view, boolean clientPacket) {
		view.putInt("Timer", timer);
		capability.write(view);
		super.write(view, clientPacket);
	}

	@Override
	protected void read(ValueInput view, boolean clientPacket) {
		timer = view.getIntOr("Timer", 0);
		capability.read(view);
		super.read(view, clientPacket);
	}

	public int getProcessingSpeed() {
		return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
	}

	private boolean canProcess(ItemStack stack) {
		SingleRecipeInput input = new SingleRecipeInput(stack);
		if (lastRecipe != null && lastRecipe.matches(input, level)) return true;
		Optional<RecipeHolder<RollingRecipe>> recipe = find(input, level);
		if (recipe.isEmpty()) return false;
		lastRecipe = recipe.get().value();
		return true;
	}

	public Optional<RecipeHolder<RollingRecipe>> find(SingleRecipeInput input, Level level) {
		if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();
		return serverLevel.recipeAccess().getRecipeFor(CARecipes.ROLLING_TYPE, input, level);
	}

	public class RollingMillInventory implements SidedItemInventory {
		private static final int[] SLOTS = SlotRangeCache.get(SIZE);
		private final NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		private boolean check = true;

		public void outputAllowInsertion() {
			check = false;
		}

		public void outputForbidInsertion() {
			check = true;
		}

		@Override
		public int getContainerSize() {
			return SIZE;
		}

		@Override
		public int[] getSlotsForFace(Direction side) {
			return SLOTS;
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return !check || (level != null && !level.isClientSide() && canProcess(stack));
		}

		@Override
		public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
			return check ? slot == 0 : slot > 0;
		}

		@Override
		public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
			return slot != 0;
		}

		@Override
		public ItemStack getItem(int slot) {
			if (slot >= SIZE) return ItemStack.EMPTY;
			return stacks.get(slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			if (slot >= SIZE) return;
			stacks.set(slot, stack);
		}

		@Override
		public void setChanged() {
			RollingMillBlockEntity.this.setChanged();
		}

		public void write(ValueOutput view) {
			ValueOutput.TypedOutputList<ItemStack> list = view.list("Inventory", ItemStack.OPTIONAL_CODEC);
			list.add(stacks.getFirst());
			for (int i = 1; i < SIZE; i++) {
				ItemStack stack = stacks.get(i);
				if (stack.isEmpty()) continue;
				list.add(stack);
			}
		}

		public void read(ValueInput view) {
			List<ItemStack> list = view.listOrEmpty("Inventory", ItemStack.OPTIONAL_CODEC).stream().toList();
			int i = 0;
			for (ItemStack itemStack : list) {
				if (i >= SIZE) break;
				stacks.set(i++, itemStack);
			}
			for (; i < SIZE; i++) setItem(i, ItemStack.EMPTY);
		}
	}
}
