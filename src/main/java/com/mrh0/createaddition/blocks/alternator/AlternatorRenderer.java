package com.mrh0.createaddition.blocks.alternator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/** PORT-NOTE(26.1): extract/submit; both half shafts rotate together (was renderSafe with two rotating buffers). */
public class AlternatorRenderer implements BlockEntityRenderer<AlternatorBlockEntity, AlternatorRenderer.AlternatorRenderState> {
	public AlternatorRenderer(Context context) {
	}

	@Override
	public AlternatorRenderState createRenderState() {
		return new AlternatorRenderState();
	}

	@Override
	public void extractRenderState(AlternatorBlockEntity be, AlternatorRenderState state, float partialTicks,
								   Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress) {
		Level level = SmartBlockEntityRenderer.extractBase(be, state, breakProgress);
		Direction facing = state.blockState.getValue(BlockStateProperties.FACING);
		int color = KineticBlockEntityRenderer.getTintColor(be);
		state.front = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state.blockState, facing)
				.cardinalLighting(level).light(state.lightCoords).color(color).extractRenderState();
		state.back = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state.blockState, facing.getOpposite())
				.cardinalLighting(level).light(state.lightCoords).color(color).extractRenderState();
		state.angle = KineticBlockEntityRenderer.getRotateAngleWithoutBeOffset(facing.getAxis(), be, state, level);
	}

	@Override
	public void submit(AlternatorRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.angle != null) matrices.rotateAround(state.angle, 0.5f, 0.5f, 0.5f);
		state.front.submit(matrices, queue);
		state.back.submit(matrices, queue);
	}

	public static class AlternatorRenderState extends BlockEntityRenderState {
		public SuperByteBufferRenderState front;
		public SuperByteBufferRenderState back;
		public @Nullable Quaternionf angle;
	}
}
