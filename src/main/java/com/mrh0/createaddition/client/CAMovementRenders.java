package com.mrh0.createaddition.client;

import com.mrh0.createaddition.blocks.liquid_blaze_burner.LiquidBlazeBurnerMovement;
import com.mrh0.createaddition.blocks.liquid_blaze_burner.LiquidBlazeBurnerMovementRender;
import com.mrh0.createaddition.blocks.portable_energy_interface.PortableEnergyInterfaceMovementRender;
import com.mrh0.createaddition.index.CABlocks;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import net.minecraft.world.level.block.Block;

/** Contraption render behaviours, attached like Create Fly's AllMovementRenders. */
public class CAMovementRenders {
	public static void register() {
		attach(CABlocks.PORTABLE_ENERGY_INTERFACE.get(), new PortableEnergyInterfaceMovementRender());

		LiquidBlazeBurnerMovementRender burner = new LiquidBlazeBurnerMovementRender();
		attach(CABlocks.LIQUID_BLAZE_BURNER.get(), burner);
		LiquidBlazeBurnerMovement.clientTick = burner::tick;
	}

	private static void attach(Block block, Object render) {
		MovementBehaviour behaviour = MovementBehaviour.REGISTRY.get(block);
		if (behaviour != null) behaviour.attachRender = render;
	}
}
