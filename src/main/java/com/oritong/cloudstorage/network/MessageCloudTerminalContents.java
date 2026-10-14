package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.CloudTerminalEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record MessageCloudTerminalContents(int containerId, long revision, int itemCapacity, long fluidCapacity, int fluidTypeCapacity,
                                          int offset, int total, List<CloudTerminalEntry> entries) implements CustomPacketPayload {
    public static final int BATCH_SIZE = 32;
    public static final Type<MessageCloudTerminalContents> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CloudStorage.MODID, "cloud_terminal_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageCloudTerminalContents> CODEC = StreamCodec.of((buf, message) -> {
        buf.writeVarInt(message.containerId);
        buf.writeVarLong(message.revision);
        buf.writeVarInt(message.itemCapacity);
        buf.writeVarLong(message.fluidCapacity);
        buf.writeVarInt(message.fluidTypeCapacity);
        buf.writeVarInt(message.offset);
        buf.writeVarInt(message.total);
        buf.writeVarInt(message.entries.size());
        for (var entry : message.entries) {
            buf.writeBoolean(entry.isFluid());
            buf.writeVarInt(entry.source());
            if (entry.isFluid()) {
                FluidStack.STREAM_CODEC.encode(buf, entry.fluid());
                buf.writeVarLong(entry.amount());
            } else ItemStack.STREAM_CODEC.encode(buf, entry.item());
        }
    }, buf -> {
        int id = buf.readVarInt();
        long revision = buf.readVarLong();
        int itemCapacity = buf.readVarInt();
        long fluidCapacity = buf.readVarLong();
        int fluidTypeCapacity = buf.readVarInt();
        long fluidSources = 256L + fluidTypeCapacity; // Preserve access to legacy fluids without uploaded tanks.
        int offset = buf.readVarInt();
        int total = buf.readVarInt();
        int size = buf.readVarInt();
        if (itemCapacity < 0 || fluidCapacity < 0 || fluidTypeCapacity < 0 || total < 0 || (long) total > (long) itemCapacity + fluidSources
                || offset < 0 || size < 0 || size > BATCH_SIZE || (long) offset + size > total)
            throw new IllegalArgumentException("Invalid mixed cloud snapshot");
        List<CloudTerminalEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            boolean fluid = buf.readBoolean();
            int source = buf.readVarInt();
            if (source < 0 || source >= (fluid ? fluidSources : itemCapacity)) throw new IllegalArgumentException("Invalid cloud source");
            if (fluid) {
                FluidStack stack = FluidStack.STREAM_CODEC.decode(buf);
                long amount = buf.readVarLong();
                if (stack.isEmpty() || amount <= 0) throw new IllegalArgumentException("Invalid cloud fluid");
                entries.add(new CloudTerminalEntry(source, ItemStack.EMPTY, stack, amount));
            } else entries.add(CloudTerminalEntry.item(source, ItemStack.STREAM_CODEC.decode(buf)));
        }
        return new MessageCloudTerminalContents(id, revision, itemCapacity, fluidCapacity, fluidTypeCapacity, offset, total, entries);
    });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void send(ServerPlayer player, CloudChestMenu menu, List<CloudTerminalEntry> entries) {
        int total = entries.size();
        if (total == 0) CloudStorage.sendNonLocal(new MessageCloudTerminalContents(menu.containerId, menu.getTerminalRevision(),
                menu.getItemCapacity(), menu.getFluidCapacity(), menu.getFluidTypeCapacity(), 0, 0, List.of()), player);
        for (int offset = 0; offset < total; offset += BATCH_SIZE) {
            CloudStorage.sendNonLocal(new MessageCloudTerminalContents(menu.containerId, menu.getTerminalRevision(),
                    menu.getItemCapacity(), menu.getFluidCapacity(), menu.getFluidTypeCapacity(), offset, total,
                    entries.subList(offset, Math.min(total, offset + BATCH_SIZE))), player);
        }
    }

    public static void handle(MessageCloudTerminalContents message, IPayloadContext context) {
        if (context.player().containerMenu instanceof CloudChestMenu menu && menu.containerId == message.containerId) menu.receiveTerminal(message);
    }
}
