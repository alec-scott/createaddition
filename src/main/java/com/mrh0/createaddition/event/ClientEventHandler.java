package com.mrh0.createaddition.event;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.debug.CADebugger;
import com.mrh0.createaddition.item.WireSpool;
import com.mrh0.createaddition.network.ObservePacketPayload;
import com.mrh0.createaddition.sound.CASoundScapes;
import com.mrh0.createaddition.util.ClientMinecraftWrapper;
import com.mrh0.createaddition.util.Util;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;

/** PORT-NOTE(26.1): NeoForge client subscribers → Fabric callbacks, registered from {@link #register()}. */
public class ClientEventHandler {

    public static boolean clientRenderHeldWire = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            playerRendererEvent();
            CASoundScapes.tick();
            // Was GameEvents.clientTickEvent
            ObservePacketPayload.tick();
            CADebugger.tick();
        });
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Listener());
    }

    private static void playerRendererEvent() {
        if (ClientMinecraftWrapper.getPlayer() == null) return;
        ItemStack stack = ClientMinecraftWrapper.getPlayer().getInventory().getSelectedItem();
        if (stack.isEmpty()) return;
        if (WireSpool.isRemover(stack.getItem())) return;
        clientRenderHeldWire = Util.getWireNodeOfSpools(stack) != null;
    }

    private static class Listener extends ResourceReloadListener implements IdentifiableResourceReloadListener {
        private static final Identifier ID = CreateAddition.asResource("soundscapes");

        @Override
        public Identifier getFabricId() {
            return ID;
        }
    }
}
