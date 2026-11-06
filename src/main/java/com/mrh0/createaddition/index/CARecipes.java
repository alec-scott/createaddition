package com.mrh0.createaddition.index;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.recipe.charging.ChargingRecipe;
import com.mrh0.createaddition.recipe.liquid_burning.LiquidBurningRecipe;
import com.mrh0.createaddition.recipe.rolling.RollingRecipe;
import com.zurrtum.create.AllAssemblyRecipeNames;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * PORT-NOTE(26.1): plain registry fields, as Create Fly's AllRecipeTypes/AllRecipeSerializers. The NeoForge
 * {@code has_fluid_tag} condition is gone (the JSON uses {@code fabric:tags_populated}).
 */
public class CARecipes {
	public static final RecipeType<RollingRecipe> ROLLING_TYPE = registerType("rolling");
	public static final RecipeSerializer<RollingRecipe> ROLLING = registerSerializer("rolling", RollingRecipe.SERIALIZER);

	public static final RecipeType<ChargingRecipe> CHARGING_TYPE = registerType("charging");
	public static final RecipeSerializer<ChargingRecipe> CHARGING = registerSerializer("charging", ChargingRecipe.SERIALIZER);

	public static final RecipeType<LiquidBurningRecipe> LIQUID_BURNING_TYPE = registerType("liquid_burning");
	public static final RecipeSerializer<LiquidBurningRecipe> LIQUID_BURNING = registerSerializer("liquid_burning", LiquidBurningRecipe.SERIALIZER);

	private static <T extends Recipe<?>> RecipeType<T> registerType(String id) {
		String name = CreateAddition.MODID + ":" + id;
		return Registry.register(BuiltInRegistries.RECIPE_TYPE, CreateAddition.asResource(id), new RecipeType<T>() {
			@Override
			public String toString() {
				return name;
			}
		});
	}

	private static <T extends Recipe<?>> RecipeSerializer<T> registerSerializer(String id, RecipeSerializer<T> serializer) {
		return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, CreateAddition.asResource(id), serializer);
	}

	public static void register() {
		AllAssemblyRecipeNames.register(CHARGING_TYPE, ChargingRecipe::getDescriptionForAssembly);
		AllAssemblyRecipeNames.register(ROLLING_TYPE, RollingRecipe::getDescriptionForAssembly);
		// Recipe viewers read client-synced recipes (as Create Fly's RecipeCommonPlugin does for its own types)
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
			net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(ROLLING);
			net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(CHARGING);
			net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(LIQUID_BURNING);
		}
	}
}
