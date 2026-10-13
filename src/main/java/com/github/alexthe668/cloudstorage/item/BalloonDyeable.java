package com.github.alexthe668.cloudstorage.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public interface BalloonDyeable {
    default int getColor(ItemStack stack) { return BalloonItem.getBalloonColor(stack); }
    default boolean hasCustomColor(ItemStack stack) { return stack.has(DataComponents.DYED_COLOR); }
    default void setColor(ItemStack stack, int color) { stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, false)); }
    default void clearColor(ItemStack stack) { stack.remove(DataComponents.DYED_COLOR); }
}
