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

public class MessageScrollCloudChest implements CustomPacketPayload {
    public static final Type<MessageScrollCloudChest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "scroll_cloud_chest"));
    public static final StreamCodec<FriendlyByteBuf, MessageScrollCloudChest> CODEC = StreamCodec.of((buf, message) -> MessageScrollCloudChest.write(message, buf), MessageScrollCloudChest::read);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }


    public int scrollAmount;

    public MessageScrollCloudChest(int scrollAmount) {
        this.scrollAmount = scrollAmount;
    }


    public MessageScrollCloudChest() {
    }

    public static MessageScrollCloudChest read(FriendlyByteBuf buf) {
        return new MessageScrollCloudChest(buf.readInt());
    }

    public static void write(MessageScrollCloudChest message, FriendlyByteBuf buf) {
        buf.writeInt(message.scrollAmount);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageScrollCloudChest message, IPayloadContext context) {

            Player player = context.player();
            if (player != null) {
                CloudStorage.PROXY.processScrollPacket(player, message.scrollAmount);
            }
        }
    }
}