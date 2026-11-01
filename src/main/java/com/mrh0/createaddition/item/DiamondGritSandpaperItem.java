package com.mrh0.createaddition.item;

import java.util.function.Consumer;

import com.mrh0.createaddition.config.CommonConfig;
import com.zurrtum.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItemRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;


public class DiamondGritSandpaperItem extends SandPaperItem {
	public DiamondGritSandpaperItem(Properties properties) {
		super(properties);
	}
	
	@Override
	public int getMaxDamage(ItemStack stack) {
		return CommonConfig.DIAMOND_GRIT_SANDPAPER_USES.get();
	}

	// This needs to be redone OnlyIn is never recommended
	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(SimpleCustomRenderer.create(this, new SandPaperItemRenderer()));
	}
}
