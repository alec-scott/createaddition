package com.mrh0.createaddition.item;

import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.index.CAItems;
import com.mrh0.createaddition.port.neoforge.capabilities.Capabilities;
import com.mrh0.createaddition.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;
import com.mrh0.createaddition.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.function.Consumer;

/**
 * PORT-NOTE(26.1): energy is stored in Team Reborn Energy's {@code team_reborn_energy:energy} component (was an
 * "energy" int in custom data) and exposed through {@link SimpleEnergyItem#createStorage}.
 */
public class CapacitorItem extends Item {
    public CapacitorItem(Properties props) {
        super(props.stacksTo(16));
    }

    public static int getEnergy(ItemStack stack) {
        return (int) Math.min(Integer.MAX_VALUE, SimpleEnergyItem.getStoredEnergyUnchecked(stack));
    }

    public static void setEnergy(ItemStack stack, int energy) {
        SimpleEnergyItem.setStoredEnergyUnchecked(stack, Math.max(0, Math.min(energy, CommonConfig.CAPACITOR_CAPACITY.get())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        IEnergyStorage es = Capabilities.getCapability(stack, Capabilities.EnergyStorage.ITEM);
        if (es != null) tooltip.accept(Util.getTextComponent(es));
    }

    // PORT-NOTE(26.1): getMaxStackSize(stack) is gone (max stack size is a component); charged capacitors still stack to 16

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((float) getEnergy(stack) / CommonConfig.CAPACITOR_CAPACITY.get() * 13f);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00BFFF;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> SimpleEnergyItem.createStorage(ctx, CommonConfig.CAPACITOR_CAPACITY.get(),
                    CommonConfig.CAPACITOR_CHARGE_RATE.get(), CommonConfig.CAPACITOR_CHARGE_RATE.get()),
            CAItems.CAPACITOR.get()
        );
    }
}
