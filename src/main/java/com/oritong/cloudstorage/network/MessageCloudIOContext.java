package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The server identifies the opened chest; face edits themselves use the menu's validated buttons. */
public record MessageCloudIOContext(int containerId, BlockPos pos, Direction front) implements CustomPacketPayload {
    public static final Type<MessageCloudIOContext> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CloudStorage.MODID, "cloud_io_context"));
    public static final StreamCodec<FriendlyByteBuf, MessageCloudIOContext> CODEC = StreamCodec.of((buf, message) -> {
        buf.writeVarInt(message.containerId);
        buf.writeBlockPos(message.pos);
        buf.writeEnum(message.front);
    }, buf -> new MessageCloudIOContext(buf.readVarInt(), buf.readBlockPos(), buf.readEnum(Direction.class)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(MessageCloudIOContext message, IPayloadContext context) {
        if (context.player().containerMenu instanceof CloudChestMenu menu && menu.containerId == message.containerId)
            menu.receiveIOContext(message.pos, message.front);
    }
}
