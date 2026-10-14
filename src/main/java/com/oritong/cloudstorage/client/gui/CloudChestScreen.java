package com.oritong.cloudstorage.client.gui;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.SlotCloudChest;
import com.oritong.cloudstorage.network.MessageSearchCloudChest;
import com.oritong.cloudstorage.network.MessageRequestCloudSearch;
import com.oritong.cloudstorage.network.MessageCloudSearchContents;
import com.oritong.cloudstorage.network.MessageCloudSearchMatches;
import com.oritong.cloudstorage.client.compat.JustEnoughCharactersCompat;
import com.oritong.cloudstorage.network.MessageSortCloudChest;
import com.oritong.cloudstorage.network.MessageCloudFluidAction;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.Locale;
import java.util.Objects;
import java.util.BitSet;

public class CloudChestScreen extends AbstractContainerScreen<CloudChestMenu> {

    public static final ResourceLocation TEXTURE = ResourceLocation.parse("cloudstorage_reborn:textures/gui/cloud_chest_gui.png");
    private final int slots;
    private float prevScrollOffs;
    private float scrollOffs;
    private boolean scrolling;
    private CloudMiniButton balloonButton;
    private CloudMiniButton sortButton;
    private CloudMiniButton searchButton;
    private EditBox searchBox;
    private int fluidScrollRow;
    public int mode = 0;
    private boolean ignoreTextInput;
    private int searchRequestId;
    private String requestedSearch = "";
    private int receivedSearchItems;
    private final BitSet searchMatches = new BitSet();

    public CloudChestScreen(CloudChestMenu menu, Inventory playerInv, Component component) {
        super(menu, playerInv, component);
        this.imageWidth = 195;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
        this.slots = CloudStorage.PROXY.getVisibleCloudSlots();
    }

    @Override
    protected void init() {
        super.init();
        resetButtons();
    }

    public void resetButtons() {
        String previousSearch = mode == 2 && searchBox != null ? searchBox.getValue() : "";
        this.clearWidgets();
        int i = this.leftPos;
        int j = this.topPos;
        this.addRenderableWidget(balloonButton = new CloudMiniButton(this, i + 174, j + 141, 0, (button) -> {
            searchFor("");
            mode = 0;
            resetButtons();
        }));
        this.addRenderableWidget(sortButton = new CloudMiniButton(this, i + 174, j + 141 + 16, 1, (button) -> {
            searchFor("");
            mode = 1;
            resetButtons();
        }));
        this.addRenderableWidget(searchButton = new CloudMiniButton(this, i + 174, j + 141 + 32, 2, (button) -> {
            mode = 2;
            resetButtons();
        }));
        Button fluidButton = Button.builder(Component.translatable(menu.isFluidView()
                ? "cloudstorage_reborn.container.item_tab" : "cloudstorage_reborn.container.fluid_tab"), button -> {
            searchFor("");
            mode = 0;
            menu.setFluidView(!menu.isFluidView());
            fluidScrollRow = 0;
            scrollOffs = prevScrollOffs = 0;
            CloudStorage.sendMSGToServer(new MessageCloudFluidAction(menu.containerId,
                    menu.isFluidView() ? MessageCloudFluidAction.FLUIDS : MessageCloudFluidAction.ITEMS, 0, 0));
            resetButtons();
        }).bounds(i + 174, j + 189, 14, 14).build();
        fluidButton.setTooltip(Tooltip.create(Component.translatable(menu.isFluidView()
                ? "cloudstorage_reborn.container.items" : "cloudstorage_reborn.container.fluids")));
        this.addRenderableWidget(fluidButton);
        this.searchBox = new EditBox(this.font, this.leftPos + 82, this.topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setValue(previousSearch);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(mode == 2);
        this.searchBox.setFocused(mode == 2);
        this.searchBox.setTextColor(16777215);
        this.addWidget(this.searchBox);
        balloonButton.active = !menu.isFluidView() && mode != 0;
        sortButton.active = !menu.isFluidView() && mode != 1;
        searchButton.active = !menu.isFluidView() && mode != 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
        this.renderBackground(guiGraphics, x, y, partialTicks);
        this.renderBg(guiGraphics, partialTicks, x, y);
        super.render(guiGraphics, x, y, partialTicks);
        if (menu.isFluidView()) renderFluids(guiGraphics, x, y);
        else this.renderGraySlots(guiGraphics, x, y);
        if (!menu.isFluidView() || fluidGridIndex(x, y) < 0) this.renderTooltip(guiGraphics, x, y);
    }

    private int fluidGridIndex(double x, double y) {
        int column = (int) Math.floor((x - leftPos - 8) / 18);
        int row = (int) Math.floor((y - topPos - 18) / 18);
        return column >= 0 && column < 9 && row >= 0 && row < 6 ? (fluidScrollRow + row) * 9 + column : -1;
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (!(menu.isFluidView() && slot instanceof SlotCloudChest)) super.renderSlot(graphics, slot);
    }

    private void renderFluids(GuiGraphics graphics, int mouseX, int mouseY) {
        var entries = menu.getClientFluids();
        fluidScrollRow = Math.min(fluidScrollRow, Math.max(0, (entries.size() + 8) / 9 - 6));
        int hovered = fluidGridIndex(mouseX, mouseY);
        for (int visible = 0; visible < 54; visible++) {
            int x = leftPos + 8 + visible % 9 * 18;
            int y = topPos + 18 + visible / 9 * 18;
            graphics.blit(TEXTURE, x - 1, y - 1, 0, 222, 18, 18);
            int index = fluidScrollRow * 9 + visible;
            if (index >= entries.size()) continue;
            var entry = entries.get(index);
            var extension = IClientFluidTypeExtensions.of(entry.fluid().getFluid());
            ResourceLocation texture = extension.getStillTexture(entry.fluid());
            if (texture != null) {
                var sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
                int color = extension.getTintColor(entry.fluid());
                graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, (color >>> 24) / 255F);
                graphics.blit(x, y, 0, 16, 16, sprite);
                graphics.setColor(1, 1, 1, 1);
            } else graphics.renderItem(new ItemStack(Items.BUCKET), x, y);
            if (hovered == index) graphics.fill(x, y, x + 16, y + 16, 0x60FFFFFF);
            String amount = entry.amount() >= 1000 ? String.format(Locale.ROOT, "%.1fB", entry.amount() / 1000D) : entry.amount() + "mB";
            graphics.pose().pushPose();
            graphics.pose().translate(x + 16, y + 11, 200);
            graphics.pose().scale(0.5F, 0.5F, 1);
            graphics.drawString(font, amount, -font.width(amount), 0, 0xFFFFFF, true);
            graphics.pose().popPose();
        }
        if (hovered >= 0) {
            java.util.List<Component> tooltip = new java.util.ArrayList<>();
            if (hovered < entries.size()) {
                var entry = entries.get(hovered);
                tooltip.add(entry.fluid().getHoverName());
                tooltip.add(Component.literal(String.format(Locale.ROOT, "%,d mB", entry.amount())));
            }
            tooltip.add(Component.translatable("cloudstorage_reborn.container.fluid_usage"));
            tooltip.add(Component.translatable("cloudstorage_reborn.container.fluid_capacity",
                    String.format(Locale.ROOT, "%,d", entries.stream().mapToLong(e -> e.amount()).sum()),
                    String.format(Locale.ROOT, "%,d", menu.getFluidCapacity())));
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderGraySlots(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        int i = this.leftPos;
        int j = this.topPos;
        int clampedSize = Math.min(this.slots, 54);
        int ySlots = clampedSize / 9;
        for (int k = 0; k < ySlots; ++k) {
            for (int l = 0; l < 9 && l + k * 9 < clampedSize; ++l) {
                if(menu.isSlotGray(l + k * 9)){
                    guiGraphics.blit(TEXTURE, i + 7 + l * 18, j + 17 + k * 18, 126, 222, 18, 18);
                }
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public boolean charTyped(char p_98521_, int p_98522_) {
        if (this.ignoreTextInput) {
            return false;
        }else if (this.mode != 2) {
            return false;
        } else {
            String s = this.searchBox.getValue();
            if (this.searchBox.charTyped(p_98521_, p_98522_)) {
                if (!Objects.equals(s, this.searchBox.getValue())) {
                    this.refreshSearchResults();
                }

                return true;
            } else {
                return false;
            }
        }
    }

    private void refreshSearchResults() {
        String s = this.searchBox.getValue().toLowerCase(Locale.ROOT);
        searchFor(s);
    }

    private void searchFor(String s) {
        ++searchRequestId;
        requestedSearch = s;
        receivedSearchItems = 0;
        searchMatches.clear();
        this.menu.scrollTo(0.0F, true);
        if (!s.isEmpty() && !s.startsWith("@") && !s.startsWith("#") && JustEnoughCharactersCompat.isAvailable()) {
            CloudStorage.sendMSGToServer(new MessageRequestCloudSearch(menu.containerId, searchRequestId, s));
        } else {
            CloudStorage.sendMSGToServer(new MessageSearchCloudChest(s));
        }
        this.menu.updateGrays(minecraft.player, s);
    }

    public void receiveSearchContents(MessageCloudSearchContents message) {
        if (message.containerId() != menu.containerId || message.requestId() != searchRequestId
                || !message.search().equals(requestedSearch)) return;
        if (message.offset() == 0) {
            receivedSearchItems = 0;
            searchMatches.clear();
        }
        if (message.offset() != receivedSearchItems || (long) message.offset() + message.items().size() > message.total()) return;
        for (var stack : message.items()) {
            if (CloudChestMenu.matchesSearch(minecraft.player, requestedSearch, stack)) searchMatches.set(receivedSearchItems);
            receivedSearchItems++;
        }
        if (receivedSearchItems == message.total()) {
            CloudStorage.sendMSGToServer(new MessageCloudSearchMatches(menu.containerId, searchRequestId, (BitSet) searchMatches.clone()));
        }
    }

    protected void renderLabels(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isFluidView()) {
            guiGraphics.drawString(font, Component.translatable("cloudstorage_reborn.container.fluids"), this.titleLabelX, this.titleLabelY, 4210752, false);
        } else if(mode == 2){
            guiGraphics.drawString(font, Component.translatable("cloudstorage_reborn.container.cloud_chest.searchbar"), this.titleLabelX + 20, this.titleLabelY, 4210752, false);
        }else{
            guiGraphics.drawString(font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        }
        guiGraphics.drawString(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    public boolean keyPressed(int p_98547_, int p_98548_, int p_98549_) {
        this.ignoreTextInput = false;
        if (mode == 2){
            boolean flag = this.hoveredSlot != null && this.hoveredSlot.hasItem();
            boolean flag1 = InputConstants.getKey(p_98547_, p_98548_).getNumericKeyValue().isPresent();
            if (flag && flag1 && this.checkHotbarKeyPressed(p_98547_, p_98548_)) {
                this.ignoreTextInput = true;
                return true;
            } else {
                String s = this.searchBox.getValue();
                if (this.searchBox.keyPressed(p_98547_, p_98548_, p_98549_)) {
                    if (!Objects.equals(s, this.searchBox.getValue())) {
                        this.refreshSearchResults();
                    }

                    return true;
                } else {
                    return this.searchBox.isFocused() && this.searchBox.isVisible() && p_98547_ != 256 ? true : super.keyPressed(p_98547_, p_98548_, p_98549_);
                }
            }
        }
        return super.keyPressed(p_98547_, p_98548_, p_98549_);
    }

    public boolean keyReleased(int p_98612_, int p_98613_, int p_98614_) {
        this.ignoreTextInput = false;
        return super.keyReleased(p_98612_, p_98613_, p_98614_);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalScroll, double verticalScroll) {
        if (menu.isFluidView()) {
            int max = Math.max(0, (menu.getClientFluids().size() + 8) / 9 - 6);
            fluidScrollRow = Mth.clamp(fluidScrollRow - (int) Math.signum(verticalScroll), 0, max);
            scrollOffs = max == 0 ? 0 : fluidScrollRow / (float) max;
            return true;
        }
        if (!this.canScroll()) {
            return false;
        } else {
            int i = (slots + 9 - 1) / 9 - 5;
            float f = (float) (verticalScroll / (double) i);
            this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
            this.menu.scrollTo(this.scrollOffs, true);
            return true;
        }
    }

    public boolean mouseDragged(double p_98535_, double p_98536_, int p_98537_, double p_98538_, double p_98539_) {
        if (this.scrolling) {
            int i = this.topPos + 18;
            int j = i + 112;
            this.scrollOffs = ((float) p_98536_ - (float) i - 7.5F) / ((float) (j - i) - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            if (menu.isFluidView()) fluidScrollRow = Math.round(scrollOffs * Math.max(0, (menu.getClientFluids().size() + 8) / 9 - 6));
            else this.menu.scrollTo(this.scrollOffs, true);
            return true;
        } else {
            return super.mouseDragged(p_98535_, p_98536_, p_98537_, p_98538_, p_98539_);
        }
    }

    public boolean mouseClicked(double x, double y, int mouseBtn) {
        int fluidIndex = fluidGridIndex(x, y);
        if (menu.isFluidView() && fluidIndex >= 0) {
            if (mouseBtn == 1 || mouseBtn == 0 && fluidIndex < menu.getClientFluids().size()) {
                CloudStorage.sendMSGToServer(new MessageCloudFluidAction(menu.containerId,
                        mouseBtn == 1 ? MessageCloudFluidAction.DEPOSIT : MessageCloudFluidAction.WITHDRAW,
                        menu.getFluidRevision(), fluidIndex));
            }
            return true;
        }
        if (mouseBtn == 0 && this.insideScrollbar(x, y)) {
            double d0 = x - (double) this.leftPos;
            double d1 = y - (double) this.topPos;
            this.scrolling = this.canScroll();
        }
        return super.mouseClicked(x, y, mouseBtn);
    }

    protected boolean insideScrollbar(double p_98524_, double p_98525_) {
        int i = this.leftPos;
        int j = this.topPos;
        int k = i + 175;
        int l = j + 18;
        int i1 = k + 14;
        int j1 = l + 112;
        return p_98524_ >= (double) k && p_98525_ >= (double) l && p_98524_ < (double) i1 && p_98525_ < (double) j1;
    }

    private boolean canScroll() {
        if (menu.isFluidView()) return menu.getClientFluids().size() > 54;
        return slots > 54;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.leftPos;
        int j = this.topPos;
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
        int clampedSize = Math.min(this.slots, 54);
        int ySlots = (int)Math.ceil(clampedSize / 9F);
        for (int k = 0; k < ySlots; ++k) {
            for (int l = 0; l < 9 && l + k * 9 < clampedSize; ++l) {
                boolean hidden = menu.isSlotGray(l + k * 9);
                guiGraphics.blit(TEXTURE, i + 7 + l * 18, j + 17 + k * 18, hidden ? 18 : 0, 222, 18, 18);
            }
        }
        int k = j + 18;
        int l = j + 130;
        float scrollOffsForRender = this.prevScrollOffs + (this.scrollOffs - this.prevScrollOffs) * partialTicks;
        guiGraphics.blit(TEXTURE, i + 175, k + (int) ((float) (l - k - 17) * scrollOffsForRender), 232 + (this.canScroll() ? 0 : 12), 0, 12, 15);
        if(this.mode == 2){
            guiGraphics.blit(TEXTURE, i + 79, k - 14, 36, 222, 90, 12);
            this.searchBox.render(guiGraphics, x, y, partialTicks);
        }
    }

    public void onClose() {
        this.searchFor("");
        super.onClose();
    }

    protected void containerTick() {
        if(this.mode == 2 || this.prevScrollOffs != scrollOffs){
            this.menu.updateGrays(minecraft.player, this.searchBox.getValue().toLowerCase(Locale.ROOT));
        }
        this.prevScrollOffs = scrollOffs;
        if(this.mode == 1){ // sort
            this.mode = 0;
            this.menu.scrollTo(0.0F, true);
            CloudStorage.sendMSGToServer(new MessageSortCloudChest(0));
            this.resetButtons();
        }
    }
}
