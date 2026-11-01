package com.mrh0.createaddition.recipe.rolling;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrh0.createaddition.index.CARecipes;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import com.zurrtum.create.foundation.recipe.CreateSingleStackRollableRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/** PORT-NOTE(26.1): Create Fly's single-ingredient recipe style, {@code {ingredient, results}}. */
public record RollingRecipe(List<ProcessingOutput> results, Ingredient ingredient) implements CreateSingleStackRollableRecipe {
    public static final MapCodec<RollingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ProcessingOutput.CODEC.listOf(1, 1).fieldOf("results").forGetter(RollingRecipe::results),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(RollingRecipe::ingredient)
    ).apply(instance, RollingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RollingRecipe> STREAM_CODEC = StreamCodec.composite(
            ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), RollingRecipe::results,
            Ingredient.CONTENTS_STREAM_CODEC, RollingRecipe::ingredient,
            RollingRecipe::new
    );
    public static final RecipeSerializer<RollingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getResultStack() {
        return results.getFirst().create();
    }

    @Override
    public RecipeSerializer<RollingRecipe> getSerializer() {
        return CARecipes.ROLLING;
    }

    @Override
    public RecipeType<RollingRecipe> getType() {
        return CARecipes.ROLLING_TYPE;
    }
}
