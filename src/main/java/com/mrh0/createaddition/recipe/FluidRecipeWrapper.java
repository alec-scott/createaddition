package com.mrh0.createaddition.recipe;

import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** PORT-NOTE(26.1): a fluid-only recipe input (was a NeoForge Container wrapper). */
public record FluidRecipeWrapper(FluidStack fluid) implements RecipeInput {
	@Override
	public ItemStack getItem(int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return fluid == null || fluid.isEmpty();
	}
}
