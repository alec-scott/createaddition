package com.mrh0.createaddition.blocks.portable_energy_interface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrh0.createaddition.index.CAPartials;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * PORT-NOTE(26.1): extract/submit for the placed block (non-Flywheel only, as before).
 * TODO(26.1): rendering on contraptions (was renderInContraption) needs a Create Fly MovementRenderBehaviour.
 */
public class PortableEnergyInterfaceRenderer implements BlockEntityRenderer<PortableEnergyInterfaceBlockEntity, PortableEnergyInterfaceRenderer.PEIRenderState> {

	public PortableEnergyInterfaceRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public PEIRenderState createRenderState() {
		return new PEIRenderState();
	}

	@Override
	public void extractRenderState(PortableEnergyInterfaceBlockEntity be, PEIRenderState state, float partialTicks,
								   Vec3 cameraPos, @Nullable CrumblingOverlay crumblingOverlay) {
		state.middle = null;
		state.top = null;
		if (VisualizationManager.supportsVisualization(be.getLevel())) return;
		SmartBlockEntityRenderer.extractBase(be, state, crumblingOverlay);
		BlockState blockState = state.blockState;
		float progress = be.getExtensionDistance(partialTicks);
		SuperByteBuffer middle = CachedBuffers.partial(getMiddleForState(blockState, be.isConnected()), blockState);
		SuperByteBuffer top = CachedBuffers.partial(getTopForState(blockState), blockState);
		Direction facing = blockState.getValue(PortableEnergyInterfaceBlock.FACING);
		rotateToFacing(middle, facing);
		rotateToFacing(top, facing);
		middle.translate(0, progress * 0.5f + 0.375f, 0);
		top.translate(0, progress, 0);
		state.middle = middle.light(state.lightCoords).extractRenderState();
		state.top = top.light(state.lightCoords).extractRenderState();
	}

	@Override
	public void submit(PEIRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
		if (state.middle != null) state.middle.submit(matrices, queue);
		if (state.top != null) state.top.submit(matrices, queue);
	}

	public static class PEIRenderState extends BlockEntityRenderState {
		public @Nullable SuperByteBufferRenderState middle;
		public @Nullable SuperByteBufferRenderState top;
	}

	private static void rotateToFacing(SuperByteBuffer buffer, Direction facing) {
		buffer.center()
				.rotateYDegrees(AngleHelper.horizontalAngle(facing))
				.rotateXDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90)
				.uncenter();
	}

	@Nullable
	static PortableEnergyInterfaceBlockEntity getTargetPSI(MovementContext context) {
		String _workingPos_ = PortableEnergyInterfaceMovement._workingPos_;
		if (!context.data.contains(_workingPos_)) return null;

		BlockPos pos = context.data.read(_workingPos_, BlockPos.CODEC).orElse(BlockPos.ZERO); // Should never else
		BlockEntity blockEntity = context.world.getBlockEntity(pos);
		if (!(blockEntity instanceof PortableEnergyInterfaceBlockEntity psi)) return null;

		if (!psi.isTransferring()) return null;
		return psi;
	}

	static PartialModel getMiddleForState(BlockState state, boolean lit) {
		return lit ? CAPartials.PORTABLE_ENERGY_INTERFACE_MIDDLE_POWERED
				: CAPartials.PORTABLE_ENERGY_INTERFACE_MIDDLE;
	}

	static PartialModel getTopForState(BlockState state) {
		return CAPartials.PORTABLE_ENERGY_INTERFACE_TOP;
	}
}
