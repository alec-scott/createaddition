package com.mrh0.createaddition;

import com.mrh0.createaddition.client.CABlockEntityBehaviours;
import com.mrh0.createaddition.event.ClientEventHandler;
import com.mrh0.createaddition.index.CAPartials;
import com.mrh0.createaddition.index.CADisplaySources;
import com.mrh0.createaddition.network.ClientPayloadHandler;
import com.mrh0.createaddition.ponder.CAPonderPlugin;
import com.mrh0.createaddition.trains.schedule.CASchedule;
import com.mrh0.createaddition.port.create.foundation.data.CreateRegistrate;
import com.mrh0.createaddition.port.registrate.util.entry.RegistryEntry;
import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.foundation.item.ItemDescription;
import com.zurrtum.create.client.foundation.item.KineticStats;
import com.zurrtum.create.client.foundation.item.TooltipModifier;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * PORT-NOTE(26.1): was FMLClientSetupEvent and the client-only parts of the mod constructor. Render layers are gone
 * (26.1 takes them from the quads), so the cutout registrations for the tesla coil, barbed wire and light connector
 * are dropped.
 */
public class CreateAdditionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CAPartials.init();
        CreateRegistrate.registerBlockModels();
        CABlockEntityBehaviours.register();
        com.mrh0.createaddition.client.CAMovementRenders.register();
        ClientPayloadHandler.register();
        ClientEventHandler.register();
        PonderIndex.addPlugin(new CAPonderPlugin());
        registerTooltips();
        CASchedule.registerRenders();
        // Display source configuration UIs (Create Fly's AllDisplaySourceRenders)
        com.zurrtum.create.client.AllDisplaySourceRenders.register(CADisplaySources.MODULAR_ACCUMULATOR.get(),
                com.mrh0.createaddition.client.ModularAccumulatorDisplaySourceRender::new);
        com.zurrtum.create.client.AllDisplaySourceRenders.register(CADisplaySources.DIGITAL_ADAPTER.get(),
                com.zurrtum.create.client.content.redstone.displayLink.source.SingleLineDisplaySourceRender::new);
    }

    // Was REGISTRATE.setTooltipModifierFactory in common code; Create Fly's tooltip classes are client only
    private static void registerTooltips() {
        for (RegistryEntry<Item, Item> entry : CreateAddition.REGISTRATE.<Item, Item>getAll(Registries.ITEM)) {
            Item item = entry.get();
            if (TooltipModifier.REGISTRY.get(item) != null) continue;
            TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
        }
    }
}
