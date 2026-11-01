package com.mrh0.createaddition.blocks.modular_accumulator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrh0.createaddition.index.CAPartials;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** PORT-NOTE(26.1): extract/submit; the gauges and dials of a controller, positioned for the whole multiblock. */
public class ModularAccumulatorRenderer implements BlockEntityRenderer<ModularAccumulatorBlockEntity, ModularAccumulatorRenderer.AccumulatorRenderState> {

	public ModularAccumulatorRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public AccumulatorRenderState createRenderState() {
		return new AccumulatorRenderState();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public void extractRenderState(ModularAccumulatorBlockEntity te, AccumulatorRenderState state, float partialTicks,
								   Vec3 cameraPos, @Nullable CrumblingOverlay crumblingOverlay) {
		SmartBlockEntityRenderer.extractBase(te, state, crumblingOverlay);
		state.parts.clear();
		if (!te.isController()) return;
		te.observe();

		BlockState blockState = te.getBlockState();
		float dialPivotY = 6f / 16f;
		float dialPivotZ = 8f / 16f;
		float progress = te.gauge.getValue(partialTicks);
		float cx = te.width / 2f, cy = te.height - 0.5f, cz = te.width / 2f;

		for (Direction d : Iterate.horizontalDirections) {
			state.parts.add(CachedBuffers.partial(CAPartials.ACCUMULATOR_GUAGE, blockState)
					.translate(cx, cy, cz)
					.rotateYDegrees(d.toYRot())
					.uncenter()
					.translate(te.width / 2f - 6 / 16f, 0, 0)
					.light(state.lightCoords)
					.extractRenderState());
			state.parts.add(CachedBuffers.partial(CAPartials.ACCUMULATOR_DIAL, blockState)
					.translate(cx, cy, cz)
					.rotateYDegrees(d.toYRot())
					.uncenter()
					.translate(te.width / 2f - 6 / 16f, 0, 0)
					.translate(0, dialPivotY, dialPivotZ)
					.rotateXDegrees(-180 * progress)
					.translate(0, -dialPivotY, -dialPivotZ)
					.light(state.lightCoords)
					.extractRenderState());
		}
	}

	@Override
	public void submit(AccumulatorRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
		for (SuperByteBufferRenderState part : state.parts) part.submit(matrices, queue);
	}

	public static class AccumulatorRenderState extends BlockEntityRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>();
	}
}
