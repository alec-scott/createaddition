package com.mrh0.createaddition.blocks.electric_motor;

import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;

import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;


public class ElectricMotorRenderer extends KineticBlockEntityRenderer {

	public ElectricMotorRenderer(Context dispatcher) {
		super(dispatcher);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(KineticBlockEntity te, BlockState state) {
		return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state);
	}
}

