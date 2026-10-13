package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.CloudStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class MessageSortCloudChest implements CustomPacketPayload {
    public static final Type<MessageSortCloudChest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "sort_cloud_chest"));
    public static final StreamCodec<FriendlyByteBuf, MessageSortCloudChest> CODEC = StreamCodec.of((buf, message) -> MessageSortCloudChest.write(message, buf), MessageSortCloudChest::read);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }


    public int type;

    public MessageSortCloudChest(int type) {
        this.type = type;
    }


    public MessageSortCloudChest() {
    }

    public static MessageSortCloudChest read(FriendlyByteBuf buf) {
        return new MessageSortCloudChest(buf.readInt());
    }

    public static void write(MessageSortCloudChest message, FriendlyByteBuf buf) {
        buf.writeInt(message.type);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageSortCloudChest message, IPayloadContext context) {

            Player player = context.player();
            if (player != null) {
                CloudStorage.PROXY.processSortPacket(player, message.type);
            }
        }
    }
}