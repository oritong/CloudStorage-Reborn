package com.oritong.cloudstorage.client.gui;

import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.CloudIOSide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;
import java.util.function.IntConsumer;

/** Original rendering with Mekanism's familiar six-face arrangement and color meanings. */
public final class CloudIOSideButton extends Button {
    private final CloudChestMenu menu;
    private final CloudIOSide side;
    private final IntConsumer action;
    private Component previousTooltip = CommonComponents.EMPTY;
    private BlockPos cachedPos;
    private BlockState cachedState;
    private ItemStack cachedNeighbor = ItemStack.EMPTY;

    public CloudIOSideButton(int x, int y, CloudChestMenu menu, CloudIOSide side, IntConsumer action) {
        super(x, y, side == null ? 14 : 22, side == null ? 14 : 22, CommonComponents.EMPTY, button -> { }, DEFAULT_NARRATION);
        this.menu = menu;
        this.side = side;
        this.action = action;
    }

    private ItemStack neighbor() {
        var client = Minecraft.getInstance();
        if (side == null || menu.getChestPos() == null || client.level == null) return ItemStack.EMPTY;
        var direction = side.direction(menu.getChestFront());
        var pos = menu.getChestPos().relative(direction);
        var state = client.level.getBlockState(pos);
        if (!pos.equals(cachedPos) || state != cachedState) {
            cachedPos = pos;
            cachedState = state;
            cachedNeighbor = state.isAir() ? ItemStack.EMPTY
                    : state.getCloneItemStack(new BlockHitResult(pos.getCenter(), direction.getOpposite(), pos, false), client.level, pos, client.player);
        }
        return cachedNeighbor;
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        int mode = side == null ? 0 : menu.getIOMode(side);
        int face = switch (mode) { case 1 -> 0xFF8A2032; case 2 -> 0xFF294B7F; case 3 -> 0xFF654084; default -> 0xFF686868; };
        int light = switch (mode) { case 1 -> 0xFFDF7387; case 2 -> 0xFF849BD0; case 3 -> 0xFFAB82CF; default -> 0xFFAAAAAA; };
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        graphics.fill(x, y, x + w, y + h, 0xFF171717);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, light);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, face);
        graphics.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0xFF303030);
        graphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF303030);
        if (isHoveredOrFocused() && side != CloudIOSide.TOP) graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0x20FFFFFF);
        ItemStack neighbor = neighbor();
        if (side != null) {
            if (!neighbor.isEmpty()) graphics.renderItem(neighbor, x + 3, y + 3);
        } else {
            // Four inward corners identify the batch/clear button.
            for (int dx : new int[]{ 0, 6 }) for (int dy : new int[]{ 0, 6 }) {
                graphics.fill(x + 3 + dx, y + 3 + dy, x + 5 + dx, y + 5 + dy, 0xFF252525);
                graphics.fill(x + (dx == 0 ? 5 : 8), y + (dy == 0 ? 5 : 8),
                        x + (dx == 0 ? 6 : 9), y + (dy == 0 ? 6 : 9), 0xFF252525);
            }
        }
        Component tooltip;
        if (side == null) tooltip = Component.translatable("cloudstorage_reborn.io.batch");
        else {
            tooltip = Component.translatable("cloudstorage_reborn.io.side." + side.name().toLowerCase(Locale.ROOT)).append("\n")
                    .append(Component.translatable(side == CloudIOSide.TOP ? "cloudstorage_reborn.io.top_locked" : "cloudstorage_reborn.io.mode." + mode));
            if (!neighbor.isEmpty()) tooltip = tooltip.copy().append("\n").append(neighbor.getHoverName());
        }
        if (!tooltip.equals(previousTooltip)) {
            previousTooltip = tooltip;
            setMessage(tooltip);
            setTooltip(Tooltip.create(tooltip));
        }
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if ((button != 0 && button != 1) || !visible || !active || !clicked(x, y)) return false;
        if (side == CloudIOSide.TOP) return true;
        playDownSound(Minecraft.getInstance().getSoundManager());
        int id;
        if (side == null) id = Screen.hasShiftDown() ? 322 : button == 0 ? 320 : 321;
        else id = (Screen.hasShiftDown() && button == 0 ? 330 : button == 0 ? 300 : 310) + side.ordinal();
        action.accept(id);
        return true;
    }

    @Override public void onPress() {
        if (side != CloudIOSide.TOP) action.accept(side == null ? 320 : 300 + side.ordinal());
    }
}
