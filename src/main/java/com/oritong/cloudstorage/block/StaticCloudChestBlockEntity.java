package com.oritong.cloudstorage.block;

import com.oritong.cloudstorage.entity.BalloonEntity;
import com.oritong.cloudstorage.entity.CSEntityRegistry;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class StaticCloudChestBlockEntity extends AbstractCloudChestBlockEntity {

    private static final Component CONTAINER_TITLE = Component.translatable("cloudstorage_reborn.container.static_cloud_chest");
    private int balloonColor = -1;

    public StaticCloudChestBlockEntity(BlockPos pos, BlockState state) {
        super(CSBlockEntityRegistry.STATIC_CLOUD_CHEST.get(), pos, state);
    }

    @Override
    public boolean hasBalloonFor(Player player) {
        return balloonColor != -1;
    }

    @Override
    public int getBalloonFor(Player player) {
        return balloonColor;
    }

    @Override
    public void setBalloonColorFor(Player player, int color) {
        balloonColor = color;
        this.setChanged();
        if (level != null) level.invalidateCapabilities(worldPosition);
    }


    @Override
    public AbstractContainerMenu getMenu(int i, Inventory playerInventory) {
        CSWorldData data = CSWorldData.get(getLevel());
        Player player = playerInventory.player;
        if (data != null && this.hasBalloonFor(player)) {
            CloudIndex cloudIndex = data.getPublicCloud(this.getBalloonFor(player));
            if (cloudIndex != null && cloudIndex.hasStorage()) {
                return new CloudChestMenu(i, playerInventory, cloudIndex, data, this);
            }
        }
        return null;
    }

    @Override
    public Component getTitle() {
        return CONTAINER_TITLE;
    }

    @Override
    public boolean hasNoInvSpace(Player player) {
        CSWorldData data = CSWorldData.get(getLevel());
        if (data != null && this.hasBalloonFor(player)) {
            CloudIndex cloudIndex = data.getPublicCloud(this.getBalloonFor(player));
            return cloudIndex == null || !cloudIndex.hasStorage();
        }
        return false;
    }


    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        balloonColor = tag.getInt("BalloonColor");
    }

    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BalloonColor", balloonColor);
    }

    public net.neoforged.neoforge.items.IItemHandler getItemHandler(@Nullable Direction side) {
        return automation().itemHandler(side);
    }

    @Override public CloudIndex getAutomationCloud(java.util.UUID owner) {
        CSWorldData data = CSWorldData.get(getLevel());
        if (data != null && this.hasBalloonFor(null)) {
            return data.getPublicCloud(this.getBalloonFor(null));
        }
        return null;
    }

    @Override
    public void releaseBalloons() {
        if(this.balloonColor != -1){
            Vec3 releasePosition = Vec3.atBottomCenterOf(this.getBlockPos()).add(0, getEmergence(1.0F) * 2F, 0);
            BalloonEntity balloon = CSEntityRegistry.BALLOON.get().create(level);
            balloon.setBalloonColor(this.balloonColor);
            balloon.setCharged(true);
            balloon.setStringLength(BalloonEntity.DEFAULT_STRING_LENGTH);
            balloon.setPos(releasePosition);
            level.addFreshEntity(balloon);
        }
    }

    @Override
    public int getContainerSize(Player player) {
        CSWorldData data = CSWorldData.get(getLevel());
        if (data != null && this.hasBalloonFor(player)) {
            CloudIndex cloudIndex = data.getPublicCloud(this.getBalloonFor(player));
            return cloudIndex != null ? cloudIndex.getContainerSize() : 0;
        }
        return 0;
    }
}
