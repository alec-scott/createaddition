package com.mrh0.createaddition.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class BiomassPelletItem extends Item {

	public BiomassPelletItem(Properties props) {
		super(props);
	}

	// PORT-NOTE(26.1): burn time (6400) is registered with Fabric's FuelValueEvents in CreateAddition
}
