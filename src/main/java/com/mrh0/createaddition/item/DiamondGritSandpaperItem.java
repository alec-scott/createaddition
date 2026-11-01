package com.mrh0.createaddition.item;

import java.util.function.Consumer;

import com.mrh0.createaddition.config.CommonConfig;
import com.zurrtum.create.content.equipment.sandPaper.SandPaperItem;

import net.minecraft.world.item.ItemStack;


public class DiamondGritSandpaperItem extends SandPaperItem {
	public DiamondGritSandpaperItem(Properties properties) {
		super(properties);
	}
	
	// PORT-NOTE(26.1): durability is a component now, set from the config default in CAItems (config is not loaded at
	// registration); the config value is no longer applied at runtime.

	// TODO(26.1): the animated sandpaper item renderer (was IClientItemExtensions + SandPaperItemRenderer)
}
