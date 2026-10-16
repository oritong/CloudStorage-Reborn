package com.oritong.cloudstorage.inventory;

import com.oritong.cloudstorage.block.AbstractCloudChestBlockEntity;
import com.oritong.cloudstorage.block.CloudChestBlock;
import com.oritong.cloudstorage.misc.CloudFunction;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.UUID;

/** Settings belong to this chest. Cached handlers always recheck the current face and cloud. */
public final class CloudChestAutomation {
    public static final int NONE = 0, INPUT = 1, OUTPUT = 2, BOTH = 3;
    private final AbstractCloudChestBlockEntity chest;
    private final int[] modes = new int[6];
    private final EnumMap<Direction, IItemHandler> items = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, IFluidHandler> fluids = new EnumMap<>(Direction.class);
    private final Container empty = new net.minecraft.world.SimpleContainer(0);
    private UUID owner;
    private boolean collectDrops;
    private int collectionOffset;

    public CloudChestAutomation(AbstractCloudChestBlockEntity chest) {
        this.chest = chest;
        for (Direction side : Direction.values()) {
            if (side == Direction.UP) continue;
            items.put(side, new ItemSide(side));
            fluids.put(side, new FluidSide(side));
        }
    }

    public Direction front() { return chest.getBlockState().getValue(CloudChestBlock.HORIZONTAL_FACING); }
    public int mode(CloudIOSide side) { return side == CloudIOSide.TOP ? NONE : modes[side.ordinal()]; }
    public int mode(Direction direction) {
        for (var side : CloudIOSide.values()) if (side.direction(front()) == direction) return mode(side);
        return NONE;
    }
    public boolean collectDrops() { return collectDrops; }
    public int settings() {
        int state = collectDrops ? 4096 : 0;
        for (var side : CloudIOSide.values()) state |= mode(side) << (side.ordinal() * 2);
        return state;
    }
    public void setMode(Player player, CloudIOSide side, int mode) {
        if (side == CloudIOSide.TOP || mode < NONE || mode > BOTH) return;
        owner = player.getUUID();
        modes[side.ordinal()] = mode;
        changed();
    }
    public void cycle(Player player, CloudIOSide side, int step) { setMode(player, side, Math.floorMod(mode(side) + step, 4)); }
    public void batch(Player player, int step) {
        int common = -1;
        for (var side : CloudIOSide.values()) if (side != CloudIOSide.TOP) {
            if (common == -1) common = mode(side);
            else if (common != mode(side)) { common = -2; break; }
        }
        int target = step == 0 || common < 0 ? NONE : Math.floorMod(common + step, 4);
        for (var side : CloudIOSide.values()) if (side != CloudIOSide.TOP) modes[side.ordinal()] = target;
        owner = player.getUUID();
        changed();
    }
    public void toggleCollection(Player player) {
        owner = player.getUUID();
        collectDrops = !collectDrops;
        changed();
    }
    public void changed() {
        chest.setChanged();
        if (chest.getLevel() != null) {
            chest.getLevel().invalidateCapabilities(chest.getBlockPos());
            chest.getLevel().sendBlockUpdated(chest.getBlockPos(), chest.getBlockState(), chest.getBlockState(), 3);
        }
    }
    public void load(CompoundTag tag) {
        int settings = tag.getInt("CloudIO");
        for (var side : CloudIOSide.values()) modes[side.ordinal()] = side == CloudIOSide.TOP ? NONE : (settings >> (side.ordinal() * 2)) & 3;
        collectDrops = (settings & 4096) != 0;
        owner = tag.hasUUID("CloudIOOwner") ? tag.getUUID("CloudIOOwner") : null;
    }
    public void save(CompoundTag tag) {
        tag.putInt("CloudIO", settings());
        if (owner != null) tag.putUUID("CloudIOOwner", owner);
    }
    @Nullable private CloudIndex cloud() {
        if (chest.isRemoved() || chest.getLevel() == null || chest.getLevel().isClientSide() || !chest.hasClearance()) return null;
        CloudIndex cloud = chest.getAutomationCloud(owner);
        return cloud != null && cloud.hasFunction(CloudFunction.HOPPER) ? cloud : null;
    }
    private void dirty() {
        CSWorldData data = CSWorldData.get(chest.getLevel());
        if (data != null) data.setDirty();
    }
    @Nullable public IItemHandler itemHandler(@Nullable Direction side) {
        return side == null || side == Direction.UP || chest.isRemoved() ? null : items.get(side);
    }
    @Nullable public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side == Direction.UP || chest.isRemoved() ? null : fluids.get(side);
    }
    public void tick() {
        if (!collectDrops || chest.tickCount % 8 != 0 || !chest.hasClearance()) return;
        CloudIndex cloud = cloud();
        if (cloud == null || cloud.getContainerSize() == 0) return;
        var pos = chest.getBlockPos();
        var area = new AABB(pos.getX(), pos.getY() + 0.875, pos.getZ(), pos.getX() + 1, pos.getY() + 1.875, pos.getZ() + 1);
        var target = new InvWrapper(cloud.getContainer());
        var entities = chest.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.getItem().isEmpty());
        int budget = Math.min(32, entities.size());
        for (int i = 0; i < budget; i++) {
            var entity = entities.get(Math.floorMod(collectionOffset + i, entities.size()));
            ItemStack original = entity.getItem();
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, original.copy(), false);
            if (remaining.getCount() == original.getCount()) continue;
            if (remaining.isEmpty()) entity.discard();
            else entity.setItem(remaining);
            dirty();
        }
        if (!entities.isEmpty()) collectionOffset = Math.floorMod(collectionOffset + budget, entities.size());
    }

    private final class ItemSide implements IItemHandler {
        private final Direction side;
        private final InvWrapper delegate;
        ItemSide(Direction side) {
            this.side = side;
            delegate = new InvWrapper(empty) {
                @Override public Container getInv() {
                    CloudIndex current = mode(side) == NONE ? null : cloud();
                    return current == null ? empty : current.getContainer();
                }
            };
        }
        @Override public int getSlots() { return delegate.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return slot < 0 || slot >= getSlots() ? ItemStack.EMPTY : delegate.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return slot < 0 || slot >= getSlots() ? 0 : delegate.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return (mode(side) & INPUT) != 0 && slot >= 0 && slot < getSlots() && delegate.isItemValid(slot, stack);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if ((mode(side) & INPUT) == 0 || slot < 0 || slot >= getSlots()) return stack;
            ItemStack remaining = delegate.insertItem(slot, stack, simulate);
            if (!simulate && remaining.getCount() != stack.getCount()) dirty();
            return remaining;
        }
        @Override public ItemStack extractItem(int slot, int count, boolean simulate) {
            if ((mode(side) & OUTPUT) == 0 || slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
            ItemStack result = delegate.extractItem(slot, count, simulate);
            if (!simulate && !result.isEmpty()) dirty();
            return result;
        }
    }

    private final class FluidSide implements IFluidHandler {
        private final Direction side;
        FluidSide(Direction side) { this.side = side; }
        private CloudFluidStorage storage() {
            CloudIndex current = mode(side) == NONE ? null : cloud();
            return current == null ? null : current.getFluids();
        }
        @Override public int getTanks() { var s = storage(); return s == null ? 0 : s.getTanks(); }
        @Override public FluidStack getFluidInTank(int tank) { var s = storage(); return s == null ? FluidStack.EMPTY : s.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { var s = storage(); return s == null ? 0 : s.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            var s = storage(); return (mode(side) & INPUT) != 0 && s != null && s.isFluidValid(tank, stack);
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            var s = storage();
            int accepted = (mode(side) & INPUT) == 0 || s == null ? 0 : s.fill(stack, action);
            if (accepted > 0 && action.execute()) dirty();
            return accepted;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            var s = storage();
            FluidStack result = (mode(side) & OUTPUT) == 0 || s == null ? FluidStack.EMPTY : s.drain(stack, action);
            if (!result.isEmpty() && action.execute()) dirty();
            return result;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            var s = storage();
            FluidStack result = (mode(side) & OUTPUT) == 0 || s == null ? FluidStack.EMPTY : s.drain(amount, action);
            if (!result.isEmpty() && action.execute()) dirty();
            return result;
        }
    }
}
