package com.oritong.cloudstorage.network;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.CloudFluidStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record MessageCloudFluids(int containerId, long revision, long capacity, int offset, int total,
                                 List<CloudFluidStorage.Entry> entries) implements CustomPacketPayload {
    public static final int BATCH_SIZE = 16;
    public static final Type<MessageCloudFluids> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CloudStorage.MODID, "cloud_fluids"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageCloudFluids> CODEC = StreamCodec.of((buf, value) -> {
        buf.writeVarInt(value.containerId);
        buf.writeVarLong(value.revision);
        buf.writeVarLong(value.capacity);
        buf.writeVarInt(value.offset);
        buf.writeVarInt(value.total);
        buf.writeVarInt(value.entries.size());
        for (var entry : value.entries) {
            FluidStack.STREAM_CODEC.encode(buf, entry.fluid());
            buf.writeVarLong(entry.amount());
        }
    }, buf -> {
        int id = buf.readVarInt();
        long revision = buf.readVarLong();
        long capacity = buf.readVarLong();
        int offset = buf.readVarInt();
        int total = buf.readVarInt();
        int size = buf.readVarInt();
        if (capacity < 0 || total < 0 || total > CloudFluidStorage.MAX_TYPES || offset < 0
                || size < 0 || size > BATCH_SIZE || (long) offset + size > total) throw new IllegalArgumentException("Invalid cloud fluid snapshot");
        List<CloudFluidStorage.Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            FluidStack fluid = FluidStack.STREAM_CODEC.decode(buf);
            long amount = buf.readVarLong();
            if (fluid.isEmpty() || amount <= 0) throw new IllegalArgumentException("Invalid cloud fluid amount");
            entries.add(new CloudFluidStorage.Entry(fluid, amount));
        }
        return new MessageCloudFluids(id, revision, capacity, offset, total, entries);
    });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void send(ServerPlayer player, int menuId, CloudFluidStorage storage) {
        var entries = storage.snapshot();
        if (entries.isEmpty()) CloudStorage.sendNonLocal(new MessageCloudFluids(menuId, storage.revision(), storage.capacity(), 0, 0, List.of()), player);
        for (int offset = 0; offset < entries.size(); offset += BATCH_SIZE) {
            CloudStorage.sendNonLocal(new MessageCloudFluids(menuId, storage.revision(), storage.capacity(), offset, entries.size(),
                    entries.subList(offset, Math.min(entries.size(), offset + BATCH_SIZE))), player);
        }
    }

    public static void handle(MessageCloudFluids message, IPayloadContext context) {
        if (context.player().containerMenu instanceof CloudChestMenu menu && menu.containerId == message.containerId) menu.receiveFluids(message);
    }
}
