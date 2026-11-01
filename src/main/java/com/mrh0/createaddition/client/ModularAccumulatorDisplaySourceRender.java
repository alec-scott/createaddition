package com.mrh0.createaddition.client;

import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.client.content.redstone.displayLink.source.SingleLineDisplaySourceRender;
import com.zurrtum.create.client.foundation.gui.ModularGuiLineBuilder;
import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import net.minecraft.network.chat.Component;

import java.util.List;

/** PORT-NOTE(26.1): the accumulator display source's configuration widgets (client side in Create Fly). */
public class ModularAccumulatorDisplaySourceRender extends SingleLineDisplaySourceRender {
    @Override
    public void initConfigurationWidgets(DisplaySource source, DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
        super.initConfigurationWidgets(source, context, builder, isFirstLine);
        if (isFirstLine) return;
        builder.addSelectionScrollInput(0, 120,
                (si, l) -> si.forOptions(List.of(
                                Component.translatable("createaddition.display_source.accumulator.progress_bar"),
                                Component.translatable("createaddition.display_source.accumulator.percent"),
                                Component.translatable("createaddition.display_source.accumulator.current"),
                                Component.translatable("createaddition.display_source.accumulator.max"),
                                Component.translatable("createaddition.display_source.accumulator.remaining")))
                        .titled(Component.translatable("createaddition.display_source.accumulator.display")), "Mode");
    }
}
