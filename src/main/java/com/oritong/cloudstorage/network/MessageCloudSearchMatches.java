package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.inventory.CloudChestMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.BitSet;

public record MessageCloudSearchMatches(int containerId, int requestId, BitSet matches) implements CustomPacketPayload {
    public static final Type<MessageCloudSearchMatches> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "cloud_search_matches"));
    public static final StreamCodec<FriendlyByteBuf, MessageCloudSearchMatches> CODEC = StreamCodec.of(
            (buf, message) -> {
                buf.writeVarInt(message.containerId);
                buf.writeVarInt(message.requestId);
                buf.writeBitSet(message.matches);
            }, buf -> new MessageCloudSearchMatches(buf.readVarInt(), buf.readVarInt(), buf.readBitSet()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MessageCloudSearchMatches message, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof CloudChestMenu menu
                && menu.containerId == message.containerId && !menu.finishClientSearch(player, message.requestId, message.matches)) {
            var pending = menu.getPendingClientSearch(message.requestId);
            if (pending != null) {
                MessageCloudSearchContents.send(player, menu.containerId, menu.beginClientSearch(pending.requestId(), pending.search()));
            }
        }
    }
}
