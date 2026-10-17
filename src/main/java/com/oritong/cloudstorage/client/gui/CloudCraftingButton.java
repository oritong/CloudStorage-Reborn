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
            drawIcon(graphics, x, y, kind,
                    priority ? 0xFFFFE9FF : enabled ? 0xFFF2FFF2 : 0xFFFFF4E0,
                    0xFF17151A);
        } else {
            int edge = isHoveredOrFocused() ? 0xFFEEF5F7 : 0xFFC6C6C6;
            graphics.fill(x, y, x + w, y + h, 0xFF373737);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, edge);
            graphics.fill(x + 2, y + 2, x + w - 1, y + h - 1, 0xFF777777);
            graphics.renderItem(icon, x + (w - 16) / 2, y + (h - 16) / 2);
        }
    }

    private static void drawIcon(GuiGraphics graphics, int x, int y, int kind, int color, int outline) {
        String[] glyph;
        if (kind == 0) { // Arrow into a storage tray: automatic clear.
            glyph = new String[]{
                    "...#...",
                    "...#...",
                    "..###..",
                    ".#####.",
                    "#.....#",
                    "#.....#",
                    "#######"
            };
        } else if (kind == 5) { // Fuel flame.
            glyph = new String[]{
                    "...#...",
                    "..##...",
                    ".###...",
                    ".#####.",
                    "##.###.",
                    ".#####.",
                    "..###.."
            };
        } else if (kind == 3) { // Funnel for item collection.
            glyph = new String[]{
                    "#######",
                    ".#####.",
                    "..###..",
                    "...#...",
                    "...#...",
                    "...#...",
                    "...#..."
            };
        } else {
            boolean up = kind == 1 || kind == 4;
            glyph = up ? new String[]{
                    "...#...",
                    "..###..",
                    ".#####.",
                    "..##...",
                    "..##...",
                    "..##...",
                    "..##..."
            } : new String[]{
                    "..##...",
                    "..##...",
                    "..##...",
                    "..##...",
                    ".#####.",
                    "..###..",
                    "...#..."
            };
        }
        drawGlyph(graphics, x + 3, y + 3, glyph, color, outline);
    }

    private static void drawGlyph(GuiGraphics graphics, int x, int y, String[] glyph, int color, int outline) {
        // Draw one continuous dark silhouette first so every pale pixel remains readable
        // against red, green, and purple button faces.
        for (int row = 0; row < glyph.length; row++) {
            for (int column = 0; column < glyph[row].length(); column++) {
                if (glyph[row].charAt(column) != '#') continue;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++)
                    graphics.fill(x + column + dx, y + row + dy, x + column + dx + 1, y + row + dy + 1, outline);
            }
        }
        for (int row = 0; row < glyph.length; row++) {
            for (int column = 0; column < glyph[row].length(); column++) {
                if (glyph[row].charAt(column) == '#')
                    graphics.fill(x + column, y + row, x + column + 1, y + row + 1, color);
            }
        }
    }
}
