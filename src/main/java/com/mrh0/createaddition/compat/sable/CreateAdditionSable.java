package com.mrh0.createaddition.compat.sable;

import dev.ryanhcode.sable.fabric.event.FabricSablePostPhysicsTickEvent;

/**
 * PORT-NOTE(26.1): only called when the "sable" mod is loaded ({@code CreateAddition.SABLE_ACTIVE}); NeoForge's
 * {@code ForgeSablePostPhysicsTickEvent} is Sable's Fabric event here.
 */
public class CreateAdditionSable {
    public static void register() {
        FabricSablePostPhysicsTickEvent.EVENT.register(SableEvents::onPostPhysicsTick);
    }
}
