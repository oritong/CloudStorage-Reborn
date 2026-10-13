package com.oritong.cloudstorage.mixin;

import com.oritong.cloudstorage.item.BalloonItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorDyeRecipe.class)
public class ArmorDyeRecipeMixin {
    private static boolean hasLootBalloon(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) if (BalloonItem.isLoot(input.getItem(i))) return true;
        return false;
    }
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    private void preventLootDye(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) { if (hasLootBalloon(input)) cir.setReturnValue(false); }
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void preventLootResult(CraftingInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) { if (hasLootBalloon(input)) cir.setReturnValue(ItemStack.EMPTY); }
}
