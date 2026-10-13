package com.github.alexthe668.cloudstorage.block;

import com.github.alexthe668.cloudstorage.CloudStorage;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CSBlockEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> DEF_REG = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CloudStorage.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CloudChestBlockEntity>> CLOUD_CHEST = DEF_REG.register("cloud_chest", () -> BlockEntityType.Builder.of(CloudChestBlockEntity::new, CSBlockRegistry.CLOUD_CHEST.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StaticCloudChestBlockEntity>> STATIC_CLOUD_CHEST = DEF_REG.register("static_cloud_chest", () -> BlockEntityType.Builder.of(StaticCloudChestBlockEntity::new, CSBlockRegistry.STATIC_CLOUD_CHEST.get()).build(null));

}
