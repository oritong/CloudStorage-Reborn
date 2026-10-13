package com.oritong.cloudstorage.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CSBlockItem  extends BlockItem {

    private final DeferredHolder<Block, Block> blockSupplier;

    public CSBlockItem(DeferredHolder<Block, Block> blockSupplier, Item.Properties props) {
        super(blockSupplier.get(), props);
        this.blockSupplier = blockSupplier;
    }

    @Override
    public Block getBlock() {
        return blockSupplier.get();
    }

    public boolean canFitInsideContainerItems() {
        return !(blockSupplier.get() instanceof ShulkerBoxBlock);
    }

    public void onDestroyed(ItemEntity entity) { super.onDestroyed(entity); }
}
