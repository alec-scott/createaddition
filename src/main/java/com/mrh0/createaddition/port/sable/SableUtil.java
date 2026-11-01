package com.mrh0.createaddition.port.sable;

import com.mrh0.createaddition.energy.IWireNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * PORT-NOTE(26.1): Sable compat ({@code compat/sable}) is excluded from the build, so CreateAddition.SABLE_ACTIVE is
 * always false. These are the non-Sable answers for the few call sites outside the compat package.
 */
public final class SableUtil {
    private SableUtil() {
    }

    public static double localNodeDistSq(Level level, BlockPos pos1, Vec3 off1, BlockPos pos2, Vec3 off2) {
        return Vec3.atCenterOf(pos1).add(off1).distanceToSqr(Vec3.atCenterOf(pos2).add(off2));
    }

    public static boolean isInSubLevel(Level level, BlockPos pos) {
        return false;
    }

    public static void breakWires(Level level, IWireNode node) {
    }

    public static Vec3 localDown(Level level, BlockPos pos, float partialTick) {
        return new Vec3(0, -1, 0);
    }
}
