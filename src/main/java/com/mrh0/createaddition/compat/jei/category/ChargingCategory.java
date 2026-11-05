package com.mrh0.createaddition.compat.jei.category;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.blocks.tesla_coil.TeslaCoilBlock;
import com.mrh0.createaddition.compat.jei.CreateAdditionJEI;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.recipe.charging.ChargingRecipe;
import com.mrh0.createaddition.util.ClientMinecraftWrapper;
import com.mrh0.createaddition.util.Util;
import com.zurrtum.create.client.catnip.gui.element.GuiGameElement;
import com.zurrtum.create.client.compat.jei.CreateCategory;
import com.zurrtum.create.client.compat.jei.renderer.IconRenderer;
import com.zurrtum.create.client.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ChargingCategory extends CreateCategory<RecipeHolder<ChargingRecipe>> {
	private static final int BLOCK = 22;
	private static final int PADDING = 16;
	private static final int BOX = BLOCK + PADDING;

	private final GuiGameElement.GuiBlockStateRenderBuilder teslaCoil = GuiGameElement.of(CABlocks.TESLA_COIL.get().defaultBlockState()
					.setValue(TeslaCoilBlock.FACING, Direction.DOWN)
					.setValue(TeslaCoilBlock.POWERED, true))
			.rotate(-22.5f, 22.5f, 0)
			.scale(BLOCK / 16f)
			.padding(PADDING);

	@Override
	public IRecipeType<RecipeHolder<ChargingRecipe>> getRecipeType() {
		return CreateAdditionJEI.CHARGING;
	}

	@Override
	public Component getTitle() {
		return Component.translatable(CreateAddition.MODID + ".recipe.charging");
	}

	@Override
	public IDrawable getIcon() {
		return new IconRenderer(CABlocks.TESLA_COIL.get().asItem());
	}

	@Override
	public int getHeight() {
		return 53;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ChargingRecipe> entry, IFocusGroup focuses) {
		ChargingRecipe recipe = entry.value();
		builder.addInputSlot(15, 9).setBackground(SLOT, -1, -1).add(recipe.ingredient());
		addChanceSlot(builder, 140, 28, recipe.results().getFirst());
	}

	@Override
	public void draw(RecipeHolder<ChargingRecipe> entry, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics,
					 double mouseX, double mouseY) {
		AllGuiTextures.JEI_ARROW.render(graphics, 85, 32);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 43, 4);
		AllGuiTextures.JEI_SHADOW.render(graphics, 32, 40);
		// Block centre at (46, 45), as the old AnimatedTeslaCoil
		teslaCoil.at(46 - BOX / 2f, 45 - BOX / 2f).render(graphics);
		graphics.text(ClientMinecraftWrapper.getFont(), Util.format(entry.value().energy()) + "⚡", 86, 9, 0xFFFFFFFF, false);
	}
}
