package com.mrh0.createaddition.compat.jei;

import com.mrh0.createaddition.blocks.tesla_coil.TeslaCoilBlock;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.recipe.charging.ChargingRecipe;
import com.zurrtum.create.client.catnip.gui.element.GuiGameElement;
import com.zurrtum.create.client.compat.jei.category.SequencedAssemblyCategory.SequencedRenderer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;

import java.util.Optional;

/** Charging step of a sequenced assembly: a powered Tesla coil. The step name comes from AllAssemblyRecipeNames. */
public class ChargingAssemblyRenderer extends SequencedRenderer<ChargingRecipe> {
	private static final int BLOCK = 18;
	private static final int PADDING = 14;
	private static final int BOX = BLOCK + PADDING;

	private final GuiGameElement.GuiBlockStateRenderBuilder coil = GuiGameElement.of(CABlocks.TESLA_COIL.get().defaultBlockState()
					.setValue(TeslaCoilBlock.FACING, Direction.UP)
					.setValue(TeslaCoilBlock.POWERED, true))
			.rotate(-22.5f, 22.5f, 0)
			.scale(BLOCK / 16f)
			.padding(PADDING);

	@Override
	public void render(GuiGraphicsExtractor graphics, int i, int x, int y, Optional<IRecipeSlotView> slot) {
		coil.at(x + 9 - BOX / 2f, y + 24 - BOX / 2f).render(graphics);
	}

	@Override
	public IRecipeSlotBuilder addSlot(IRecipeLayoutBuilder builder, int x, int y, ChargingRecipe recipe) {
		return null;
	}
}
