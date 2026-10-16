package com.oritong.cloudstorage.inventory;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The real ingredients belong to the cloud, including while no terminal is open. */
public class CloudCraftingContainer extends CloudChestContainer implements CraftingContainer {
    private List<ItemStack> pattern = emptyPattern();
    private int batchDepth;
    private boolean batchChanged;
    private long revision;

    public CloudCraftingContainer() { super(9); }

    @Override public int getWidth() { return 3; }
    @Override public int getHeight() { return 3; }
    public long revision() { return revision; }
    public List<ItemStack> pattern() { return pattern; }
    public void setPattern(List<ItemStack> items) { pattern = items.stream().map(stack -> stack.copyWithCount(1)).toList(); }
    public void rememberPattern() { pattern = getItems().stream().map(stack -> stack.copyWithCount(1)).toList(); }
    public void beginBatch() { batchDepth++; }
    public void endBatch() {
        if (--batchDepth == 0 && batchChanged) {
            batchChanged = false;
            revision++;
            super.setChanged();
        }
    }

    @Override public void setChanged() {
        if (batchDepth > 0) { batchChanged = true; return; }
        rememberPattern();
        revision++;
        super.setChanged();
    }

    private static List<ItemStack> emptyPattern() {
        return java.util.Collections.nCopies(9, ItemStack.EMPTY);
    }
}
