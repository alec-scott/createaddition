package com.mrh0.createaddition.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** PORT-NOTE(26.1): Fabric payloads (was NeoForge's PayloadRegistrar with DirectionalPayloadHandler). */
public class ServerPayloadHandler {
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ObservePacketPayload.TYPE, ObservePacketPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnergyNetworkPacketPayload.TYPE, EnergyNetworkPacketPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TimeRemainingPacketPayload.TYPE, TimeRemainingPacketPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ObservePacketPayload.TYPE, (pkt, ctx) -> handleObservePayload(pkt, ctx.player()));
    }

    public static void handleObservePayload(final ObservePacketPayload pkt, final ServerPlayer player) {
        try {
            sendUpdate(pkt, player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendUpdate(ObservePacketPayload pkt, ServerPlayer player) {
        BlockEntity be = player.level().getBlockEntity(pkt.pos());
        if (be instanceof IObserveBlockEntity ote) {
            ote.onObserved(player, pkt);
            Packet<ClientGamePacketListener> updatePacket = be.getUpdatePacket();
            if (updatePacket != null) player.connection.send(updatePacket);
        }
    }
}
