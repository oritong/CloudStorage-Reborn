package com.oritong.cloudstorage.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Persistent inputs for the uploaded smithing table and stonecutter. */
public final class CloudStations {
    private final CloudChestContainer smithing = new CloudChestContainer(3);
    private final CloudChestContainer stonecutter = new CloudChestContainer(1);
    private int stoneRecipe;

    public CloudChestContainer smithing() { return smithing; }
    public CloudChestContainer stonecutter() { return stonecutter; }
    public int stoneRecipe() { return stoneRecipe; }
    public void setStoneRecipe(int value) { stoneRecipe = Math.max(0, value); }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Smithing", smithing.createTag(registries));
        tag.put("Stonecutter", stonecutter.createTag(registries));
        tag.putInt("StoneRecipe", stoneRecipe);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        smithing.fromTag(tag.getList("Smithing", 10), registries);
        stonecutter.fromTag(tag.getList("Stonecutter", 10), registries);
        stoneRecipe = Math.max(0, tag.getInt("StoneRecipe"));
    }

    public void clearSmithing() {
        for (int i = 0; i < smithing.getContainerSize(); i++) smithing.setItem(i, ItemStack.EMPTY);
    }
}
