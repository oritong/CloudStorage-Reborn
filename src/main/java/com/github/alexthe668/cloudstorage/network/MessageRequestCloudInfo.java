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

public class MessageRequestCloudInfo implements CustomPacketPayload {
    public static final Type<MessageRequestCloudInfo> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "request_cloud_info"));
    public static final StreamCodec<FriendlyByteBuf, MessageRequestCloudInfo> CODEC = StreamCodec.of((buf, message) -> MessageRequestCloudInfo.write(message, buf), MessageRequestCloudInfo::read);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }


    public int balloonColor;

    public MessageRequestCloudInfo(int balloonColor) {
        this.balloonColor = balloonColor;
    }


    public MessageRequestCloudInfo() {
    }

    public static MessageRequestCloudInfo read(FriendlyByteBuf buf) {
        return new MessageRequestCloudInfo(buf.readInt());
    }

    public static void write(MessageRequestCloudInfo message, FriendlyByteBuf buf) {
        buf.writeInt(message.balloonColor);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageRequestCloudInfo message, IPayloadContext context) {

            Player player = context.player();
            if (player != null) {
                CloudStorage.PROXY.processCloudInfoRequest(player, message.balloonColor);
            }
        }
    }
}