package com.mrh0.createaddition.blocks.alternator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;

import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class AlternatorRenderer extends KineticBlockEntityRenderer<AlternatorBlockEntity> {

	public AlternatorRenderer(Context dispatcher) {
		super(dispatcher);
	}

	@Override
	protected void renderSafe(AlternatorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
			int light, int overlay) {
		if (VisualizationManager.supportsVisualization(be.getLevel())) return;
		BlockState state = be.getBlockState();
		Direction facing = state.getValue(BlockStateProperties.FACING);
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		renderRotatingBuffer(be, CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state), ms, vb, light);
		renderRotatingBuffer(be, CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,
				state.setValue(BlockStateProperties.FACING, facing.getOpposite())), ms, vb, light);
	}
}
