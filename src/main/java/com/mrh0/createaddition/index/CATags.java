package com.mrh0.createaddition.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/** PORT-NOTE(26.1): the runtime tags from datagen's CATagRegister (datagen is excluded from the build). */
public final class CATags {
    private CATags() {
    }

    public static final class Fluids {
        public static final TagKey<Fluid> IGNITES = TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", "ignites"));
    }
}
