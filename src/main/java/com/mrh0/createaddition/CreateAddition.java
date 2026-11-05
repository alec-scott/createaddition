package com.mrh0.createaddition;

import com.mojang.brigadier.CommandDispatcher;
import com.mrh0.createaddition.commands.CCApiCommand;
import com.mrh0.createaddition.commands.WireGraphCommand;
import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.event.GameEvents;
import com.mrh0.createaddition.index.*;
import com.mrh0.createaddition.network.ServerPayloadHandler;
import com.mrh0.createaddition.port.create.CreateTransforms;
import com.mrh0.createaddition.port.create.foundation.data.CreateRegistrate;
import com.mrh0.createaddition.trains.schedule.CASchedule;
import com.zurrtum.create.api.boiler.BoilerHeater;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PORT-NOTE(26.1): the NeoForge {@code @Mod} constructor and mod-bus listeners became this {@link ModInitializer}; client
 * setup (ponder, partials, tooltips) is in {@link CreateAdditionClient}.
 */
public class CreateAddition implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "createaddition";

    public static boolean IE_ACTIVE = false;
    public static boolean CC_ACTIVE = false;
    public static boolean AE2_ACTIVE = false;
    public static boolean MEK_ACTIVE = false;
    public static boolean SIM_ACTIVE = false;
    public static boolean SABLE_ACTIVE = false;

    // PORT-NOTE(26.1): the tooltip modifier factory moved to CreateAdditionClient (Create Fly's tooltip classes are client only)
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(CreateAddition.MODID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    public static final ResourceKey<CreativeModeTab> MAIN_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, asResource(MODID));

    private static final ItemLike[] excludedItemsList = new ItemLike[]{
            CAItems.CAKE_BASE,
            CAItems.CAKE_BASE_BAKED,
            CAItems.BIOMASS
    };

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        IE_ACTIVE = loader.isModLoaded("immersiveengineering");
        CC_ACTIVE = loader.isModLoaded("computercraft");
        AE2_ACTIVE = loader.isModLoaded("ae2");
        MEK_ACTIVE = loader.isModLoaded("mekanism");
        // PORT-NOTE(26.1): Sable Companion ("sablecompanion" on NeoForge) is bundled in the Sable jar on Fabric.
        SIM_ACTIVE = loader.isModLoaded("simulated");
        SABLE_ACTIVE = loader.isModLoaded("sable");
        if (SABLE_ACTIVE) com.mrh0.createaddition.compat.sable.CreateAdditionSable.register();

        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.COMMON, CommonConfig.COMMON_CONFIG);

        CABlocks.register();
        CABlockEntities.register();
        CAItems.register();
        registerCreativeTab();
        CAFluids.register();
        CAEffects.register();
        CARecipes.register();
        CASounds.register();
        CASchedule.register();
        CADamageTypes.register();
        CADisplaySources.register();

        REGISTRATE.register();
        CreateTransforms.apply();

        CAArmInteractions.register();
        CACapabilities.register();
        ServerPayloadHandler.register();
        GameEvents.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));

        BoilerHeater.REGISTRY.register(CABlocks.LIQUID_BLAZE_BURNER.get(), (level, pos, state) -> {
            BlazeBurnerBlock.HeatLevel value = state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
            if (value == BlazeBurnerBlock.HeatLevel.NONE) return -1;
            if (value == BlazeBurnerBlock.HeatLevel.SEETHING) return 2;
            if (value.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) return 1;
            return 0;
        });

        LOGGER.info("Create Crafts & Additions Initialized!");
    }

    // PORT-NOTE(26.1): Fabric tab registration (was a NeoForge DeferredRegister; tab ordering is Fabric's default)
    private static void registerCreativeTab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_TAB, FabricCreativeModeTab.builder()
                .icon(() -> CABlocks.ELECTRIC_MOTOR.get().asItem().getDefaultInstance())
                .title(Component.translatable("itemGroup.createaddition.main"))
                .displayItems((itemDisplayParameters, output) -> REGISTRATE.<Item, Item>getAll(Registries.ITEM).forEach((item -> {
                    for (ItemLike excluded : excludedItemsList) {
                        if (item.get() == excluded.asItem()) {
                            output.accept(item.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
                            return;
                        }
                    }
                    output.accept(item.get());
                })))
                .build());
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        CCApiCommand.register(dispatcher);
        WireGraphCommand.register(dispatcher);
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
