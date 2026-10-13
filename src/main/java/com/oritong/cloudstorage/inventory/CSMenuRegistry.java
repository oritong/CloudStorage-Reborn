package com.oritong.cloudstorage.inventory;

import com.oritong.cloudstorage.CloudStorage;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CSMenuRegistry {

    public static final DeferredRegister<MenuType<?>> DEF_REG = DeferredRegister.create(Registries.MENU, CloudStorage.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CloudChestMenu>> CLOUD_CHEST_MENU = DEF_REG.register("cloud_chest_menu", () -> new MenuType<CloudChestMenu>(CloudChestMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<BalloonStandMenu>> BALLOON_STAND_MENU = DEF_REG.register("balloon_stand_menu", () -> new MenuType<BalloonStandMenu>(BalloonStandMenu::new, FeatureFlags.DEFAULT_FLAGS));

}
