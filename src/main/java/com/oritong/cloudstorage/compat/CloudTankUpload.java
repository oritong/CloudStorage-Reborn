package com.oritong.cloudstorage.compat;

import com.oritong.cloudstorage.CloudStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

/** Tag-controlled tank uploads; MI's long storage API is optional and accessed only for its tanks. */
public final class CloudTankUpload {
    public static final TagKey<Block> TAG = TagKey.create(Registries.BLOCK, ResourceLocation.parse("cloudstorage:fluid_storage"));

    private CloudTankUpload() { }

    @Nullable
    public static Tank capture(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!CloudStorage.CONFIG.fluidStorage.get() || !state.is(TAG)) return null;
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null) {
            for (var side : Direction.values()) {
                handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
                if (handler != null) break;
            }
        }
        if (handler == null || handler.getTanks() <= 0) return null;
        long capacity = 0;
        long amount = 0;
        FluidStack fluid = FluidStack.EMPTY;
        for (int i = 0; i < handler.getTanks(); i++) {
            long tankCapacity = Math.max(0, handler.getTankCapacity(i));
            if (tankCapacity > Long.MAX_VALUE - capacity) return null;
            capacity += tankCapacity;
            var contents = handler.getFluidInTank(i);
            if (!contents.isEmpty()) {
                if (!fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid, contents)) return null;
                fluid = contents.copyWithAmount(1);
                amount += contents.getAmount();
            }
        }
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("modern_industrialization")) {
            // IFluidHandler saturates quantum tank capacity and contents at Integer.MAX_VALUE mB.
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) return null;
            try {
                capacity = ((Number) blockEntity.getClass().getMethod("getCapacity").invoke(blockEntity)).longValue();
                amount = ((Number) blockEntity.getClass().getMethod("getAmount").invoke(blockEntity)).longValue();
            } catch (ReflectiveOperationException | LinkageError e) {
                CloudStorage.LOGGER.warn("Cannot read uploaded MI tank at {}", pos, e);
                return null;
            }
        }
        return capacity > 0 && amount >= 0 && amount <= capacity && (amount == 0 || !fluid.isEmpty())
                ? new Tank(capacity, fluid, amount) : null;
    }

    public record Tank(long capacity, FluidStack fluid, long amount) { }
}
