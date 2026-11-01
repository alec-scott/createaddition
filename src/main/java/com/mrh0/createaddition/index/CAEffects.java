package com.mrh0.createaddition.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.effect.ShockingEffect;
import net.neoforged.bus.api.IEventBus;
import com.mrh0.createaddition.port.porting_lib.util.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class CAEffects {
	public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, CreateAddition.MODID);
	public static final DeferredHolder<MobEffect, MobEffect> SHOCKING = EFFECTS.register("shocking", () -> new ShockingEffect()
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, CreateAddition.asResource("shocking"), (double)-100f, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	
	public static void register(IEventBus bus) {
		EFFECTS.register(bus);
	}
}
