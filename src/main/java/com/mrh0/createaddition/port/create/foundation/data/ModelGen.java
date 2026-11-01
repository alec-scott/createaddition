package com.mrh0.createaddition.port.create.foundation.data;

import com.mrh0.createaddition.port.registrate.builders.ItemBuilder;
import com.mrh0.createaddition.port.registrate.util.nullness.NonNullFunction;
import net.minecraft.world.item.BlockItem;

public final class ModelGen {
    private ModelGen() {
    }

    public static <I extends BlockItem, P> NonNullFunction<ItemBuilder<I, P>, P> customItemModel() {
        return b -> b.model((ctx, prov) -> { }).build();
    }

    public static <I extends BlockItem, P> NonNullFunction<ItemBuilder<I, P>, P> customItemModel(String... ignored) {
        return customItemModel();
    }
}
