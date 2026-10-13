package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.inventory.CloudChestMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Locale;

public record MessageRequestCloudSearch(int containerId, int requestId, String search) implements CustomPacketPayload {
    public static final Type<MessageRequestCloudSearch> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "request_cloud_search"));
    public static final StreamCodec<FriendlyByteBuf, MessageRequestCloudSearch> CODEC = StreamCodec.of(
            (buf, message) -> {
                buf.writeVarInt(message.containerId);
                buf.writeVarInt(message.requestId);
                buf.writeUtf(message.search, 50);
            }, buf -> new MessageRequestCloudSearch(buf.readVarInt(), buf.readVarInt(), buf.readUtf(50)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MessageRequestCloudSearch message, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof CloudChestMenu menu
                && menu.containerId == message.containerId) {
            String query = message.search.toLowerCase(Locale.ROOT);
            if (query.isEmpty() || query.startsWith("@") || query.startsWith("#")) {
                menu.search(player, query);
            } else {
                MessageCloudSearchContents.send(player, menu.containerId, menu.beginClientSearch(message.requestId, query));
            }
        }
    }
}
