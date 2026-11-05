package com.mrh0.createaddition.compat.jei;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.compat.jei.category.ChargingCategory;
import com.mrh0.createaddition.compat.jei.category.LiquidBurningCategory;
import com.mrh0.createaddition.compat.jei.category.RollingMillCategory;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.index.CAItems;
import com.mrh0.createaddition.index.CARecipes;
import com.mrh0.createaddition.recipe.charging.ChargingRecipe;
import com.mrh0.createaddition.recipe.liquid_burning.LiquidBurningRecipe;
import com.mrh0.createaddition.recipe.rolling.RollingRecipe;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.client.compat.jei.JeiClientPlugin;
import com.zurrtum.create.client.compat.jei.category.SequencedAssemblyCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.Internal;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * Registered through the {@code jei_mod_plugin} entrypoint in fabric.mod.json (JEI discovers it, so the mod runs
 * without JEI). Categories follow Create Fly's JeiClientPlugin: they work on {@link RecipeHolder}s.
 */
public class CreateAdditionJEI implements IModPlugin {
	private static final Identifier ID = CreateAddition.asResource("jei_plugin");

	public static final IRecipeType<RecipeHolder<ChargingRecipe>> CHARGING = createRecipeHolderType("charging");
	public static final IRecipeType<RecipeHolder<RollingRecipe>> ROLLING = createRecipeHolderType("rolling");
	public static final IRecipeType<RecipeHolder<LiquidBurningRecipe>> LIQUID_BURNING = createRecipeHolderType("liquid_burning");

	@SuppressWarnings("unchecked")
	private static <T> IRecipeType<T> createRecipeHolderType(String path) {
		return (IRecipeType<T>) IRecipeType.create(CreateAddition.asResource(path), RecipeHolder.class);
	}

	@Override
	public Identifier getPluginUid() {
		return ID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new ChargingCategory(), new RollingMillCategory(), new LiquidBurningCategory());

		// Steps of Create Fly sequenced assembly recipes
		SequencedAssemblyCategory.registerRenderer(CARecipes.CHARGING_TYPE, new ChargingAssemblyRenderer());
		SequencedAssemblyCategory.registerRenderer(CARecipes.ROLLING_TYPE, new RollingMillAssemblyRenderer());
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		RecipeMap recipes = Internal.getClientSyncedRecipes();
		registration.addRecipes(CHARGING, recipes.byType(CARecipes.CHARGING_TYPE).stream().toList());
		registration.addRecipes(ROLLING, recipes.byType(CARecipes.ROLLING_TYPE).stream().toList());
		registration.addRecipes(LIQUID_BURNING, recipes.byType(CARecipes.LIQUID_BURNING_TYPE).stream().toList());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addCraftingStation(CHARGING, CABlocks.TESLA_COIL.get());
		registration.addCraftingStation(ROLLING, CABlocks.ROLLING_MILL.get());
		registration.addCraftingStation(LIQUID_BURNING, AllItems.BLAZE_BURNER);
		registration.addCraftingStation(JeiClientPlugin.SANDPAPER_POLISHING, CAItems.DIAMOND_GRIT_SANDPAPER.get());
	}
}
