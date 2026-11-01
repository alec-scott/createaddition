package com.mrh0.createaddition.blocks.liquid_blaze_burner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrh0.createaddition.index.CAPartials;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.content.processing.burner.BlazeBurnerRenderer;
import com.zurrtum.create.client.content.processing.burner.BlazeBurnerRenderer.BlazeBurnerRenderState;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * PORT-NOTE(26.1): extract/submit through Create Fly's BlazeBurnerRenderer data, with CC&A's liquid hat as the hat
 * partial (Create Fly's renderer already places train/logistics hats the same way). The Sable sub-level path and
 * contraption rendering are not ported (see PORTING-26.1.md).
 */
public class LiquidBlazeBurnerRenderer implements BlockEntityRenderer<LiquidBlazeBurnerBlockEntity, BlazeBurnerRenderState> {

	public LiquidBlazeBurnerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public BlazeBurnerRenderState createRenderState() {
		return new BlazeBurnerRenderState();
	}

	@Override
	public void extractRenderState(LiquidBlazeBurnerBlockEntity be, BlazeBurnerRenderState state, float partialTicks,
								   Vec3 cameraPos, @Nullable CrumblingOverlay crumblingOverlay) {
		state.data = null;
		BlazeBurnerBlock.HeatLevel heatLevel = be.getHeatLevelFromBlock();
		if (heatLevel == BlazeBurnerBlock.HeatLevel.NONE) return;
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		float animation = be.headAnimation.getValue(partialTicks) * .175f;
		float horizontalAngle = AngleHelper.rad(be.headAngle.getValue(partialTicks));
		boolean canDrawFlame = heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
		state.data = BlazeBurnerRenderer.getBlazeBurnerRenderData(be.getLevel(), state.blockState, heatLevel, animation,
				horizontalAngle, canDrawFlame, be.goggles, CAPartials.LIQUID_HAT, be.hashCode());
	}

	@Override
	public void submit(BlazeBurnerRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.data != null) state.data.submit(matrices, queue);
	}

	public static PartialModel getBlazeModel(BlazeBurnerBlock.HeatLevel heatLevel, boolean blockAbove) {
		return BlazeBurnerRenderer.getBlazeModel(heatLevel, blockAbove);
	}
}
