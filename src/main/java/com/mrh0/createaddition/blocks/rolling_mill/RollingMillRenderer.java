package com.mrh0.createaddition.blocks.rolling_mill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * PORT-NOTE(26.1): extract/submit. Create Fly's KineticBlockEntityRenderer draws the through shaft (skipped with
 * Flywheel, as before); the two rollers are half shafts extracted alongside.
 */
public class RollingMillRenderer extends KineticBlockEntityRenderer<RollingMillBlockEntity, RollingMillRenderer.RollingMillRenderState> {

	public RollingMillRenderer(Context context) {
		super(context);
	}

	@Override
	public RollingMillRenderState createRenderState() {
		return new RollingMillRenderState();
	}

	@Override
	protected BlockState getRenderedBlockState(RollingMillBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	@Override
	public void extractRenderState(RollingMillBlockEntity be, RollingMillRenderState state, float tickProgress,
								   Vec3 cameraPos, @Nullable CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(be, state, tickProgress, cameraPos, crumblingOverlay);
		state.rollerA = null;
		state.rollerB = null;
		if (state.support) return;
		BlockState blockState = be.getBlockState();
		Axis axis = getRotationAxisOf(be);
		float angle = getAngleForBe(be, be.getBlockPos(), axis);
		state.rollerA = CachedBuffers.partial(AllPartialModels.SHAFT_HALF, blockState)
				.rotateCentered(axis == Axis.Z ? 0 : RAD_90, Direction.UP)
				.translate(0, 4f / 16f, 0)
				.rotateCentered(angle, Direction.NORTH)
				.light(state.lightCoords)
				.extractRenderState();
		state.rollerB = CachedBuffers.partial(AllPartialModels.SHAFT_HALF, blockState)
				.rotateCentered(axis == Axis.Z ? RAD_180 : RAD_270, Direction.UP)
				.translate(0, 4f / 16f, 0)
				.rotateCentered(-angle, Direction.NORTH)
				.light(state.lightCoords)
				.extractRenderState();
	}

	@Override
	public void submit(RollingMillRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
		super.submit(state, matrices, queue, cameraState);
		if (state.rollerA != null) state.rollerA.submit(matrices, queue);
		if (state.rollerB != null) state.rollerB.submit(matrices, queue);
	}

	public static class RollingMillRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState rollerA;
		public @Nullable SuperByteBufferRenderState rollerB;
	}
}
