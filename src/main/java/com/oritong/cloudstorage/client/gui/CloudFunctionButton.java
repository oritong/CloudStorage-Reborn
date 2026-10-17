package com.oritong.cloudstorage.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A compact pixel-art tab for uploaded functional blocks. */
public final class CloudFunctionButton extends Button {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("cloudstorage_reborn:textures/gui/cloud_function_button.png");
    private final ItemStack icon;
    private final boolean selected;

    public CloudFunctionButton(int x, int y, Component tooltip, ItemStack icon, boolean selected, OnPress onPress) {
        super(x, y, 24, 24, tooltip, onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.selected = selected;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int frame = selected ? (isHoveredOrFocused() ? 3 : 1) : (isHoveredOrFocused() ? 2 : 0);
        graphics.blit(TEXTURE, getX(), getY(), frame * 24, 0, 24, 24, 96, 24);
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, getX() + 4, getY() + 4);
            graphics.renderItemDecorations(Minecraft.getInstance().font, icon, getX() + 4, getY() + 4);
        }
    }
}
