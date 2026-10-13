package com.github.alexthe668.cloudstorage.misc;

import com.github.alexthe668.cloudstorage.CloudStorage;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CSRecipeRegistry {

    public static final DeferredRegister<RecipeSerializer<?>> DEF_REG = DeferredRegister.create(Registries.RECIPE_SERIALIZER, CloudStorage.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> BALLOON_ARROW_RECIPE = DEF_REG.register("balloon_arrow", () -> new SimpleCraftingRecipeSerializer<>(RecipeBalloonArrow::new));

}
