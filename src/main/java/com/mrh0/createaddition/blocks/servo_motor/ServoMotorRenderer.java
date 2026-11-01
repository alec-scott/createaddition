package com.mrh0.createaddition.blocks.servo_motor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.contraptions.bearing.BearingRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * PORT-NOTE(26.1): extract/submit on Create Fly's BearingRenderer (the servo's angle comes from its
 * getInterpolatedAngle override). Like the 1.21.1 renderer, only the disc is drawn; the motor body is the block model.
 */
public class ServoMotorRenderer extends BearingRenderer<ServoMotorBlockEntity> {

	public ServoMotorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BearingRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
		if (state.topAngle != null) matrices.rotateAround(state.topAngle, 0.5f, 0.5f, 0.5f);
		if (state.upAngle != null) matrices.rotateAround(state.upAngle, 0.5f, 0.5f, 0.5f);
		if (state.eastAngle != null) matrices.rotateAround(state.eastAngle, 0.5f, 0.5f, 0.5f);
		state.top.submit(matrices, queue);
	}
}
