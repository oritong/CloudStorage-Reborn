package com.github.alexthe668.cloudstorage.network;

import com.github.alexthe668.cloudstorage.CloudStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class MessageSearchCloudChest implements CustomPacketPayload {
    public static final Type<MessageSearchCloudChest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "search_cloud_chest"));
    public static final StreamCodec<FriendlyByteBuf, MessageSearchCloudChest> CODEC = StreamCodec.of((buf, message) -> MessageSearchCloudChest.write(message, buf), MessageSearchCloudChest::read);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }


    public String search;

    public MessageSearchCloudChest(String search) {
        this.search = search;
    }


    public MessageSearchCloudChest() {
    }

    public static MessageSearchCloudChest read(FriendlyByteBuf buf) {
        return new MessageSearchCloudChest(buf.readUtf());
    }

    public static void write(MessageSearchCloudChest message, FriendlyByteBuf buf) {
        buf.writeUtf(message.search);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageSearchCloudChest message, IPayloadContext context) {

            Player player = context.player();
            if (player != null) {
                CloudStorage.PROXY.processSearchPacket(player, message.search);
            }
        }
    }
}