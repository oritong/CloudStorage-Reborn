package com.oritong.cloudstorage.entity;

import com.oritong.cloudstorage.CloudStorage;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

@net.neoforged.fml.common.EventBusSubscriber(modid = CloudStorage.MODID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public class CSEntityRegistry {

    public static final DeferredRegister<EntityType<?>> DEF_REG = DeferredRegister.create(Registries.ENTITY_TYPE, CloudStorage.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<BadloonEntity>> BADLOON = DEF_REG.register("badloon", () -> EntityType.Builder.of(BadloonEntity::new, MobCategory.MONSTER).sized(0.7F, 0.9F).build("badloon"));
    public static final DeferredHolder<EntityType<?>, EntityType<BadloonHandEntity>> BADLOON_HAND = DEF_REG.register("badloon_hand", () -> (EntityType)EntityType.Builder.of(BadloonHandEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(1).clientTrackingRange(20).build("badloon_hand"));
    public static final DeferredHolder<EntityType<?>, EntityType<BalloonEntity>> BALLOON = DEF_REG.register("balloon", () -> (EntityType)EntityType.Builder.of(BalloonEntity::new, MobCategory.MISC).sized(0.7F, 0.9F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build("balloon"));
    public static final DeferredHolder<EntityType<?>, EntityType<BalloonTieEntity>> BALLOON_TIE = DEF_REG.register("balloon_tie", () -> (EntityType)EntityType.Builder.of(BalloonTieEntity::new, MobCategory.MISC).sized(0.4F, 0.55F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build("balloon_tie"));
    public static final DeferredHolder<EntityType<?>, EntityType<BalloonCargoEntity>> BALLOON_CARGO = DEF_REG.register("balloon_cargo", () -> (EntityType)EntityType.Builder.of(BalloonCargoEntity::new, MobCategory.MISC).sized(0.99F, 0.99F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build("balloon_cargo"));
    public static final DeferredHolder<EntityType<?>, EntityType<BloviatorEntity>> BLOVIATOR = DEF_REG.register("bloviator", () -> EntityType.Builder.of(BloviatorEntity::new, MobCategory.MONSTER).sized(2F, 1.3F).build("bloviator"));
    public static final DeferredHolder<EntityType<?>, EntityType<BalloonBuddyEntity>> BALLOON_BUDDY = DEF_REG.register("balloon_buddy", () -> EntityType.Builder.of(BalloonBuddyEntity::new, MobCategory.CREATURE).sized(0.7F, 0.9F).build("balloon_buddy"));

    @SubscribeEvent
    public static void initializeAttributes(EntityAttributeCreationEvent event) {
        event.put(BADLOON.get(), BadloonEntity.bakeAttributes().build());
        event.put(BLOVIATOR.get(), BloviatorEntity.bakeAttributes().build());
        event.put(BALLOON_BUDDY.get(), BalloonBuddyEntity.bakeAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(BADLOON.get(), net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BadloonEntity::canBadloonSpawn, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BLOVIATOR.get(), net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BloviatorEntity::canBloviatorSpawn, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
