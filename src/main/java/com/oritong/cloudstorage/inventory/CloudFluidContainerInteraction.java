package com.oritong.cloudstorage.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** AE2-style carried-container interaction using NeoForge's standard fluid capability. */
public final class CloudFluidContainerInteraction {
    private CloudFluidContainerInteraction() { }

    public static boolean interact(Player player, AbstractContainerMenu menu, CloudFluidStorage storage,
                                   FluidStack selected, boolean deposit) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return false;
        // FluidUtil simulates both sides and operates on a single copy. In particular,
        // an indivisible bucket cannot lose fluid when less than a bucket fits.
        var transfer = deposit ? FluidUtil.tryEmptyContainer(carried, storage, Integer.MAX_VALUE, player, true)
                : FluidUtil.tryFillContainer(carried, new SelectedFluid(storage, selected), Integer.MAX_VALUE, player, true);
        if (!transfer.isSuccess()) return false;
        ItemStack result = transfer.getResult();
        ItemStack remaining = carried.copy();
        remaining.shrink(1);
        if (remaining.isEmpty()) menu.setCarried(result);
        else {
            menu.setCarried(remaining);
            player.getInventory().placeItemBackInInventory(result);
        }
        player.getInventory().setChanged();
        menu.broadcastChanges();
        return true;
    }

    /** Expose only the clicked fluid, including its components, to a container's generic drain call. */
    private record SelectedFluid(CloudFluidStorage storage, FluidStack selected) implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return drain(Integer.MAX_VALUE, FluidAction.SIMULATE); }
        @Override public int getTankCapacity(int tank) { return storage.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, FluidStack fluid) { return false; }
        @Override public int fill(FluidStack fluid, FluidAction action) { return 0; }
        @Override public FluidStack drain(int amount, FluidAction action) {
            return amount <= 0 || selected.isEmpty() ? FluidStack.EMPTY : storage.drain(selected.copyWithAmount(amount), action);
        }
        @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
            return FluidStack.isSameFluidSameComponents(selected, fluid) ? storage.drain(fluid, action) : FluidStack.EMPTY;
        }
    }
}
