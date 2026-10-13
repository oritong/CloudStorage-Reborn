package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record MessageCloudSearchContents(int containerId, int requestId, String search, int offset, int total,
                                       List<ItemStack> items) implements CustomPacketPayload {
    public static final int BATCH_SIZE = 32;
    public static final Type<MessageCloudSearchContents> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "cloud_search_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageCloudSearchContents> CODEC = StreamCodec.of(
            (buf, message) -> {
                buf.writeVarInt(message.containerId);
                buf.writeVarInt(message.requestId);
                buf.writeUtf(message.search, 50);
                buf.writeVarInt(message.offset);
                buf.writeVarInt(message.total);
                buf.writeVarInt(message.items.size());
                for (ItemStack stack : message.items) ItemStack.STREAM_CODEC.encode(buf, stack);
            }, buf -> {
                int containerId = buf.readVarInt();
                int requestId = buf.readVarInt();
                String search = buf.readUtf(50);
                int offset = buf.readVarInt();
                int total = buf.readVarInt();
                int size = buf.readVarInt();
                if (size < 0 || size > BATCH_SIZE) throw new IllegalArgumentException("Invalid cloud search batch size");
                List<ItemStack> items = new ArrayList<>(size);
                for (int i = 0; i < size; i++) items.add(ItemStack.STREAM_CODEC.decode(buf));
                return new MessageCloudSearchContents(containerId, requestId, search, offset, total, items);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, int containerId, CloudChestMenu.SearchSnapshot snapshot) {
        int total = snapshot.items().size();
        if (total == 0) {
            CloudStorage.sendNonLocal(new MessageCloudSearchContents(containerId, snapshot.requestId(), snapshot.search(), 0, 0, List.of()), player);
        }
        for (int offset = 0; offset < total; offset += BATCH_SIZE) {
            var items = snapshot.items().subList(offset, Math.min(offset + BATCH_SIZE, total));
            CloudStorage.sendNonLocal(new MessageCloudSearchContents(containerId, snapshot.requestId(), snapshot.search(), offset, total, items), player);
        }
    }

    public static void handle(MessageCloudSearchContents message, IPayloadContext context) {
        CloudStorage.PROXY.receiveCloudSearchContents(message);
    }
}
