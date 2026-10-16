package com.oritong.cloudstorage.misc;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Functional uploads are independent of item and fluid capacity. */
public enum CloudFunction {
    CRAFTING_TABLE("crafting_table", Blocks.CRAFTING_TABLE),
    HOPPER("hopper", Blocks.HOPPER),
    FURNACE("furnace", Blocks.FURNACE),
    SMITHING_TABLE("smithing_table", Blocks.SMITHING_TABLE),
    STONECUTTER("stonecutter", Blocks.STONECUTTER);

    private final ResourceLocation id;
    private final Block block;

    CloudFunction(String name, Block block) {
        this.id = ResourceLocation.fromNamespaceAndPath("minecraft", name);
        this.block = block;
    }

    public ResourceLocation id() { return id; }
    public Block block() { return block; }
    public int mask() { return 1 << ordinal(); }

    public static CloudFunction forBlock(BlockState state) {
        for (var function : values()) if (state.is(function.block)) return function;
        return null;
    }
}
