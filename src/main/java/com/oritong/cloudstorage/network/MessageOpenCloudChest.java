package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.CloudStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class MessageOpenCloudChest implements CustomPacketPayload {
    public static final Type<MessageOpenCloudChest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "open_cloud_chest"));
    public static final StreamCodec<FriendlyByteBuf, MessageOpenCloudChest> CODEC = StreamCodec.of((buf, message) -> MessageOpenCloudChest.write(message, buf), MessageOpenCloudChest::read);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }


    public int containerSize;

    public MessageOpenCloudChest(int containerSize) {
        this.containerSize = containerSize;
    }

    public MessageOpenCloudChest() {
    }

    public static MessageOpenCloudChest read(FriendlyByteBuf buf) {
        return new MessageOpenCloudChest(buf.readInt());
    }

    public static void write(MessageOpenCloudChest message, FriendlyByteBuf buf) {
        buf.writeInt(message.containerSize);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageOpenCloudChest message, IPayloadContext context) {

            CloudStorage.PROXY.setVisibleCloudSlots(message.containerSize);
        }
    }
}