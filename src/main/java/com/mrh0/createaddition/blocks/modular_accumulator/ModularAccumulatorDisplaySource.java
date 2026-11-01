package com.mrh0.createaddition.blocks.modular_accumulator;

import java.util.List;

import com.mrh0.createaddition.util.Util;
import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import com.zurrtum.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.zurrtum.create.client.foundation.gui.ModularGuiLineBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public class ModularAccumulatorDisplaySource extends PercentOrProgressBarDisplaySource {

	@Override
	protected MutableComponent formatNumeric(DisplayLinkContext context, Float currentLevel) {
		int mode = getMode(context);
		if (mode == 1)
			return super.formatNumeric(context, currentLevel);
		return Util.getTextComponent(Math.round(currentLevel), "fe");
	}

	private int getMode(DisplayLinkContext context) {
		return context.sourceConfig()
			.getIntOr("Mode", 0);
	}

	@Nullable
	@Override
	protected Float getProgress(DisplayLinkContext context) {
		if (!(context.getSourceBlockEntity() instanceof ModularAccumulatorBlockEntity be)) return null;
		be = be.getControllerBE();
		if(be == null) return null;

		float capacity = be.energyCapability.getMaxEnergyStored();
		float stored = be.energyCapability.getEnergyStored();

		if (capacity == 0) return 0f;

		return switch (getMode(context)) {
			case 0, 1 -> stored / capacity;
			case 2 -> stored;
			case 3 -> capacity;
			case 4 -> capacity - stored;
			default -> 0f;
		};
	}

	@Override
	public boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}

	@Override
	public boolean progressBarActive(DisplayLinkContext context) {
		return getMode(context) == 0;
	}

	// PORT-NOTE(26.1): the configuration widgets are client side, ModularAccumulatorDisplaySourceRender

	@Override
	public String getTranslationKey() {
		return "modular_accumulator";
	}
}
