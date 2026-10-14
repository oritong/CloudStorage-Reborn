package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MessageCloudFluidAction(int containerId, int action, long revision, int entry) implements CustomPacketPayload {
    public static final int ITEMS = 0, FLUIDS = 1, DEPOSIT = 2, WITHDRAW = 3;
    public static final Type<MessageCloudFluidAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CloudStorage.MODID, "cloud_fluid_action"));
    public static final StreamCodec<FriendlyByteBuf, MessageCloudFluidAction> CODEC = StreamCodec.of((buf, value) -> {
        buf.writeVarInt(value.containerId);
        buf.writeVarInt(value.action);
        buf.writeVarLong(value.revision);
        buf.writeVarInt(value.entry);
    }, buf -> new MessageCloudFluidAction(buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(MessageCloudFluidAction message, IPayloadContext context) {
        if (context.player().containerMenu instanceof CloudChestMenu menu && menu.containerId == message.containerId
                && menu.stillValid(context.player())) menu.handleFluidAction(context.player(), message);
    }
}
