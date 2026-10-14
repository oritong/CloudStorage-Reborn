package com.oritong.cloudstorage.client.gui;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.CloudTerminalEntry;
import com.oritong.cloudstorage.inventory.CloudTerminalOrder;
import com.oritong.cloudstorage.inventory.SlotCloudChest;
import com.oritong.cloudstorage.network.MessageCloudTerminalAction;
import com.oritong.cloudstorage.network.MessageCloudSearchContents;
import com.oritong.cloudstorage.network.MessageCloudQuickMove;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CloudChestScreen extends AbstractContainerScreen<CloudChestMenu> {
    public static final ResourceLocation TEXTURE = ResourceLocation.parse("cloudstorage_reborn:textures/gui/cloud_chest_gui.png");
    private EditBox searchBox;
    private int scrollRow;
    private boolean scrolling;
    private boolean consumeRelease;
    private boolean shiftKeyHeld;
    private List<CloudTerminalEntry> lockedOrder;
    private List<CloudTerminalEntry> displayed = List.of();
    private boolean sortByName = true;
    private long displayedRevision = -1;
    private String displayedQuery = "";
    private boolean displayedSort;
    private boolean displayedLocked;
    private final List<Slot> mouseTweaksSlots = new ArrayList<>();
    public int mode;

    public CloudChestScreen(CloudChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 195;
        imageHeight = 222;
        inventoryLabelY = imageHeight - 94;
        var virtual = new SimpleContainer(54);
        for (int i = 0; i < 54; i++) mouseTweaksSlots.add(new MouseTweaksTerminalSlot(virtual, i));
    }

    @Override protected void init() {
        super.init();
        String previous = searchBox == null ? "" : searchBox.getValue();
        searchBox = new EditBox(font, leftPos + 82, topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
        searchBox.setMaxLength(50);
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(previous);
        searchBox.setResponder(query -> { scrollRow = 0; lockedOrder = null; updateDisplay(); });
        addWidget(searchBox);
        addRenderableWidget(new CloudMiniButton(this, leftPos + 174, topPos + 141, 0, button -> {
            searchBox.setValue("");
            scrollRow = 0;
        }));
        addRenderableWidget(new CloudMiniButton(this, leftPos + 174, topPos + 157, 1, button -> {
            sortByName = !sortByName;
            if (!orderLocked()) scrollRow = 0;
        }));
        addRenderableWidget(new CloudMiniButton(this, leftPos + 174, topPos + 173, 2, button -> searchBox.setFocused(true)));
        updateDisplay();
    }

    public void receiveSearchContents(MessageCloudSearchContents message) { }
    private boolean orderLocked() { return shiftKeyHeld || hasShiftDown(); }

    private void updateDisplay() {
        var current = menu.getTerminalEntries();
        boolean locked = orderLocked();
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT);
        if (displayedRevision == menu.getTerminalRevision() && displayedQuery.equals(query)
                && displayedSort == sortByName && displayedLocked == locked) return;
        if (locked) {
            if (lockedOrder == null) lockedOrder = List.copyOf(displayed);
            lockedOrder = CloudTerminalOrder.refreshLocked(lockedOrder, current, entry -> matches(entry, query));
            displayed = lockedOrder;
        } else {
            lockedOrder = null;
            List<CloudTerminalEntry> matches = new ArrayList<>();
            for (var entry : current) if (matches(entry, query)) matches.add(entry);
            if (sortByName) matches.sort(Comparator.comparing(entry -> name(entry).getString().toLowerCase(Locale.ROOT)));
            displayed = List.copyOf(matches);
            scrollRow = Math.min(scrollRow, maxScrollRow());
        }
        displayedRevision = menu.getTerminalRevision();
        displayedQuery = query;
        displayedSort = sortByName;
        displayedLocked = locked;
    }

    private boolean matches(CloudTerminalEntry entry, String query) {
        if (query.isEmpty()) return true;
        if (!entry.isFluid()) return CloudChestMenu.matchesSearch(minecraft.player, query, entry.item());
        if (query.startsWith("@")) return BuiltInRegistries.FLUID.getKey(entry.fluid().getFluid()).getNamespace().contains(query.substring(1));
        if (query.startsWith("#")) return entry.fluid().getFluid().builtInRegistryHolder().tags().anyMatch(tag -> tag.location().toString().contains(query.substring(1)));
        return CloudStorage.PROXY.matchesSearchText(name(entry).getString().toLowerCase(Locale.ROOT), query)
                || CloudStorage.PROXY.matchesSearchText(BuiltInRegistries.FLUID.getKey(entry.fluid().getFluid()).toString(), query);
    }

    private static Component name(CloudTerminalEntry entry) {
        if (!entry.isFluid()) return entry.item().getHoverName();
        return entry.fluid().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_NAME, entry.fluid().getHoverName());
    }

    public List<CloudTerminalEntry> getDisplayedEntries() { return displayed; }
    public int getScrollRow() { return scrollRow; }

    public List<Slot> getMouseTweaksSlots() {
        List<Slot> slots = new ArrayList<>(mouseTweaksSlots);
        for (Slot slot : menu.slots) if (!(slot instanceof SlotCloudChest)) slots.add(slot);
        return slots;
    }

    public Slot getMouseTweaksSlot(double x, double y) {
        updateDisplay();
        int index = gridIndex(x, y);
        if (index >= 0) return mouseTweaksSlots.get(index - scrollRow * 9);
        for (Slot slot : menu.slots) {
            if (!(slot instanceof SlotCloudChest) && slot.isActive() && isHovering(slot.x, slot.y, 16, 16, x, y)) return slot;
        }
        return null;
    }

    public boolean isMouseTweaksSlotIgnored(Slot slot) {
        return slot instanceof MouseTweaksTerminalSlot && !slot.hasItem();
    }

    public boolean disableMouseTweaksQuickCraft() {
        boolean wasQuickCrafting = isQuickCrafting;
        consumeRelease = true;
        isQuickCrafting = false;
        quickCraftSlots.clear();
        return wasQuickCrafting;
    }

    public void clickMouseTweaksSlot(Slot slot, int button, ClickType type) {
        if (slot instanceof MouseTweaksTerminalSlot virtual) {
            var entry = virtual.entry();
            if (button == 0 && type == ClickType.QUICK_MOVE && menu.getCarried().isEmpty()
                    && entry != null && !entry.isFluid() && entry.amount() > 0)
                CloudStorage.sendMSGToServer(new MessageCloudQuickMove(menu.containerId, entry.source(), entry.item().copy()));
        } else if (menu.slots.contains(slot)) slotClicked(slot, slot.index, button, type);
    }

    private final class MouseTweaksTerminalSlot extends Slot {
        private final int visibleIndex;
        private MouseTweaksTerminalSlot(SimpleContainer virtual, int index) {
            super(virtual, index, 8 + index % 9 * 18, 18 + index / 9 * 18);
            visibleIndex = index;
        }
        private CloudTerminalEntry entry() {
            int index = scrollRow * 9 + visibleIndex;
            return index < displayed.size() ? displayed.get(index) : null;
        }
        @Override public ItemStack getItem() {
            var entry = entry();
            return entry == null || entry.isFluid() || entry.amount() == 0 ? ItemStack.EMPTY : entry.item();
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    private int maxScrollRow() { return Math.max(0, (displayed.size() + 8) / 9 - 6); }
    private int gridIndex(double x, double y) {
        int column = (int) Math.floor((x - leftPos - 8) / 18);
        int row = (int) Math.floor((y - topPos - 18) / 18);
        return column >= 0 && column < 9 && row >= 0 && row < 6 ? (scrollRow + row) * 9 + column : -1;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateDisplay();
        super.render(graphics, mouseX, mouseY, partialTick);
        int hovered = gridIndex(mouseX, mouseY);
        var entry = hovered >= 0 && hovered < displayed.size() ? displayed.get(hovered) : null;
        List<Component> containerTooltip = hovered >= 0 ? getContainerTooltip(entry) : List.of();
        if (!containerTooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, containerTooltip, mouseX, mouseY);
        } else if (entry != null && menu.getCarried().isEmpty()) {
            if (entry.isFluid()) {
                graphics.renderComponentTooltip(font, List.of(name(entry), Component.literal(String.format(Locale.ROOT, "%,d mB", entry.amount())),
                        Component.translatable("cloudstorage_reborn.container.fluid_capacity", String.format(Locale.ROOT, "%,d", menu.getTerminalEntries().stream().filter(CloudTerminalEntry::isFluid).mapToLong(CloudTerminalEntry::amount).sum()),
                                String.format(Locale.ROOT, "%,d", menu.getFluidCapacity())),
                        Component.translatable("cloudstorage_reborn.container.fluid_types",
                                menu.getTerminalEntries().stream().filter(CloudTerminalEntry::isFluid).count(), menu.getFluidTypeCapacity())), mouseX, mouseY);
            } else graphics.renderTooltip(font, getTooltipFromContainerItem(entry.item()), entry.item().getTooltipImage(), entry.item(), mouseX, mouseY);
        } else renderTooltip(graphics, mouseX, mouseY);
    }

    /** Describe the two different actions of a carried container, without covering the grid with storage details. */
    public List<Component> getContainerTooltip(CloudTerminalEntry hovered) {
        ItemStack carried = menu.getCarried();
        if (!menu.isFluidStorageEnabled() || carried.isEmpty() || FluidUtil.getFluidHandler(carried.copyWithCount(1)).isEmpty()) return List.of();
        var contained = FluidUtil.getFluidContained(carried.copyWithCount(1));
        Component left = hovered != null && hovered.isFluid()
                ? containerAction("take", "left", name(hovered))
                : containerAction("store", "left", carried.getHoverName());
        if (contained.isPresent()) {
            FluidStack fluid = contained.get();
            Component fluidName = fluid.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_NAME, fluid.getHoverName());
            return List.of(left, containerAction("store", "right", fluidName));
        }
        return hovered != null && hovered.isFluid() ? List.of(left) : List.of();
    }

    private static Component containerAction(String action, String button, Component target) {
        return Component.translatable("cloudstorage_reborn.container.action_" + action,
                Component.translatable("cloudstorage_reborn.container.click_" + button),
                target.copy().withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY);
    }

    @Override protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (!(slot instanceof SlotCloudChest)) super.renderSlot(graphics, slot);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        graphics.setColor(1, 1, 1, 1);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int hovered = gridIndex(mouseX, mouseY);
        for (int visible = 0; visible < 54; visible++) {
            int x = leftPos + 8 + visible % 9 * 18;
            int y = topPos + 18 + visible / 9 * 18;
            graphics.blit(TEXTURE, x - 1, y - 1, 0, 222, 18, 18);
            int index = scrollRow * 9 + visible;
            if (index >= displayed.size()) continue;
            var entry = displayed.get(index);
            if (entry.isFluid()) renderFluid(graphics, entry, x, y);
            else {
                graphics.renderItem(entry.item(), x, y);
                graphics.renderItemDecorations(font, entry.item(), x, y, entry.amount() == 0 ? "0" : null);
            }
            if (entry.amount() == 0) graphics.fill(x, y, x + 16, y + 16, 0x90000000);
            if (hovered == index) graphics.fill(x, y, x + 16, y + 16, 0x60FFFFFF);
        }
        int max = maxScrollRow();
        graphics.blit(TEXTURE, leftPos + 175, topPos + 18 + (max == 0 ? 0 : Math.round(95F * scrollRow / max)), 232 + (max > 0 ? 0 : 12), 0, 12, 15);
        graphics.blit(TEXTURE, leftPos + 79, topPos + 4, 36, 222, 90, 12);
        searchBox.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderFluid(GuiGraphics graphics, CloudTerminalEntry entry, int x, int y) {
        var extension = IClientFluidTypeExtensions.of(entry.fluid().getFluid());
        var texture = extension.getStillTexture(entry.fluid());
        if (texture != null) {
            var sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
            int color = extension.getTintColor(entry.fluid());
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, (color >>> 24) / 255F);
            graphics.blit(x, y, 0, 16, 16, sprite);
            graphics.setColor(1, 1, 1, 1);
        } else graphics.renderItem(new ItemStack(Items.BUCKET), x, y);
        String amount = entry.amount() >= 1000 ? String.format(Locale.ROOT, "%.1fB", entry.amount() / 1000D) : entry.amount() + "mB";
        graphics.pose().pushPose();
        graphics.pose().translate(x + 16, y + 11, 200);
        graphics.pose().scale(0.5F, 0.5F, 1);
        graphics.drawString(font, amount, -font.width(amount), 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 4210752, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }

    @Override public boolean charTyped(char character, int modifiers) { return searchBox.charTyped(character, modifiers); }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 340 || key == 344) {
            updateDisplay();
            shiftKeyHeld = true;
            lockedOrder = List.copyOf(displayed);
            return true;
        }
        if (searchBox.isFocused() && key != 256) {
            searchBox.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public boolean keyReleased(int key, int scanCode, int modifiers) {
        if (key == 340 || key == 344) { shiftKeyHeld = false; lockedOrder = null; updateDisplay(); }
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        updateDisplay();
        int index = gridIndex(x, y);
        if (index >= 0) {
            consumeRelease = true;
            isQuickCrafting = false;
            quickCraftSlots.clear();
            if (button == 0 || button == 1) {
                var entry = index < displayed.size() ? displayed.get(index) : null;
                int serverIndex = entry == null ? -1 : menu.getTerminalEntries().indexOf(entry);
                if (entry == null || entry.amount() > 0 && serverIndex >= 0)
                    CloudStorage.sendMSGToServer(new MessageCloudTerminalAction(menu.containerId, menu.getTerminalRevision(), serverIndex, button, orderLocked()));
            }
            return true;
        }
        if (button == 0 && x >= leftPos + 175 && x < leftPos + 189 && y >= topPos + 18 && y < topPos + 130) {
            scrolling = maxScrollRow() > 0;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        scrolling = false;
        if (consumeRelease) { consumeRelease = false; return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (scrolling) { scrollRow = Math.round(Mth.clamp(((float) y - topPos - 25.5F) / 97F, 0, 1) * maxScrollRow()); return true; }
        if (consumeRelease) return true;
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(vertical), 0, maxScrollRow());
        return true;
    }
    @Override protected void containerTick() { updateDisplay(); }
}
