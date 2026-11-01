package com.mrh0.createaddition.event;

import com.mrh0.createaddition.blocks.liquid_blaze_burner.LiquidBlazeBurnerBlock;
import com.mrh0.createaddition.blocks.portable_energy_interface.PortableEnergyManager;
import com.mrh0.createaddition.energy.network.WireGraph;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.index.CAItems;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.mrh0.createaddition.port.minecraft.IOnLoad;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** PORT-NOTE(26.1): NeoForge game-bus subscribers → Fabric callbacks, registered from {@link #register()}. */
public class GameEvents {
	public static void register() {
		// Was LevelTickEvent.Pre (server levels only)
		ServerTickEvents.START_LEVEL_TICK.register(WireGraph::tick);
		// Using the server tick instead of the level tick because some contraptions can switch levels.
		ServerTickEvents.START_SERVER_TICK.register(server -> PortableEnergyManager.tick());
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> WireGraph.chunkLoaded(level, chunk));
		ServerLevelEvents.UNLOAD.register((server, level) -> WireGraph.levelUnloaded(level));
		UseBlockCallback.EVENT.register(GameEvents::interact);
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
			if (be instanceof IOnLoad loadAware) loadAware.onLoad();
		});
	}

	private static InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		try {
			if (level.isClientSide()) return InteractionResult.PASS;
			BlockPos pos = hit.getBlockPos();
			ItemStack stack = player.getItemInHand(hand);
			BlockState state = level.getBlockState(pos);
			if (stack.getItem() == CAItems.STRAW.get() && level.getBlockEntity(pos) instanceof BlazeBurnerBlockEntity) {
				if (state.is(AllBlocks.BLAZE_BURNER)) {
					BlockState newState = CABlocks.LIQUID_BLAZE_BURNER.getDefaultState()
							.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING)
							.setValue(LiquidBlazeBurnerBlock.FACING, state.getValue(BlazeBurnerBlock.FACING));
					level.setBlockAndUpdate(pos, newState);
					if (!player.isCreative())
						stack.shrink(1);
					return InteractionResult.SUCCESS;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return InteractionResult.PASS;
	}
}
