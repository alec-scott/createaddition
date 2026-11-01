package com.mrh0.createaddition.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** PORT-NOTE(26.1): Fabric client receivers; payload types are registered in {@link ServerPayloadHandler#register()}. */
public class ClientPayloadHandler {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EnergyNetworkPacketPayload.TYPE,
                (pkt, ctx) -> EnergyNetworkPacketPayload.updateClientCache(pkt.pos(), pkt.demand(), pkt.buff()));
        ClientPlayNetworking.registerGlobalReceiver(TimeRemainingPacketPayload.TYPE,
                (pkt, ctx) -> TimeRemainingPacketPayload.updateClientCache(pkt.timeRemaining()));
    }
}
