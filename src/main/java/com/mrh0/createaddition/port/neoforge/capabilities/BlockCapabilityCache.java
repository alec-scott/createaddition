package com.mrh0.createaddition.port.neoforge.capabilities;

import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;
import com.mrh0.createaddition.port.neoforge.energy.TREnergy;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import java.util.function.BooleanSupplier;

/**
 * PORT-NOTE(26.1): NeoForge's capability cache, energy only, over Fabric's {@link BlockApiCache}. Fabric's cache has
 * no invalidation listener; the lookup is re-run on every {@link #getCapability()} (cheap, the block entity is cached).
 */
public final class BlockCapabilityCache<T, C> {
    private final BlockApiCache<EnergyStorage, Direction> cache;
    private final Direction side;
    private final BooleanSupplier isValid;

    private BlockCapabilityCache(BlockApiCache<EnergyStorage, Direction> cache, Direction side, BooleanSupplier isValid) {
        this.cache = cache;
        this.side = side;
        this.isValid = isValid;
    }

    public static BlockCapabilityCache<IEnergyStorage, Direction> create(Capabilities.BlockCapability<IEnergyStorage> capability,
                                                                         ServerLevel level, BlockPos pos, Direction side,
                                                                         BooleanSupplier isValid, Runnable invalidationListener) {
        if (capability != Capabilities.EnergyStorage.BLOCK) throw new IllegalArgumentException("Unknown capability " + capability);
        return new BlockCapabilityCache<>(BlockApiCache.create(EnergyStorage.SIDED, level, pos), side, isValid);
    }

    public static BlockCapabilityCache<IEnergyStorage, Direction> create(Capabilities.BlockCapability<IEnergyStorage> capability,
                                                                         ServerLevel level, BlockPos pos, Direction side) {
        if (capability != Capabilities.EnergyStorage.BLOCK) throw new IllegalArgumentException("Unknown capability " + capability);
        return new BlockCapabilityCache<>(BlockApiCache.create(EnergyStorage.SIDED, level, pos), side, () -> true);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public T getCapability() {
        if (!isValid.getAsBoolean()) return null;
        return (T) TREnergy.fromTR(cache.find(side));
    }

    public BlockPos pos() {
        return cache.getPos();
    }

    public ServerLevel level() {
        return cache.getLevel();
    }
}
