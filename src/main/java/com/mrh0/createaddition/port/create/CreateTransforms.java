package com.mrh0.createaddition.port.create;

import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.api.behaviour.display.DisplayTarget;
import com.zurrtum.create.api.contraption.storage.item.MountedItemStorageType;
import com.mrh0.createaddition.port.registrate.builders.BlockBuilder;
import com.mrh0.createaddition.port.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Upstream Create's static builder transforms ({@code DisplaySource.displaySource}, {@code DisplayTarget.displayTarget},
 * {@code MountedItemStorageType.mountedItemStorage}).
 *
 * <p>PORT-NOTE(26.1): Create Fly has the registries but not the Registrate helpers. Entries are recorded per block and
 * applied by {@link #apply()} once everything (including D&D's storage types) is registered.</p>
 */
public final class CreateTransforms {
    private static final List<Runnable> PENDING = new ArrayList<>();

    private CreateTransforms() {
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> displaySource(DisplaySource source) {
        return b -> b.onRegister(block -> PENDING.add(() -> DisplaySource.BY_BLOCK.add(block, source)));
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> displayTarget(DisplayTarget target) {
        return b -> b.onRegister(block -> PENDING.add(() -> DisplayTarget.BY_BLOCK.register(block, target)));
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> mountedItemStorage(Supplier<? extends MountedItemStorageType<?>> type) {
        return b -> b.onRegister(block -> PENDING.add(() -> MountedItemStorageType.REGISTRY.register(block, type.get())));
    }

    public static void apply() {
        PENDING.forEach(Runnable::run);
        PENDING.clear();
    }
}
