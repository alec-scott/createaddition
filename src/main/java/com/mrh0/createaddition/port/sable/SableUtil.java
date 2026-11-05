package com.mrh0.createaddition.port.sable;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.energy.IWireNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * PORT-NOTE(26.1): Sable is optional. Call sites outside {@code compat/sable} go through this facade: with Sable
 * loaded ({@link CreateAddition#SABLE_ACTIVE}) it forwards to {@link com.mrh0.createaddition.compat.sable.SableUtil},
 * otherwise it gives the non-Sable answers, so no Sable class is loaded without the mod.
 */
public final class SableUtil {
    private SableUtil() {
    }

    public static double localNodeDistSq(Level level, BlockPos pos1, Vec3 off1, BlockPos pos2, Vec3 off2) {
        if (CreateAddition.SABLE_ACTIVE)
            return com.mrh0.createaddition.compat.sable.SableUtil.localNodeDistSq(level, pos1, off1, pos2, off2);
        return Vec3.atCenterOf(pos1).add(off1).distanceToSqr(Vec3.atCenterOf(pos2).add(off2));
    }

    public static boolean isInSubLevel(Level level, BlockPos pos) {
        return CreateAddition.SABLE_ACTIVE && com.mrh0.createaddition.compat.sable.SableUtil.isInSubLevel(level, pos);
    }

    public static void breakWires(Level level, IWireNode node) {
        if (CreateAddition.SABLE_ACTIVE) com.mrh0.createaddition.compat.sable.SableUtil.breakWires(level, node);
    }

    /** World position of a block, for light lookups (a sub-level block's plot position has no world light). */
    public static BlockPos projectOut(Level level, BlockPos pos) {
        if (CreateAddition.SABLE_ACTIVE) return com.mrh0.createaddition.compat.sable.SableUtil.projectOut(level, pos);
        return pos;
    }

    /** Gravity in the frame of the block at {@code pos} (rotated with its sub-level, client side). */
    public static Vec3 localDown(Level level, BlockPos pos, float partialTick) {
        if (CreateAddition.SABLE_ACTIVE)
            return com.mrh0.createaddition.compat.sable.SableUtil.localDown(level, pos, partialTick);
        return new Vec3(0, -1, 0);
    }

    /** The point {@code remoteOffset} from {@code remotePos}'s centre, in the frame of the block at {@code pos}. */
    public static Vec3 nodeWireOffsetRelativeTo(Level level, BlockPos pos, BlockPos remotePos, Vec3 remoteOffset,
                                                float partialTick) {
        if (CreateAddition.SABLE_ACTIVE)
            return com.mrh0.createaddition.compat.sable.SableUtil.nodeWireOffsetRelativeTo(level, pos, remotePos,
                    remoteOffset, partialTick);
        return Vec3.atCenterOf(remotePos).add(remoteOffset);
    }

    /** {@code vec} (world or plot space) in the frame of the block at {@code pos}. */
    public static Vec3 transformRelativeTo(Level level, BlockPos pos, Vec3 vec, float partialTick) {
        if (CreateAddition.SABLE_ACTIVE)
            return com.mrh0.createaddition.compat.sable.SableUtil.transformRelativeTo(level, pos, vec, partialTick);
        return vec;
    }
}
