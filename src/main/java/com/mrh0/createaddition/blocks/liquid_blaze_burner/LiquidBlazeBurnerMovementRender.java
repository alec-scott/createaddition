package com.mrh0.createaddition.blocks.liquid_blaze_burner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mrh0.createaddition.index.CAPartials;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderBehaviour;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderState;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.content.processing.burner.BlazeBurnerRenderer;
import com.zurrtum.create.client.content.processing.burner.BlazeBurnerRenderer.BlazeBurnerRenderData;
import com.zurrtum.create.client.foundation.virtualWorld.VirtualRenderWorld;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Contraption rendering of the liquid blaze burner (was LiquidBlazeBurnerRenderer.renderInContraption). Mirrors Create
 * Fly's BlazeBurnerMovementRenderBehaviour, with CC&A's liquid hat instead of the train hat; no conductor handling.
 */
public class LiquidBlazeBurnerMovementRender implements MovementRenderBehaviour {

	public void tick(MovementContext context) {
		if (!shouldRender(context)) return;

		RandomSource r = context.world.getRandom();
		Vec3 v = context.position.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .125f).multiply(1, 0, 1));
		if (r.nextInt(3) == 0 && context.motion.length() < 1 / 64f)
			context.world.addParticle(ParticleTypes.LARGE_SMOKE, v.x, v.y, v.z, 0, 0, 0);

		LerpedFloat headAngle = getHeadAngle(context);
		headAngle.chase(headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), getTargetAngle(context)),
				.5f, LerpedFloat.Chaser.exp(5));
		headAngle.tickChaser();
	}

	private boolean shouldRender(MovementContext context) {
		return LiquidBlazeBurnerBlock.getHeatLevelOf(context.state) != BlazeBurnerBlock.HeatLevel.NONE;
	}

	private LerpedFloat getHeadAngle(MovementContext context) {
		if (!(context.temporaryData instanceof LerpedFloat))
			context.temporaryData = LerpedFloat.angular().startWithValue(getTargetAngle(context));
		return (LerpedFloat) context.temporaryData;
	}

	private float getTargetAngle(MovementContext context) {
		Entity player = Minecraft.getInstance().getCameraEntity();
		if (player != null && !player.isInvisible() && context.position != null) {
			Vec3 rotated = context.contraption.entity.reverseRotation(player.position().subtract(context.position), 1);
			return AngleHelper.deg(-Mth.atan2(rotated.z, rotated.x)) - 90;
		}
		return 0;
	}

	@Override
	@Nullable
	public MovementRenderState getRenderState(Vec3 camera, Font textRenderer, MovementContext context,
											  VirtualRenderWorld renderWorld, Pose transform, Matrix4f worldMatrix4f) {
		BlockState blockState = context.state;
		BlazeBurnerBlock.HeatLevel heatLevel = LiquidBlazeBurnerBlock.getHeatLevelOf(blockState);
		if (heatLevel == BlazeBurnerBlock.HeatLevel.NONE) return null;
		if (!heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING))
			heatLevel = BlazeBurnerBlock.HeatLevel.FADING;

		State state = new State();
		state.pose = transform;
		state.pos = context.localPos;
		Level level = context.world;
		float horizontalAngle = AngleHelper.rad(getHeadAngle(context).getValue(AnimationTickHolder.getPartialTicks(level)));
		boolean drawGoggles = context.blockEntityData.contains("Goggles");
		state.data = BlazeBurnerRenderer.getBlazeBurnerRenderData(level, blockState, heatLevel, 0, horizontalAngle, false,
				drawGoggles, CAPartials.LIQUID_HAT, context.hashCode());
		return state;
	}

	public static class State implements MovementRenderState {
		public @UnknownNullability BlazeBurnerRenderData data;
		public @UnknownNullability Pose pose;
		public @UnknownNullability BlockPos pos;

		@Override
		public void submit(PoseStack matrices, SubmitNodeCollector queue) {
			matrices.pushPose();
			transform(matrices, pose, pos);
			data.submit(matrices, queue);
			matrices.popPose();
		}
	}
}
