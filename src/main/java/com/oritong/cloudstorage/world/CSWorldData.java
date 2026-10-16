package com.oritong.cloudstorage.world;

import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.CloudStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = CloudStorage.MODID)
public class CSWorldData extends SavedData {

    private static final String IDENTIFIER = "cloudstorage_reborn_world_data";
    private List<CloudIndex> privateClouds = new ArrayList<>();
    private List<CloudIndex> publicClouds = new ArrayList<>();
    private List<CloudIndex> lootClouds = new ArrayList<>();

    private CSWorldData() {
        super();
    }

    @SubscribeEvent public static void tickFurnaces(ServerTickEvent.Post event) {
        var level = event.getServer().overworld();
        var data = get(level);
        if (data == null) return;
        for (var cloud : data.privateClouds) if (cloud.getFurnace().count() > 0) cloud.getFurnace().tick(level, cloud.getContainer());
        for (var cloud : data.publicClouds) if (cloud.getFurnace().count() > 0) cloud.getFurnace().tick(level, cloud.getContainer());
    }

    public static CSWorldData get(Level world) {
        if (world instanceof ServerLevel) {
            ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
            DimensionDataStorage storage = overworld.getDataStorage();
            CSWorldData data = storage.computeIfAbsent(new SavedData.Factory<>(CSWorldData::new, CSWorldData::load, null), IDENTIFIER);
            if (data != null) {
                data.setDirty();
            }
            return data;
        }
        return null;
    }

    public static CSWorldData load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        CSWorldData data = new CSWorldData();
        if (nbt.contains("PrivateClouds")) {
            ListTag listtag = nbt.getList("PrivateClouds", 10);
            for (int i = 0; i < listtag.size(); ++i) {
                CompoundTag innerTag = listtag.getCompound(i);
                data.privateClouds.add(new CloudIndex(innerTag, registries));
            }
        }
        if (nbt.contains("PublicClouds")) {
            ListTag listtag = nbt.getList("PublicClouds", 10);
            for (int i = 0; i < listtag.size(); ++i) {
                CompoundTag innerTag = listtag.getCompound(i);
                data.publicClouds.add(new CloudIndex(innerTag, registries));
            }
        }
        if (nbt.contains("LootClouds")) {
            ListTag listtag = nbt.getList("LootClouds", 10);
            for (int i = 0; i < listtag.size(); ++i) {
                CompoundTag innerTag = listtag.getCompound(i);
                data.lootClouds.add(new CloudIndex(innerTag, registries));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries) {
        if (!this.privateClouds.isEmpty()) {
            ListTag listTag = new ListTag();
            for(CloudIndex cloud : privateClouds){
                CompoundTag tag = new CompoundTag();
                cloud.writeToNBT(tag, registries);
                listTag.add(tag);
            }
            compound.put("PrivateClouds", listTag);
        }
        if (!this.publicClouds.isEmpty()) {
            ListTag listTag = new ListTag();
            for(CloudIndex cloud : publicClouds){
                CompoundTag tag = new CompoundTag();
                cloud.writeToNBT(tag, registries);
                listTag.add(tag);
            }
            compound.put("PublicClouds", listTag);
        }
        if (!this.lootClouds.isEmpty()) {
            ListTag listTag = new ListTag();
            for(CloudIndex cloud : lootClouds){
                CompoundTag tag = new CompoundTag();
                cloud.writeToNBT(tag, registries);
                listTag.add(tag);
            }
            compound.put("LootClouds", listTag);
        }
        return compound;
    }

    public CloudIndex getPrivateCloud(UUID player, int color){
        for(CloudIndex cloud : privateClouds){
            if(cloud.getBalloonColor() == color && cloud.getPlayerUUID().equals(player)){
                return cloud;
            }
        }
        return null;
    }

    public void addPrivateCloud(CloudIndex cloud){
        this.privateClouds.add(cloud);
    }

    public CloudIndex getPublicCloud(int color){
        for(CloudIndex cloud : publicClouds){
            if(cloud.getBalloonColor() == color){
                return cloud;
            }
        }
        return null;
    }

    public void addPublicCloud(CloudIndex cloud){
        this.publicClouds.add(cloud);
    }

    public CloudIndex getLootCloud(UUID player, int color){
        for(CloudIndex cloud : lootClouds){
            if(cloud.getBalloonColor() == color && cloud.getPlayerUUID().equals(player)){
                return cloud;
            }
        }
        return null;
    }

    public void addLootCloud(CloudIndex cloud){
        this.lootClouds.add(cloud);
    }

    public Collection<CloudIndex> getAllPrivateCloudsFor(UUID uuid) {
        return privateClouds.stream().filter((cloudIndex -> cloudIndex.getPlayerUUID().equals(uuid))).toList();
    }

    public Collection<CloudIndex> getAllPublicClouds() {
        return publicClouds;
    }
}
