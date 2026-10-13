package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.item.CloudBlowerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class MessageLeftClickCloudBlower implements CustomPacketPayload {
    public static final Type<MessageLeftClickCloudBlower> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "left_click_cloud_blower"));
    public static final StreamCodec<FriendlyByteBuf, MessageLeftClickCloudBlower> CODEC = StreamCodec.of((buf, message) -> MessageLeftClickCloudBlower.write(message, buf), MessageLeftClickCloudBlower::read);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }


    public MessageLeftClickCloudBlower() {
    }

    public static MessageLeftClickCloudBlower read(FriendlyByteBuf buf) {
        return new MessageLeftClickCloudBlower();
    }

    public static void write(MessageLeftClickCloudBlower message, FriendlyByteBuf buf) {
    }

    public static class Handler {

        public Handler() {
        }

        public static void handle(MessageLeftClickCloudBlower message, IPayloadContext context) {

            context.enqueueWork(() -> {
                Player player = context.player();
                if (player != null) {
                    CloudBlowerItem.onLeftClick(player, player.getItemInHand(InteractionHand.OFF_HAND));
                    CloudBlowerItem.onLeftClick(player, player.getItemInHand(InteractionHand.MAIN_HAND));
                }
            });
        }
    }
}
