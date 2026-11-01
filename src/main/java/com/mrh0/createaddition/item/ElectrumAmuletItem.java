package com.mrh0.createaddition.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.index.CAEffects;
import com.mrh0.createaddition.index.CASounds;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.mrh0.createaddition.port.neoforge.capabilities.Capabilities;
import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;

public class ElectrumAmuletItem extends Item {
    private static int CHARGE_CHANCE = 300;
    
    public ElectrumAmuletItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide()) return;
        if (!CommonConfig.AMULET_EFFECT_ENABLED.get()) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack toCharge;
        if (mainHand == stack) toCharge = offHand;
        else if (offHand == stack) toCharge = mainHand;
        else return;

        IEnergyStorage es = Capabilities.getCapability(toCharge, Capabilities.EnergyStorage.ITEM);
        boolean canCharge = es != null && es.receiveEnergy(1, true) > 0;

        if (canCharge && level.getRandom().nextInt(CHARGE_CHANCE) == 0) {
            es.receiveEnergy(CHARGE_CHANCE * CommonConfig.ELECTRUM_AMULET_CHARGE_RATE.get(), false);
            player.addEffect(new MobEffectInstance(CAEffects.SHOCKING, 40, 0, false, true, true));
            if (CommonConfig.AUDIO_ENABLED.get())
                level.playSound(null, player.blockPosition(), CASounds.LITTLE_ZAP.get(), SoundSource.PLAYERS, 0.4f, 1f);
        }
    }
}
