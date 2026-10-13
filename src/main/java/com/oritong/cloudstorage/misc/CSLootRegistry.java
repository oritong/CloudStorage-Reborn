package com.oritong.cloudstorage.misc;

import com.oritong.cloudstorage.CloudStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CSLootRegistry {
    public static final DeferredRegister<LootItemFunctionType<?>> DEF_REG = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, CloudStorage.MODID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<DyeRandomlyLootFunction>> DYE_RANDOMLY = DEF_REG.register("dye_randomly", () -> new LootItemFunctionType<>(DyeRandomlyLootFunction.CODEC));

}
