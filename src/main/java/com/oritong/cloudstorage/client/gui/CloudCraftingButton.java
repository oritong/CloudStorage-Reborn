package com.oritong.cloudstorage.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Small pixel-art controls used by the uploaded functional panels. */
public final class CloudCraftingButton extends Button {
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.parse("cloudstorage_reborn:textures/gui/cloud_panel_button.png");
    private final ItemStack icon;
    private final int kind;
    private final int state;

    public CloudCraftingButton(int x, int y, int size, Component tooltip, ItemStack icon, int kind,
                               boolean selected, OnPress onPress) {
        this(x, y, size, tooltip, icon, kind, selected ? 1 : 0, onPress);
    }

    public CloudCraftingButton(int x, int y, int size, Component tooltip, ItemStack icon, int kind,
                               int state, OnPress onPress) {
        super(x, y, size, size, tooltip, onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.kind = kind;
        this.state = state;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean toggle = icon.isEmpty();
        boolean enabled = state != 0;
        boolean priority = state == 2;
        if (toggle) {
            int frame = priority ? 4 : enabled ? 2 : 0;
            if (isHoveredOrFocused()) frame++;
            graphics.blit(PANEL_TEXTURE, x, y, frame * 14, 0, 14, 14, 84, 14);
            drawIcon(graphics, x, y, kind, priority ? 0xFFF6E6FF : enabled ? 0xFFE8FFE9 : 0xFFFFE8E8,
                    priority ? 0xFF6B347B : enabled ? 0xFF276A31 : 0xFF6C2528);
        } else {
            int edge = isHoveredOrFocused() ? 0xFFEEF5F7 : 0xFFC6C6C6;
            graphics.fill(x, y, x + w, y + h, 0xFF373737);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, edge);
            graphics.fill(x + 2, y + 2, x + w - 1, y + h - 1, 0xFF777777);
            graphics.renderItem(icon, x + (w - 16) / 2, y + (h - 16) / 2);
        }
    }

    private static void drawIcon(GuiGraphics graphics, int x, int y, int kind, int color, int shadow) {
        if (kind == 0) { // Return the grid to storage.
            bar(graphics, x + 4, y + 3, x + 10, y + 4, color, shadow);
            bar(graphics, x + 5, y + 5, x + 9, y + 8, color, shadow);
            bar(graphics, x + 3, y + 8, x + 7, y + 10, color, shadow);
            bar(graphics, x + 4, y + 10, x + 6, y + 11, color, shadow);
        } else if (kind == 5) { // Fuel flame.
            bar(graphics, x + 7, y + 2, x + 8, y + 5, color, shadow);
            bar(graphics, x + 5, y + 5, x + 10, y + 8, color, shadow);
            bar(graphics, x + 4, y + 8, x + 11, y + 10, color, shadow);
            bar(graphics, x + 6, y + 10, x + 9, y + 12, color, shadow);
        } else if (kind == 3 || kind == 6) { // Funnel for item collection/intake.
            bar(graphics, x + 3, y + 3, x + 11, y + 4, color, shadow);
            bar(graphics, x + 4, y + 5, x + 10, y + 6, color, shadow);
            bar(graphics, x + 5, y + 7, x + 9, y + 8, color, shadow);
            bar(graphics, x + 7, y + 9, x + 8, y + 12, color, shadow);
        } else { // Directional transfer arrow.
            boolean up = kind == 1;
            int shaftTop = up ? 4 : 3;
            int shaftBottom = up ? 10 : 9;
            bar(graphics, x + 6, y + shaftTop, x + 7, y + shaftBottom, color, shadow);
            if (up) {
                bar(graphics, x + 4, y + 4, x + 9, y + 5, color, shadow);
                bar(graphics, x + 3, y + 5, x + 4, y + 7, color, shadow);
                bar(graphics, x + 9, y + 5, x + 10, y + 7, color, shadow);
            } else {
                bar(graphics, x + 4, y + 9, x + 9, y + 10, color, shadow);
                bar(graphics, x + 3, y + 7, x + 4, y + 9, color, shadow);
                bar(graphics, x + 9, y + 7, x + 10, y + 9, color, shadow);
            }
        }
    }

    private static void bar(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, int shadow) {
        graphics.fill(x1 + 1, y1 + 1, x2 + 2, y2 + 2, shadow);
        graphics.fill(x1, y1, x2 + 1, y2 + 1, color);
    }
}
