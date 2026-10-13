package com.oritong.cloudstorage.misc;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oritong.cloudstorage.item.BalloonDyeable;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;

public class DyeRandomlyLootFunction extends LootItemConditionalFunction {
    public static final MapCodec<DyeRandomlyLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance).apply(instance, DyeRandomlyLootFunction::new));
    DyeRandomlyLootFunction(List<LootItemCondition> conditions) { super(conditions); }
    @Override public LootItemFunctionType<DyeRandomlyLootFunction> getType() { return CSLootRegistry.DYE_RANDOMLY.get(); }
    @Override public ItemStack run(ItemStack stack, LootContext context) {
        var random = context.getRandom();
        if (stack.getItem() instanceof BalloonDyeable dyeable) dyeable.setColor(stack, random.nextInt(0x1000000));
        if (stack.is(Items.FIREWORK_ROCKET)) {
            var color = Util.getRandom(DyeColor.values(), random);
            var shape = Util.getRandom(FireworkExplosion.Shape.values(), random);
            stack.set(DataComponents.FIREWORKS, new Fireworks(random.nextInt(3), List.of(new FireworkExplosion(shape, IntList.of(color.getFireworkColor()), IntList.of(), false, false))));
        }
        return stack;
    }
}
