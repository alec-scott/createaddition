package com.mrh0.createaddition.blocks.modular_accumulator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.redstone.Orientation;

import net.minecraft.world.level.ScheduledTickAccess;

import net.minecraft.util.RandomSource;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.index.CABlockEntities;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.foundation.block.IBE;
import com.zurrtum.create.foundation.blockEntity.ComparatorUtil;

import com.mrh0.createaddition.port.registrate.providers.DataGenContext;
import com.mrh0.createaddition.port.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mrh0.createaddition.port.porting_lib.models.generators.BlockModelProvider;
import com.mrh0.createaddition.port.porting_lib.models.generators.ModelFile;
import com.mrh0.createaddition.port.porting_lib.models.generators.MultiPartBlockStateBuilder;

public class ModularAccumulatorBlock extends Block implements IWrenchable, IBE<ModularAccumulatorBlockEntity> {

	public static final BooleanProperty TOP = BooleanProperty.create("top");
	public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

	public static ModularAccumulatorBlock regular(Properties props) {
		return new ModularAccumulatorBlock(props);
	}

	@Override
	public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
		// AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	public static boolean isAccumulator(BlockState state) {
		return state.getBlock() instanceof ModularAccumulatorBlock;
	}

	protected ModularAccumulatorBlock(Properties props) {
		super(props);
		registerDefaultState(defaultBlockState().setValue(TOP, true)
			.setValue(BOTTOM, true));
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
		if (oldState.getBlock() == state.getBlock()) return;
		if (moved) return;
		withBlockEntityDo(world, pos, ModularAccumulatorBlockEntity::updateConnectivity);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(TOP, BOTTOM);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.SUCCESS;
	}

	@Override
	public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
		return Shapes.block();
	}

	@Override
	public BlockState updateShape(BlockState pState, LevelReader pLevel, ScheduledTickAccess ticks, BlockPos pCurrentPos, Direction pDirection, BlockPos pNeighborPos, BlockState pNeighborState, RandomSource random) {
		return pState;
	}

	// PORT-NOTE(26.1): onRemove -> ModularAccumulatorBlockEntity.preRemoveSideEffects (as Create Fly's fluid tank). The
	// batch-placement sound silencing is dropped (entity-aware getSoundType and persistent data are NeoForge-only;
	// Create Fly dropped it for tanks and vaults too).

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos, Direction direction) {
		return getBlockEntityOptional(level, pos).map(ModularAccumulatorBlockEntity::getControllerBE)
			.map(te -> ComparatorUtil.fractionToRedstoneLevel(te.getFillState()))
			.orElse(0);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, @Nullable Orientation orientation, boolean isMoving) {
		BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
		if(be != null) {
			if(be instanceof ModularAccumulatorBlockEntity) {
				((ModularAccumulatorBlockEntity)be).updateCache();
			}
		}
	}

	@Override
	public Class<ModularAccumulatorBlockEntity> getBlockEntityClass() {
		return ModularAccumulatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ModularAccumulatorBlockEntity> getBlockEntityType() {
		return CABlockEntities.MODULAR_ACCUMULATOR.get();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return CABlockEntities.MODULAR_ACCUMULATOR.create(pos, state);
	}

	public static void makeBlockState(DataGenContext<Block, ModularAccumulatorBlock> ctx, RegistrateBlockstateProvider provider) {
		BlockModelProvider models = provider.models();
		String basePath = "block/modular_accumulator/block_";
		MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(ctx.get());

		ModelFile.ExistingModelFile middle = models.getExistingFile(Identifier.fromNamespaceAndPath(CreateAddition.MODID, basePath + "middle"));
		ModelFile.ExistingModelFile bottom = models.getExistingFile(Identifier.fromNamespaceAndPath(CreateAddition.MODID, basePath + "bottom"));
		ModelFile.ExistingModelFile top = models.getExistingFile(Identifier.fromNamespaceAndPath(CreateAddition.MODID, basePath + "top"));
		ModelFile.ExistingModelFile single = models.getExistingFile(Identifier.fromNamespaceAndPath(CreateAddition.MODID, basePath + "single"));

		builder.part()
				.modelFile(middle)
				.addModel()
				.condition(BOTTOM, false)
				.condition(TOP, false)
				.end()
				.part()
				.modelFile(bottom)
				.addModel()
				.condition(BOTTOM, true)
				.condition(TOP, false)
				.end()
				.part()
				.modelFile(top)
				.addModel()
				.condition(BOTTOM, false)
				.condition(TOP, true)
				.end()
				.part()
				.modelFile(single)
				.addModel()
				.condition(BOTTOM, true)
				.condition(TOP, true)
				.end();
	}
}
