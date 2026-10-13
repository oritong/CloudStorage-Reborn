package com.oritong.cloudstorage.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.function.Consumer;

/** Writes custom item state through the immutable data component API. */
public final class StackData {
    private StackData() {}
    public static CompoundTag get(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static boolean has(ItemStack stack) { return stack.has(DataComponents.CUSTOM_DATA); }
    public static void set(ItemStack stack, CompoundTag tag) { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); }
    public static void update(ItemStack stack, Consumer<CompoundTag> edit) { CustomData.update(DataComponents.CUSTOM_DATA, stack, edit); }
    public static ItemStack load(net.minecraft.core.HolderLookup.Provider registries, CompoundTag saved) {
        if (!saved.contains("Count")) return ItemStack.parseOptional(registries, saved);
        // Custom cloud inventories are not upgraded by vanilla's world data fixer.
        CompoundTag legacy = saved.getCompound("tag");
        var fixed = net.minecraft.util.datafix.DataFixers.getDataFixer().update(
            net.minecraft.util.datafix.fixes.References.ITEM_STACK,
            new com.mojang.serialization.Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, saved),
            3465, net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        ItemStack stack = ItemStack.parseOptional(registries, (CompoundTag) fixed.getValue());
        if (stack.getItem() instanceof BalloonDyeable dyeable && legacy.getCompound("display").contains("color")) {
            dyeable.setColor(stack, legacy.getCompound("display").getInt("color"));
        }
        return stack;
    }
}
