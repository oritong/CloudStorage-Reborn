package com.github.alexthe668.cloudstorage.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class CloudChestContainer extends SimpleContainer {

    public CloudChestContainer(int size) {
        super(size);
    }

    public void fromTag(ListTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        for(int i = 0; i < tag.size(); ++i) {
            CompoundTag compoundtag = tag.getCompound(i);
            int j = compoundtag.getInt("Slot");
            if (j >= 0 && j < getContainerSize()) {
                setItem(j, com.github.alexthe668.cloudstorage.item.StackData.load(registries, compoundtag));
            }
        }
    }

    public ListTag createTag(net.minecraft.core.HolderLookup.Provider registries) {
        ListTag listtag = new ListTag();

        for(int i = 0; i < getContainerSize(); ++i) {
            ItemStack itemstack = getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = (CompoundTag) itemstack.save(registries);
                compoundtag.putInt("Slot", i);
                listtag.add(compoundtag);
            }
        }
        return listtag;
    }
}
