package com.oritong.cloudstorage.item;

import com.oritong.cloudstorage.entity.BalloonEntity;
import com.oritong.cloudstorage.entity.CSEntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import com.oritong.cloudstorage.item.BalloonDyeable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BalloonArrowItem extends ArrowItem implements BalloonDyeable {


    public BalloonArrowItem(Properties props) {
        super(props);
    }

    public BalloonArrowItem() {
        this(new Properties());
    }

    public static int getBalloonColor(ItemStack stack) { return BalloonItem.getBalloonColor(stack); }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity entity, ItemStack weapon) {
        Arrow arrow = new Arrow(level, entity, stack.copyWithCount(1), weapon);
        BalloonEntity balloon = CSEntityRegistry.BALLOON.get().create(level);
        balloon.setPos(entity.getX(), entity.getY(1.0), entity.getZ());
        balloon.setChildId(arrow.getUUID());
        balloon.setBalloonColor(getBalloonColor(stack));
        balloon.setStringLength(0);
        balloon.setArrow(true);
        level.addFreshEntity(balloon);
        return arrow;
    }
}
