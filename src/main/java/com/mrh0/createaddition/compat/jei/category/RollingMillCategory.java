package com.mrh0.createaddition.compat.jei.category;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.compat.jei.CreateAdditionJEI;
import com.mrh0.createaddition.compat.jei.render.RollingMillRenderState;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.recipe.rolling.RollingRecipe;
import com.zurrtum.create.client.compat.jei.CreateCategory;
import com.zurrtum.create.client.compat.jei.renderer.IconRenderer;
import com.zurrtum.create.client.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.joml.Matrix3x2f;

public class RollingMillCategory extends CreateCategory<RecipeHolder<RollingRecipe>> {
	@Override
	public IRecipeType<RecipeHolder<RollingRecipe>> getRecipeType() {
		return CreateAdditionJEI.ROLLING;
	}

	@Override
	public Component getTitle() {
		return Component.translatable(CreateAddition.MODID + ".recipe.rolling");
	}

	@Override
	public IDrawable getIcon() {
		return new IconRenderer(CABlocks.ROLLING_MILL.get().asItem());
	}

	@Override
	public int getHeight() {
		return 53;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RollingRecipe> entry, IFocusGroup focuses) {
		RollingRecipe recipe = entry.value();
		builder.addInputSlot(15, 9).setBackground(SLOT, -1, -1).add(recipe.ingredient());
		addChanceSlot(builder, 140, 28, recipe.results().getFirst());
	}

	@Override
	public void draw(RecipeHolder<RollingRecipe> entry, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics,
					 double mouseX, double mouseY) {
		AllGuiTextures.JEI_ARROW.render(graphics, 85, 32);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 43, 4);
		AllGuiTextures.JEI_SHADOW.render(graphics, 32, 40);
		// Block centre at (46, 45), as the old AnimatedRollingMill
		graphics.guiRenderState.addPicturesInPictureState(new RollingMillRenderState(
				new Matrix3x2f(graphics.pose()), 46 - RollingMillRenderState.SIZE / 2, 45 - RollingMillRenderState.SIZE / 2));
	}
}
