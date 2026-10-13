package com.oritong.cloudstorage.block;

import com.oritong.cloudstorage.CloudStorage;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CSPOIRegistry {

    public static final DeferredRegister<PoiType> DEF_REG = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, CloudStorage.MODID);
    public static final DeferredHolder<PoiType, PoiType> BALLOON_STAND = DEF_REG.register("balloon_stand", () -> new PoiType(ImmutableSet.copyOf(CSBlockRegistry.BALLOON_STAND.get().getStateDefinition().getPossibleStates()), 1, 1));
}
