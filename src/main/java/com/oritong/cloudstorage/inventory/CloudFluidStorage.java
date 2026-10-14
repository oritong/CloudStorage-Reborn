package com.oritong.cloudstorage.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.IntSupplier;
import java.util.function.BooleanSupplier;

/** Fluid type quota and pooled capacity come from uploaded tanks. Amounts use longs. */
public final class CloudFluidStorage implements IFluidHandler {
    public static final int MAX_TYPES = 256;
    private final LongSupplier capacity;
    private final IntSupplier types;
    private final BooleanSupplier enabled;
    private final List<Entry> entries = new ArrayList<>();
    private long revision;

    public CloudFluidStorage(LongSupplier capacity) {
        this(capacity, () -> MAX_TYPES, () -> true);
    }

    public CloudFluidStorage(LongSupplier capacity, IntSupplier types, BooleanSupplier enabled) {
        this.capacity = capacity;
        this.types = types;
        this.enabled = enabled;
    }

    public long capacity() { return capacity.getAsLong(); }
    public int typeCapacity() { return Math.max(0, types.getAsInt()); }
    public boolean isEnabled() { return enabled.getAsBoolean(); }
    public long revision() { return revision; }
    public long storedAmount() { return entries.stream().mapToLong(Entry::amount).sum(); }
    public List<Entry> snapshot() { return entries.stream().map(e -> new Entry(e.fluid.copy(), e.amount)).toList(); }

    private int find(FluidStack fluid) {
        for (int i = 0; i < entries.size(); i++) {
            if (FluidStack.isSameFluidSameComponents(entries.get(i).fluid, fluid)) return i;
        }
        return -1;
    }

    @Override public int getTanks() { return isEnabled() ? Math.max(entries.size(), typeCapacity()) : 0; }
    @Override public FluidStack getFluidInTank(int tank) {
        if (!isEnabled() || tank < 0 || tank >= entries.size()) return FluidStack.EMPTY;
        Entry entry = entries.get(tank);
        return entry.fluid.copyWithAmount((int) Math.min(Integer.MAX_VALUE, entry.amount));
    }
    @Override public int getTankCapacity(int tank) { return tank < 0 || tank >= getTanks() ? 0 : (int) Math.min(Integer.MAX_VALUE, Math.max(0, capacity())); }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return isEnabled() && tank >= 0 && tank < getTanks() && !stack.isEmpty(); }

    @Override public int fill(FluidStack resource, FluidAction action) {
        if (!isEnabled() || resource.isEmpty()) return 0;
        int index = find(resource);
        if (index < 0 && entries.size() >= typeCapacity()) return 0;
        int accepted = (int) Math.min(resource.getAmount(), Math.max(0, capacity() - storedAmount()));
        if (accepted > 0 && action.execute()) {
            if (index < 0) entries.add(new Entry(resource.copyWithAmount(1), accepted));
            else {
                Entry entry = entries.get(index);
                entries.set(index, new Entry(entry.fluid, entry.amount + accepted));
            }
            revision++;
        }
        return accepted;
    }

    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        if (!isEnabled() || resource.isEmpty()) return FluidStack.EMPTY;
        int index = find(resource);
        if (index < 0) return FluidStack.EMPTY;
        Entry entry = entries.get(index);
        int amount = (int) Math.min(resource.getAmount(), entry.amount);
        FluidStack result = entry.fluid.copyWithAmount(amount);
        if (action.execute()) {
            if (amount == entry.amount) entries.remove(index);
            else entries.set(index, new Entry(entry.fluid, entry.amount - amount));
            revision++;
        }
        return result;
    }

    @Override public FluidStack drain(int amount, FluidAction action) {
        return amount <= 0 || entries.isEmpty() ? FluidStack.EMPTY : drain(entries.getFirst().fluid.copyWithAmount(amount), action);
    }

    public ListTag save(HolderLookup.Provider registries) {
        ListTag result = new ListTag();
        for (Entry entry : entries) {
            CompoundTag tag = new CompoundTag();
            tag.put("Fluid", entry.fluid.save(registries));
            tag.putLong("Amount", entry.amount);
            result.add(tag);
        }
        return result;
    }

    /** Trusted saved or uploaded contents must survive even when a legacy cloud is over quota. */
    public void restoreFluid(FluidStack fluid, long amount) {
        if (fluid.isEmpty() || amount <= 0 || amount > Long.MAX_VALUE - storedAmount()) return;
        int index = find(fluid);
        if (index < 0) entries.add(new Entry(fluid.copyWithAmount(1), amount));
        else {
            Entry entry = entries.get(index);
            entries.set(index, new Entry(entry.fluid, entry.amount + amount));
        }
        revision++;
    }

    public void load(ListTag tags, HolderLookup.Provider registries) {
        entries.clear();
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = tags.getCompound(i);
            FluidStack fluid = FluidStack.parseOptional(registries, tag.getCompound("Fluid"));
            long amount = tag.getLong("Amount");
            restoreFluid(fluid, amount);
        }
        revision++;
    }

    public record Entry(FluidStack fluid, long amount) { }
}
