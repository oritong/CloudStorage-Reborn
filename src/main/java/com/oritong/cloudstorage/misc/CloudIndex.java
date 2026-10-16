package com.oritong.cloudstorage.misc;

import com.oritong.cloudstorage.inventory.CloudChestContainer;
import com.oritong.cloudstorage.inventory.CloudFluidStorage;
import com.oritong.cloudstorage.inventory.CloudCraftingContainer;
import com.oritong.cloudstorage.inventory.CloudFurnace;
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
    private int functions;
    private final CloudCraftingContainer crafting = new CloudCraftingContainer();
    private final CloudFurnace furnace = new CloudFurnace();
    private boolean autoClear;
    private boolean autoPull;
    private boolean craftToCloud;
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
        for (var function : CloudFunction.values()) {
            if (innerTag.getCompound("Functions").getBoolean(function.id().toString())) unlock(function);
        }
        crafting.fromTag(innerTag.getList("CraftingGrid", 10), registries);
        if (innerTag.contains("CraftingPattern", 9)) {
            var pattern = new CloudChestContainer(9);
            pattern.fromTag(innerTag.getList("CraftingPattern", 10), registries);
            crafting.setPattern(pattern.getItems());
        }
        autoClear = innerTag.getBoolean("CraftingAutoClear");
        autoPull = innerTag.getBoolean("CraftingAutoPull");
        craftToCloud = innerTag.getBoolean("CraftingToCloud");
        furnace.load(innerTag.getCompound("Furnace"), registries);
        if (furnace.count() > 0) unlock(CloudFunction.FURNACE);
    }

    public void writeToNBT(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putInt("BalloonColor", balloonColor);
        tag.putInt("Size", containerSize);
        tag.putUUID("PlayerUUID", playerUUID);
        tag.put("ContainerTag", this.container.createTag(registries));
        tag.put("Fluids", this.fluids.save(registries));
        tag.putLong("FluidCapacity", fluidCapacity);
        tag.putInt("FluidTypes", fluidTypes);
        CompoundTag unlocked = new CompoundTag();
        for (var function : CloudFunction.values()) if (hasFunction(function)) unlocked.putBoolean(function.id().toString(), true);
        tag.put("Functions", unlocked);
        tag.put("CraftingGrid", crafting.createTag(registries));
        var pattern = new CloudChestContainer(9);
        for (int i = 0; i < 9; i++) pattern.setItem(i, crafting.pattern().get(i).copy());
        tag.put("CraftingPattern", pattern.createTag(registries));
        tag.putBoolean("CraftingAutoClear", autoClear);
        tag.putBoolean("CraftingAutoPull", autoPull);
        tag.putBoolean("CraftingToCloud", craftToCloud);
        tag.put("Furnace", furnace.save(registries));
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
        return functions != 0 || containerSize > 0 || fluids.isEnabled() && (fluidCapacity > 0 || fluids.storedAmount() > 0);
    }

    public void unlock(CloudFunction function) { functions |= function.mask(); }
    public boolean hasFunction(CloudFunction function) { return (functions & function.mask()) != 0; }
    public int functionMask() { return functions; }
    public CloudCraftingContainer getCrafting() { return crafting; }
    public CloudFurnace getFurnace() { return furnace; }
    public boolean uploadFunction(CloudFunction function) {
        if (function == CloudFunction.FURNACE) {
            if (!furnace.upload()) return false;
            unlock(function);
            return true;
        }
        if (hasFunction(function)) return false;
        unlock(function);
        return true;
    }
    public boolean autoClear() { return autoClear; }
    public boolean autoPull() { return autoPull; }
    public boolean craftToCloud() { return craftToCloud; }
    public void setAutoClear(boolean value) { autoClear = value; }
    public void setAutoPull(boolean value) { autoPull = value; }
    public void setCraftToCloud(boolean value) { craftToCloud = value; }

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
