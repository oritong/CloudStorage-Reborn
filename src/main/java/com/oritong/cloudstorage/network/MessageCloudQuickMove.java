package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A stable source slot keeps rapid drag transfers independent of intermediate list snapshots. */
public record MessageCloudQuickMove(int containerId, int source, ItemStack expected) implements CustomPacketPayload {
    public static final Type<MessageCloudQuickMove> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CloudStorage.MODID, "cloud_quick_move"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageCloudQuickMove> CODEC = StreamCodec.of((buf, message) -> {
        buf.writeVarInt(message.containerId);
        buf.writeVarInt(message.source);
        ItemStack.STREAM_CODEC.encode(buf, message.expected);
    }, buf -> new MessageCloudQuickMove(buf.readVarInt(), buf.readVarInt(), ItemStack.STREAM_CODEC.decode(buf)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(MessageCloudQuickMove message, IPayloadContext context) {
        if (context.player().containerMenu instanceof CloudChestMenu menu) menu.handleQuickMove(context.player(), message);
    }
}
