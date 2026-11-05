package com.mrh0.createaddition.compat.jei.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mrh0.createaddition.index.CABlocks;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.gui.render.BlockBakedQuadOutput;
import com.zurrtum.create.client.flywheel.lib.model.baked.ModelConsumer;
import com.zurrtum.create.client.flywheel.lib.model.baked.ModelRenderHelper;
import com.zurrtum.create.client.flywheel.lib.model.baked.SinglePosVirtualBlockGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Replacement for the old {@code AnimatedRollingMill}: modelled on Create Fly's MillstoneRenderer. Two shafts along Z
 * spin in opposite directions through the mill, which is drawn at Create's usual 22.5/22.5 GUI angle.
 */
public class RollingMillRenderer extends PictureInPictureRenderer<RollingMillRenderState> {
    private final BlockBakedQuadOutput output;

    public RollingMillRenderer(BufferSource vertexConsumers) {
        super(vertexConsumers);
        output = new BlockBakedQuadOutput(vertexConsumers);
    }

    @Override
    protected void renderToTexture(RollingMillRenderState state, PoseStack matrices) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        // Block centre ends up at the middle of the PiP box
        matrices.translate(-0.5f, 0.5f, -0.5f);
        matrices.scale(1, -1, -1);
        output.setPoseStack(matrices);
        SinglePosVirtualBlockGetter world = SinglePosVirtualBlockGetter.createFullBright();
        float angle = (AnimationTickHolder.getRenderTime() * 4f) % 360;

        BlockState air = Blocks.AIR.defaultBlockState();
        world.blockState(air);
        BlockStateModel shaft = AllPartialModels.SHAFT.get();
        drawShaft(matrices, world, shaft, air, 0f, -angle);
        drawShaft(matrices, world, shaft, air, -4f / 16f, angle);

        BlockState mill = CABlocks.ROLLING_MILL.get().defaultBlockState();
        world.blockState(mill);
        BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(mill);
        matrices.pushPose();
        matrices.translate(0.5f, 0.5f, 0.5f);
        matrices.mulPose(Axis.XP.rotationDegrees(22.5f));
        matrices.mulPose(Axis.YP.rotationDegrees(22.5f));
        matrices.translate(-0.5f, -0.5f, -0.5f);
        output.updateBuffer(model);
        ModelConsumer blockRenderer = ModelRenderHelper.getHelper(output);
        blockRenderer.tesselateBlock(0, 0, 0, world, BlockPos.ZERO, mill, model, 42L);
        matrices.popPose();
        output.clear();
    }

    private void drawShaft(PoseStack matrices, SinglePosVirtualBlockGetter world, BlockStateModel shaft,
                           BlockState air, float yOffset, float angle) {
        matrices.pushPose();
        matrices.translate(0.5f, 0.5f, 0.5f);
        matrices.mulPose(Axis.XP.rotationDegrees(22.5f));
        matrices.mulPose(Axis.YP.rotationDegrees(22.5f));
        matrices.translate(0, yOffset, 0);
        matrices.mulPose(Axis.ZP.rotationDegrees(angle));
        // The shaft model stands along Y: lay it down along Z
        matrices.mulPose(Axis.XP.rotationDegrees(90));
        matrices.translate(-0.5f, -0.5f, -0.5f);
        output.updateBuffer(shaft);
        ModelConsumer blockRenderer = ModelRenderHelper.getHelper(output);
        blockRenderer.tesselateBlock(0, 0, 0, world, BlockPos.ZERO, air, shaft, 42L);
        matrices.popPose();
    }

    @Override
    protected String getTextureLabel() {
        return "Rolling Mill";
    }

    @Override
    public Class<RollingMillRenderState> getRenderStateClass() {
        return RollingMillRenderState.class;
    }
}
