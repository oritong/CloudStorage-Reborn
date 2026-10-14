package com.oritong.cloudstorage.mixin;

import com.oritong.cloudstorage.client.gui.CloudChestScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import yalter.mousetweaks.api.IMTModGuiContainer3Ex;

import java.util.List;

/** Applied only when Mouse Tweaks is installed; its own handler controls drag gestures and settings. */
@Mixin(CloudChestScreen.class)
public abstract class MouseTweaksScreenMixin implements IMTModGuiContainer3Ex {
    @Unique private CloudChestScreen cloudstorage_reborn$screen() { return (CloudChestScreen) (Object) this; }
    @Override public boolean MT_isMouseTweaksDisabled() { return false; }
    @Override public boolean MT_isWheelTweakDisabled() { return true; }
    @Override public List<Slot> MT_getSlots() { return cloudstorage_reborn$screen().getMouseTweaksSlots(); }
    @Override public Slot MT_getSlotUnderMouse(double x, double y) { return cloudstorage_reborn$screen().getMouseTweaksSlot(x, y); }
    @Override public boolean MT_isCraftingOutput(Slot slot) { return false; }
    @Override public boolean MT_isIgnored(Slot slot) { return cloudstorage_reborn$screen().isMouseTweaksSlotIgnored(slot); }
    @Override public boolean MT_disableRMBDraggingFunctionality() { return cloudstorage_reborn$screen().disableMouseTweaksQuickCraft(); }
    @Override public void MT_clickSlot(Slot slot, int button, ClickType type) { cloudstorage_reborn$screen().clickMouseTweaksSlot(slot, button, type); }
}
