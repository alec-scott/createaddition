package com.mrh0.createaddition.compat.jei.category;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.compat.jei.CreateAdditionJEI;
import com.mrh0.createaddition.index.CAItems;
import com.mrh0.createaddition.recipe.liquid_burning.LiquidBurningRecipe;
import com.mrh0.createaddition.util.ClientMinecraftWrapper;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.client.compat.jei.CreateCategory;
import com.zurrtum.create.client.compat.jei.renderer.IconRenderer;
import com.zurrtum.create.client.foundation.gui.AllGuiTextures;
import com.zurrtum.create.client.foundation.gui.render.BasinBlazeBurnerRenderState;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.content.processing.recipe.HeatCondition;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix3x2f;

import java.util.List;

import static com.mrh0.createaddition.util.Util.formatTime;

public class LiquidBurningCategory extends CreateCategory<RecipeHolder<LiquidBurningRecipe>> {
	private static final int CENTER = 177 / 2;

	@Override
	public IRecipeType<RecipeHolder<LiquidBurningRecipe>> getRecipeType() {
		return CreateAdditionJEI.LIQUID_BURNING;
	}

	@Override
	public Component getTitle() {
		return Component.translatable(CreateAddition.MODID + ".recipe.liquid_burning");
	}

	@Override
	public IDrawable getIcon() {
		return new IconRenderer(AllItems.BLAZE_BURNER);
	}

	@Override
	public int getHeight() {
		return 55;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<LiquidBurningRecipe> entry, IFocusGroup focuses) {
		LiquidBurningRecipe recipe = entry.value();
		List<ItemStack> buckets = recipe.fluidIngredient().getMatchingFluids().stream()
				.map(Fluid::getBucket)
				.map(ItemStack::new)
				.toList();
		builder.addInputSlot(CENTER - 56, 3).setBackground(SLOT, -1, -1).add(new ItemStack(CAItems.STRAW.get()));
		IRecipeSlotBuilder bucketSlot = builder.addInputSlot(CENTER - 36, 3).setBackground(SLOT, -1, -1);
		buckets.forEach(bucketSlot::add);
		addFluidSlot(builder, CENTER - 16, 3, recipe.fluidIngredient());
	}

	@Override
	public void draw(RecipeHolder<LiquidBurningRecipe> entry, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics,
					 double mouseX, double mouseY) {
		LiquidBurningRecipe recipe = entry.value();
		HeatCondition requiredHeat = recipe.superheated() ? HeatCondition.SUPERHEATED : HeatCondition.HEATED;

		graphics.text(ClientMinecraftWrapper.getFont(), formatTime(recipe.burnTime()), CENTER + 48, 36, 0xFFFFFFFF, false);

		AllGuiTextures.JEI_LIGHT.render(graphics, 81, 38);
		AllGuiTextures.JEI_HEAT_BAR.render(graphics, 4, 30);
		graphics.text(ClientMinecraftWrapper.getFont(), CreateLang.translateDirect(requiredHeat.getTranslationKey()), 9, 36,
				requiredHeat.getColor(), false);

		graphics.guiRenderState.addPicturesInPictureState(new BasinBlazeBurnerRenderState(
				new Matrix3x2f(graphics.pose()), 91, 19, requiredHeat.visualizeAsBlazeBurner()));

		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, CENTER + 3, 8);
	}
}
