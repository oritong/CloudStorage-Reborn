package com.github.alexthe668.cloudstorage.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

public class BalloonDispenseBehavior extends OptionalDispenseItemBehavior {

    public static final BalloonDispenseBehavior INSTANCE = new BalloonDispenseBehavior();

    private BalloonDispenseBehavior(){}

    protected ItemStack execute(BlockSource blockSource, ItemStack stack) {
        this.setSuccess(false);
        Item item = stack.getItem();
        if (item instanceof BalloonItem balloon) {
            Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
            BlockPos blockpos = blockSource.pos();
            setSuccess(balloon.placeBalloon(blockSource.level(), stack, blockpos, direction, null, true));
        }

        return stack;
    }
}
