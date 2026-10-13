package com.oritong.cloudstorage.client.gui;

import com.github.alexthe666.citadel.client.gui.GuiBasicBook;
import com.github.alexthe666.citadel.client.gui.data.LineData;
import com.oritong.cloudstorage.CloudStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class GuideBookScreen extends GuiBasicBook {

    private static final ResourceLocation ROOT = ResourceLocation.parse("cloudstorage_reborn:book/guide_book/root.json");

    public GuideBookScreen(ItemStack bookStack) {
        super(bookStack, Component.translatable("item.cloudstorage_reborn.guide_book"));
    }

    protected int getBindingColor() {
        return 0XDE2727;
    }

    public ResourceLocation getRootPage() {
        return ROOT;
    }

    public ResourceLocation getTextFileDirectory() {
        return ResourceLocation.parse("cloudstorage_reborn:book/guide_book/");
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        // Citadel draws the book immediately; finish the queued background first.
        graphics.flush();
    }

    @Override
    protected void readInPageText(ResourceLocation resource) {
        if (!resource.getPath().contains("/zh_cn/")) {
            super.readInPageText(resource);
            return;
        }
        try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(resource)) {
            var paragraphs = reader.lines().toList();
            linesFromJSON = paragraphs.size();
            lines.clear();
            int position = 0;
            String text = String.join(" ", paragraphs).replace("<NEWLINE>", "\n\n");
            for (String paragraph : text.split("\n", -1)) {
                if (paragraph.isBlank()) {
                    position++;
                    continue;
                }
                // Font widths and CJK line breaks are more accurate than word lengths.
                for (var line : font.getSplitter().splitLines(paragraph.strip(), 180, Style.EMPTY)) {
                    while (overlapsWidget(position)) position++;
                    int page = position / 28;
                    int column = (position / 14) % 2;
                    lines.add(new LineData(column * 200, position % 14, line.getString(), page));
                    position++;
                }
            }
            linesFromPrinting = lines.size();
            maxPagesFromPrinting = lines.isEmpty() ? 0 : lines.getLast().page();
        } catch (java.io.IOException exception) {
            CloudStorage.LOGGER.error("Could not read guide page {}", resource, exception);
        }
    }

    private boolean overlapsWidget(int position) {
        int page = position / 28;
        int x = 10 + ((position / 14) % 2) * 200;
        int y = 10 + (position % 14) * 12;
        for (var widget : yIndexesToSkip) {
            if (widget.page() == page && x < widget.x() + widget.width() && x + 180 > widget.x()
                && y < widget.y() + widget.height() && y + font.lineHeight > widget.y()) return true;
        }
        return false;
    }
}
