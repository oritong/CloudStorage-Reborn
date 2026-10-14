package com.oritong.cloudstorage.misc;

import com.oritong.cloudstorage.inventory.CloudChestContainer;
import com.oritong.cloudstorage.inventory.CloudFluidStorage;
import com.oritong.cloudstorage.CloudStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;

import java.util.UUID;

public class CloudIndex {

    private UUID playerUUID;
    private int balloonColor;
    private int containerSize;
    private CloudChestContainer container;
    private long fluidCapacity;
    private int fluidTypes;
    private final CloudFluidStorage fluids = new CloudFluidStorage(() -> fluidCapacity, () -> fluidTypes,
            () -> CloudStorage.CONFIG.fluidStorage.get());

    public CloudIndex(UUID playerUUID, int balloonColor, int size){
        this.playerUUID = playerUUID;
        this.balloonColor = balloonColor;
        this.containerSize = size;
        this.container = new CloudChestContainer(size);
    }

    public CloudIndex(CompoundTag innerTag, net.minecraft.core.HolderLookup.Provider registries) {
        this.balloonColor = innerTag.getInt("BalloonColor");
        this.containerSize = innerTag.getInt("Size");
        this.playerUUID = innerTag.getUUID("PlayerUUID");
        this.container = new CloudChestContainer(this.containerSize);
        this.container.fromTag(innerTag.getList("ContainerTag", 10), registries);
        this.fluidCapacity = Math.max(0, innerTag.getLong("FluidCapacity"));
        this.fluidTypes = Math.max(0, innerTag.getInt("FluidTypes"));
        this.fluids.load(innerTag.getList("Fluids", 10), registries);
    }

    public void writeToNBT(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putInt("BalloonColor", balloonColor);
        tag.putInt("Size", containerSize);
        tag.putUUID("PlayerUUID", playerUUID);
        tag.put("ContainerTag", this.container.createTag(registries));
        tag.put("Fluids", this.fluids.save(registries));
        tag.putLong("FluidCapacity", fluidCapacity);
        tag.putInt("FluidTypes", fluidTypes);
    }

    public void resize(int newSize){
        if(this.containerSize != newSize){
            CloudChestContainer previous = this.container;
            this.containerSize = newSize;
            this.container = new CloudChestContainer(newSize);
            if (previous != null) {
                for (int i = 0; i < Math.min(previous.getContainerSize(), newSize); i++) this.container.setItem(i, previous.getItem(i));
            }
        }
    }

    public int getBalloonColor() {
        return balloonColor;
    }

    public void setBalloonColor(int balloonColor) {
        this.balloonColor = balloonColor;
    }

    public int getContainerSize() {
        return containerSize;
    }

    public SimpleContainer getContainer() {
        return container;
    }

    public CloudFluidStorage getFluids() { return fluids; }

    public boolean addFluidStorage(long capacity) { return addFluidStorage(capacity, FluidStack.EMPTY, 0); }

    public boolean addFluidStorage(long capacity, FluidStack fluid, long amount) {
        if (capacity <= 0 || fluidTypes == Integer.MAX_VALUE || amount < 0 || amount > capacity
                || (amount > 0 && fluid.isEmpty()) || amount > Long.MAX_VALUE - fluids.storedAmount()) return false;
        fluidCapacity += Math.min(capacity, Long.MAX_VALUE - fluidCapacity);
        fluidTypes++;
        if (amount > 0) fluids.restoreFluid(fluid, amount);
        return true;
    }

    public boolean hasStorage() {
        return containerSize > 0 || fluids.isEnabled() && (fluidCapacity > 0 || fluids.storedAmount() > 0);
    }

    public UUID getPlayerUUID(){
        return playerUUID;
    }

    public int calcUsedSlots() {
        int slots = 0;
        for(int i = 0; i < container.getContainerSize(); i++){
            if(!container.getItem(i).isEmpty()){
                slots++;
            }
        }
        return slots;
    }
}
