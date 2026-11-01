package com.mrh0.createaddition.blocks.servo_motor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.content.contraptions.bearing.BearingRenderer;

import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class ServoMotorRenderer extends BearingRenderer<ServoMotorBlockEntity> {

	public ServoMotorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ServoMotorBlockEntity be, float partialTicks, PoseStack ms,
			MultiBufferSource buffer, int light, int overlay) {
		if (VisualizationManager.supportsVisualization(be.getLevel())) return;

		Direction facing = be.getBlockState().getValue(BlockStateProperties.FACING);
		SuperByteBuffer superBuffer = CachedBuffers.partial(AllPartialModels.BEARING_TOP, be.getBlockState());

		float interpolatedAngle = be.getInterpolatedAngle(partialTicks - 1);
		kineticRotationTransform(superBuffer, be, facing.getAxis(), (float) (interpolatedAngle / 180 * Math.PI), light);

		if (facing.getAxis().isHorizontal())
			superBuffer.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
		superBuffer.rotateCentered(AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)), Direction.EAST);
		superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
	}

}
