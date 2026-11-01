package com.mrh0.createaddition.blocks.electric_pump;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrh0.createaddition.index.CAPartials;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** PORT-NOTE(26.1): extract/submit; the pulsing pump partials, only without Flywheel (as before). */
public class ElectricPumpRenderer implements BlockEntityRenderer<ElectricPumpBlockEntity, ElectricPumpRenderer.PumpRenderState> {

	public ElectricPumpRenderer(Context context) {
	}

	@Override
	public PumpRenderState createRenderState() {
		return new PumpRenderState();
	}

	@Override
	public void extractRenderState(ElectricPumpBlockEntity pump, PumpRenderState state, float partialTicks,
								   Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress) {
		state.parts.clear();
		Level level = pump.getLevel();
		if (VisualizationManager.supportsVisualization(level)) return;
		SmartBlockEntityRenderer.extractBase(pump, state, breakProgress);
		float renderTime = AnimationTickHolder.getRenderTime(level);
		Direction facing = state.blockState.getValue(ElectricPumpBlock.FACING);
		Quaternionf rotation = ElectricPumpBlockEntity.getFacingRotation(facing);

		for (int i = 0; i < ElectricPumpBlockEntity.PARTIAL_OFFSETS.length; i++) {
			float[] offset = ElectricPumpBlockEntity.PARTIAL_OFFSETS[i];
			float scale = ElectricPumpBlockEntity.getPulseScale(pump.isActive(), pump.getPumpSpeed(), renderTime,
					ElectricPumpBlockEntity.PARTIAL_PHASE_OFFSETS[i]);
			state.parts.add(CachedBuffers.partial(CAPartials.ELECTRIC_PUMP_PARTIAL, state.blockState)
					.rotateCentered(rotation)
					.translate(offset[0], offset[1], offset[2])
					.scale(scale)
					.light(state.lightCoords)
					.extractRenderState());
		}
	}

	@Override
	public void submit(PumpRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
		for (SuperByteBufferRenderState part : state.parts) part.submit(matrices, queue);
	}

	public static class PumpRenderState extends BlockEntityRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>();
	}
}
