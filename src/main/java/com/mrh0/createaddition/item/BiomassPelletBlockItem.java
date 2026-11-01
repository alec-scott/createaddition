package com.mrh0.createaddition.item;

import com.mrh0.createaddition.index.CAItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

public class BiomassPelletBlockItem extends BlockItem {

	public BiomassPelletBlockItem(Block pBlock, Properties pProperties) {
		super(pBlock, pProperties);
	}

	// PORT-NOTE(26.1): burn time (9 pellets) is registered with Fabric's FuelValueEvents in CreateAddition
}
