package com.oritong.cloudstorage.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** A displayed entry refers to a real cloud slot or fluid key, never to a temporary inventory. */
public record CloudTerminalEntry(int source, ItemStack item, FluidStack fluid, long amount) {
    public static CloudTerminalEntry item(int slot, ItemStack item) {
        return new CloudTerminalEntry(slot, item.copy(), FluidStack.EMPTY, item.getCount());
    }
    public static CloudTerminalEntry fluid(int tank, CloudFluidStorage.Entry fluid) {
        return new CloudTerminalEntry(tank, ItemStack.EMPTY, fluid.fluid().copy(), fluid.amount());
    }
    public boolean isFluid() { return !fluid.isEmpty(); }
}
