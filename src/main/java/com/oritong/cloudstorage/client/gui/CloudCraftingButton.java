package com.oritong.cloudstorage.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Block items use Minecraft's normal isometric item renderer. */
public final class CloudCraftingButton extends Button {
    private final ItemStack icon;
    private final int kind;
    private final boolean selected;

    public CloudCraftingButton(int x, int y, int size, Component tooltip, ItemStack icon, int kind,
                               boolean selected, OnPress onPress) {
        super(x, y, size, size, tooltip, onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.kind = kind;
        this.selected = selected;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean toggle = icon.isEmpty();
        int highlight = toggle ? (selected ? 0xFF8FE6A5 : 0xFFF28C8C) : (selected ? 0xFF74A6CA : 0xFFFFFFFF);
        int background = toggle
                ? (selected ? (isHoveredOrFocused() ? 0xFF42A65A : 0xFF31934D)
                            : (isHoveredOrFocused() ? 0xFFD55353 : 0xFFBC4040))
                : (isHoveredOrFocused() ? 0xFFDCE7ED : 0xFFC6C6C6);
        int shadow = toggle ? (selected ? 0xFF1D5630 : 0xFF6C2323) : 0xFF777777;
        graphics.fill(x, y, x + w, y + h, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, highlight);
        graphics.fill(x + 2, y + 2, x + w - 1, y + h - 1, background);
        graphics.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, shadow);
        graphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, shadow);
        if (!icon.isEmpty()) graphics.renderItem(icon, x + (w - 16) / 2, y + (h - 16) / 2);
        else {
            int color = 0xFFFFFFFF;
            if (kind == 0) { // Clear: cross.
                for (int i = 0; i < 7; i++) {
                    graphics.fill(x + 4 + i, y + 4 + i, x + 5 + i, y + 5 + i, color);
                    graphics.fill(x + 10 - i, y + 4 + i, x + 11 - i, y + 5 + i, color);
                }
            } else { // Pull / store: arrows into / out of a small cloud-storage bar.
                int direction = kind == 1 ? -1 : 1;
                graphics.fill(x + 3, y + (kind == 1 ? 3 : 11), x + 12, y + (kind == 1 ? 5 : 13), color);
                graphics.fill(x + 7, y + 5, x + 9, y + 11, color);
                int tipY = kind == 1 ? y + 10 : y + 5;
                for (int i = 0; i < 3; i++) graphics.fill(x + 5 + i, tipY + direction * i,
                        x + 11 - i, tipY + direction * i + 1, color);
            }
        }
        if (!toggle && selected) graphics.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0xFF398652);
    }
}
