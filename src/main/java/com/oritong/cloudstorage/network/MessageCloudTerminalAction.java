package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MessageCloudTerminalAction(int containerId, long revision, int entry, int button, boolean shift) implements CustomPacketPayload {
    public static final Type<MessageCloudTerminalAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CloudStorage.MODID, "cloud_terminal_action"));
    public static final StreamCodec<FriendlyByteBuf, MessageCloudTerminalAction> CODEC = StreamCodec.of((buf, message) -> {
        buf.writeVarInt(message.containerId);
        buf.writeVarLong(message.revision);
        buf.writeVarInt(message.entry);
        buf.writeVarInt(message.button);
        buf.writeBoolean(message.shift);
    }, buf -> new MessageCloudTerminalAction(buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(MessageCloudTerminalAction message, IPayloadContext context) {
        if (context.player().containerMenu instanceof CloudChestMenu menu) menu.handleTerminalAction(context.player(), message);
    }
}
