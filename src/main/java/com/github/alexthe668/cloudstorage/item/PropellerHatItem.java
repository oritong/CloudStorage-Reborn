package com.github.alexthe668.cloudstorage.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import java.util.Map;
import java.util.List;

public class PropellerHatItem extends ArmorItem {
    public static final Holder<ArmorMaterial> HAT_ARMOR_MATERIAL = Holder.direct(new ArmorMaterial(
        Map.of(Type.HELMET, 0, Type.CHESTPLATE, 0, Type.LEGGINGS, 0, Type.BOOTS, 0, Type.BODY, 0), 20,
        SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(CSItemRegistry.BALLOON_BIT.get()),
        List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", "propeller_hat"))), 0, 0));
    public PropellerHatItem() { super(HAT_ARMOR_MATERIAL, Type.HELMET, new Item.Properties().durability(300)); }
}
