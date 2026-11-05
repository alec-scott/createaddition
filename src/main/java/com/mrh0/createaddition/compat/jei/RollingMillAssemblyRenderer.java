package com.mrh0.createaddition.compat.jei;

import com.mrh0.createaddition.compat.jei.render.RollingMillRenderState;
import com.mrh0.createaddition.recipe.rolling.RollingRecipe;
import com.zurrtum.create.client.compat.jei.category.SequencedAssemblyCategory.SequencedRenderer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.Optional;

/** Rolling step of a sequenced assembly: the animated rolling mill, scaled down like Create Fly's press step. */
public class RollingMillAssemblyRenderer extends SequencedRenderer<RollingRecipe> {
	private static final float SCALE = 0.6f;

	@Override
	public void render(GuiGraphicsExtractor graphics, int i, int x, int y, Optional<IRecipeSlotView> slot) {
		Matrix3x2fStack matrices = graphics.pose();
		matrices.pushMatrix();
		matrices.translate(x, y);
		matrices.scale(SCALE, SCALE);
		matrices.translate(-x, -y);
		// Centre of the mill: 9 px right of and 24 px below the step's slot corner (in scaled pixels)
		int cx = x + Math.round(9 / SCALE);
		int cy = y + Math.round(24 / SCALE);
		graphics.guiRenderState.addPicturesInPictureState(new RollingMillRenderState(
				new Matrix3x2f(matrices), cx - RollingMillRenderState.SIZE / 2, cy - RollingMillRenderState.SIZE / 2));
		matrices.popMatrix();
	}

	@Override
	public IRecipeSlotBuilder addSlot(IRecipeLayoutBuilder builder, int x, int y, RollingRecipe recipe) {
		return null;
	}
}
