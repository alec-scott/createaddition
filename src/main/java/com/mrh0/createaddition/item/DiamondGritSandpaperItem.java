package com.mrh0.createaddition.item;

import com.zurrtum.create.content.equipment.sandPaper.SandPaperItem;

/**
 * Behaves like Create's sandpaper. PORT-NOTE(26.1): durability is a data component set when the item is registered
 * (CAItems, from {@code diamond_grit_sandpaper_uses}; the common config is loaded when it is registered, before the
 * items, so a change needs a restart). The use animation is Create Fly's {@code create:model/sand_paper} item model
 * type, selected in {@code items/diamond_grit_sandpaper.json}.
 */
public class DiamondGritSandpaperItem extends SandPaperItem {
	public DiamondGritSandpaperItem(Properties properties) {
		super(properties);
	}
}
