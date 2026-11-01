package com.mrh0.createaddition.recipe.charging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.index.CARecipes;
import com.mrh0.createaddition.util.Util;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import com.zurrtum.create.foundation.recipe.CreateSingleStackRollableRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * PORT-NOTE(26.1): Create Fly's single-ingredient recipe style, {@code {ingredient, results, energy, max_charge_rate}}.
 * Sequenced assembly steps are plain recipes in Create Fly; the step name comes from
 * {@link #getDescriptionForAssembly}, registered with {@code AllAssemblyRecipeNames} in {@link CARecipes#register()}.
 */
public record ChargingRecipe(List<ProcessingOutput> results, Ingredient ingredient, int energy, int maxChargeRate)
        implements CreateSingleStackRollableRecipe {
    public static final MapCodec<ChargingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ProcessingOutput.CODEC.listOf(1, 1).fieldOf("results").forGetter(ChargingRecipe::results),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(ChargingRecipe::ingredient),
            Codec.INT.fieldOf("energy").forGetter(ChargingRecipe::energy),
            Codec.INT.fieldOf("max_charge_rate").forGetter(ChargingRecipe::maxChargeRate)
    ).apply(instance, ChargingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargingRecipe> STREAM_CODEC = StreamCodec.composite(
            ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), ChargingRecipe::results,
            Ingredient.CONTENTS_STREAM_CODEC, ChargingRecipe::ingredient,
            ByteBufCodecs.VAR_INT, ChargingRecipe::energy,
            ByteBufCodecs.VAR_INT, ChargingRecipe::maxChargeRate,
            ChargingRecipe::new
    );
    public static final RecipeSerializer<ChargingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public int getEnergy() {
        return energy;
    }

    public int getMaxChargeRate() {
        return maxChargeRate;
    }

    public ItemStack getResultStack() {
        return results.getFirst().create();
    }

    public static Component getDescriptionForAssembly(DynamicOps<JsonElement> ops, JsonObject json) {
        int energy = json.has("energy") ? json.get("energy").getAsInt() : 0;
        return Component.translatable(CreateAddition.MODID + ".recipe.charging.sequence", Util.format(energy));
    }

    @Override
    public RecipeSerializer<ChargingRecipe> getSerializer() {
        return CARecipes.CHARGING;
    }

    @Override
    public RecipeType<ChargingRecipe> getType() {
        return CARecipes.CHARGING_TYPE;
    }
}
