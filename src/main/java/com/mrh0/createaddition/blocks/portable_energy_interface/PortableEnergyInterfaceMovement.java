package com.mrh0.createaddition.blocks.portable_energy_interface;

import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.trains.entity.CarriageContraption;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PortableEnergyInterfaceMovement extends MovementBehaviour {

	static final String _workingPos_ = "WorkingPos";
	static final String _clientPrevPos_ = "ClientPrevPos";

	@Override
	public Vec3 getActiveAreaOffset(MovementContext context) {
		return Vec3.atLowerCornerOf(context.state.getValue(PortableEnergyInterfaceBlock.FACING).getUnitVec3i()).scale(1.850000023841858D);
	}

	//@Override
	//public boolean hasSpecialInstancedRendering() {
	//	return true;
	//}

	// TODO(26.1): contraption rendering (renderInContraption / createVisual) moves to a client MovementRenderBehaviour
	// registered with Create Fly's AllMovementRenders (see PortableEnergyInterfaceRenderer, PEIActorVisual).

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		boolean onCarriage = context.contraption instanceof CarriageContraption;
		if (!onCarriage || !(context.motion.length() > 0.25D)) {
			if (!this.findInterface(context, pos)) context.data.remove(_workingPos_);
		}
	}

	@Override
	public void tick(MovementContext context) {
		if (context.world.isClientSide()) {
			getAnimation(context).tickChaser();
		}

		boolean onCarriage = context.contraption instanceof CarriageContraption;
		if (!onCarriage || !(context.motion.length() > 0.25D)) {
			BlockPos pos;
			if (context.world.isClientSide()) {
				pos = BlockPos.containing(context.position);
				if (!this.findInterface(context, pos)) this.reset(context);

			} else if (context.data.contains(_workingPos_)) {
				pos = context.data.read(_workingPos_, BlockPos.CODEC).orElse(BlockPos.ZERO); // Should never orElse
				Vec3 target = VecHelper.getCenterOf(pos);
				if (!context.stall && !onCarriage && context.position.closerThan(target, target.distanceTo(context.position.add(context.motion)))) {
					context.stall = true;
				}

				Optional<Direction> currentFacingIfValid = this.getCurrentFacingIfValid(context);
				if (currentFacingIfValid.isPresent()) {
					PortableEnergyInterfaceBlockEntity stationaryInterface = this.getStationaryInterfaceAt(context.world, pos, context.state, currentFacingIfValid.get());
					if (stationaryInterface == null) {
						this.reset(context);
					} else {
						if (stationaryInterface.getConnectedEntity() == null) {
							stationaryInterface.startTransferringTo(context.contraption, stationaryInterface.getConnectionDistance());
						}
						boolean timerBelow = stationaryInterface.getTransferTimer() <= 4;
						stationaryInterface.keepAlive = 2;
						if (context.stall && timerBelow) context.stall = false;
					}
				}
			}
		}
	}

	protected boolean findInterface(MovementContext context, BlockPos pos) {
		Contraption var4 = context.contraption;
		if (var4 instanceof CarriageContraption cc) {
            if (!cc.notInPortal()) return false;
		}

		Optional<Direction> currentFacingIfValid = this.getCurrentFacingIfValid(context);
		if (currentFacingIfValid.isEmpty()) return false;
		else {
			Direction currentFacing = currentFacingIfValid.get();
			PortableEnergyInterfaceBlockEntity psi = this.findStationaryInterface(context.world, pos, context.state, currentFacing);
			if (psi == null) return false;
			else if (psi.isPowered()) return false;
			else {
				context.data.put(_workingPos_, NbtUtils.writeBlockPos(psi.getBlockPos()));
				if (!context.world.isClientSide()) {
					Vec3 diff = VecHelper.getCenterOf(psi.getBlockPos()).subtract(context.position);
					diff = VecHelper.project(diff, Vec3.atLowerCornerOf(currentFacing.getUnitVec3i()));
					float distance = (float)(diff.length() + 1.850000023841858D - 1.0D);
					psi.startTransferringTo(context.contraption, distance);
				} else {
					context.data.store(_clientPrevPos_, BlockPos.CODEC, pos);
					if (context.contraption instanceof CarriageContraption || context.contraption.entity.isStalled() || context.motion.lengthSqr() == 0.0D) {
						getAnimation(context).chase(psi.getConnectionDistance() / 2.0F, 0.25D, LerpedFloat.Chaser.LINEAR);
					}
				}

				return true;
			}
		}
	}

    @Override
	public void cancelStall(MovementContext context) {
		this.reset(context);
	}

	public void reset(MovementContext context) {
		context.data.remove(_clientPrevPos_);
		context.data.remove(_workingPos_);
		context.stall = false;
		getAnimation(context).chase(0.0D, 0.25D, LerpedFloat.Chaser.LINEAR);
	}

	@Nullable
	private PortableEnergyInterfaceBlockEntity findStationaryInterface(Level world, BlockPos pos, BlockState state, Direction facing) {
		for(int i = 0; i < 2; ++i) {
			PortableEnergyInterfaceBlockEntity interfaceAt = this.getStationaryInterfaceAt(world, pos.relative(facing, i), state, facing);
			if (interfaceAt != null) return interfaceAt;
		}
		return null;
	}

	@Nullable
	private PortableEnergyInterfaceBlockEntity getStationaryInterfaceAt(Level world, BlockPos pos, BlockState state, Direction facing) {
		BlockEntity te = world.getBlockEntity(pos);
		if (te instanceof PortableEnergyInterfaceBlockEntity psi) {
            BlockState blockState = world.getBlockState(pos);
			if (blockState.getBlock() != state.getBlock()) return null;
			else if (blockState.getValue(PortableEnergyInterfaceBlock.FACING) != facing.getOpposite()) return null;
			else return psi.isPowered() ? null : psi;
		} else return null;
	}

	private Optional<Direction> getCurrentFacingIfValid(MovementContext context) {
		Vec3 directionVec = Vec3.atLowerCornerOf(context.state.getValue(PortableEnergyInterfaceBlock.FACING).getUnitVec3i());
		directionVec = context.rotation.apply(directionVec);
		Direction facingFromVector = Direction.getApproximateNearest(directionVec.x, directionVec.y, directionVec.z);
		return directionVec.distanceTo(Vec3.atLowerCornerOf(facingFromVector.getUnitVec3i())) > 0.5D ? Optional.empty() : Optional.of(facingFromVector);
	}

	public static LerpedFloat getAnimation(MovementContext context) {
		Object var2 = context.temporaryData;
		if (var2 instanceof LerpedFloat lf) {
			return lf;
		} else {
			LerpedFloat nlf = LerpedFloat.linear();
			context.temporaryData = nlf;
			return nlf;
		}
	}

}
