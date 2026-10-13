package com.oritong.cloudstorage.tests;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.client.compat.JustEnoughCharactersCompat;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.item.BalloonItem;
import com.oritong.cloudstorage.item.CSItemRegistry;
import com.oritong.cloudstorage.item.StackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Development-only check: start the client, render every item, capture it and exit. */
@EventBusSubscriber(modid = CloudStorage.MODID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static int readyTicks;
    private static int renderedFrames;
    private static boolean started;
    private static boolean checkingBook;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("cloudstorage_reborn.clientSmokeTest") || started) return;
        Minecraft client = Minecraft.getInstance();
        if (client.getOverlay() != null || client.screen == null || ++readyTicks < 40) return;
        started = true;
        boolean expectJec = ModList.get().isLoaded("jecharacters");
        if (JustEnoughCharactersCompat.isAvailable() != expectJec) {
            throw new AssertionError("Unexpected JEC availability: expected " + expectJec);
        }
        String diamondName = "\u94bb\u77f3";
        if (!JustEnoughCharactersCompat.contains(diamondName, diamondName)
                || JustEnoughCharactersCompat.contains(diamondName, "nonsense")) {
            throw new AssertionError("Literal search changed");
        }
        if (JustEnoughCharactersCompat.contains(diamondName, "zuanshi") != expectJec
                || JustEnoughCharactersCompat.contains(diamondName, "zs") != expectJec) {
            throw new AssertionError("Full pinyin or initials search failed");
        }
        CloudStorage.LOGGER.info("JEC_SEARCH_SMOKE_TEST_PASSED: optional dependency available={}", expectJec);
        String name = Component.translatable("itemGroup.cloudstorage_reborn").getString();
        if (!name.equals("云存储：涅槃") && !name.equals("Cloud Storage:Reborn")) {
            throw new AssertionError("Mod name did not translate: " + name);
        }
        var pages = client.getResourceManager().listResources("book/guide_book", id -> id.getNamespace().equals(CloudStorage.MODID) && id.getPath().endsWith(".json"));
        if (pages.size() != 7) throw new AssertionError("Missing guide book pages");
        for (var resource : pages.values()) {
            try (var reader = resource.openAsReader()) {
                com.github.alexthe666.citadel.client.gui.BookPage.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, com.google.gson.JsonParser.parseReader(reader)).getOrThrow();
            }
        }
        client.setScreen(new ItemPreview());
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        Minecraft client = Minecraft.getInstance();
        if (!(client.screen instanceof ItemPreview) && !(checkingBook && client.screen instanceof com.oritong.cloudstorage.client.gui.GuideBookScreen)) return;
        if (++renderedFrames != 40) return;
        var output = client.gameDirectory.toPath().resolve(checkingBook ? "screenshots/guide-book-smoke.png" : "screenshots/client-smoke.png");
        Files.createDirectories(output.getParent());
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            image.writeToFile(output);
        }
        if (!checkingBook) {
            checkingBook = true;
            renderedFrames = 0;
            client.setScreen(new com.oritong.cloudstorage.client.gui.GuideBookScreen(new ItemStack(CSItemRegistry.GUIDE_BOOK.get())));
            return;
        }
        CloudStorage.LOGGER.info("CLIENT_SMOKE_TEST_PASSED: all item models and guide book rendered; screenshot {}", output);
        client.stop();
    }

    private static final class ItemPreview extends Screen {
        private final List<ItemStack> items = new ArrayList<>();
        ItemPreview() {
            super(Component.translatable("itemGroup.cloudstorage_reborn"));
            CSItemRegistry.DEF_REG.getEntries().forEach(item -> items.add(new ItemStack(item.get())));
            CSBlockRegistry.DEF_REG.getEntries().forEach(block -> items.add(new ItemStack(block.get())));
            ItemStack balloon = new ItemStack(CSItemRegistry.BALLOON.get());
            ((BalloonItem) balloon.getItem()).setColor(balloon, 0x48C9E8);
            StackData.update(balloon, tag -> tag.putInt("3DRender", 1));
            items.add(balloon);
        }
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF182334);
            graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
            graphics.drawCenteredString(font, "Minecraft 1.21.1 / NeoForge / Citadel 2.7.1", width / 2, 29, 0xA9C2D9);
            for (int i = 0; i < items.size(); i++) {
                int x = 18 + (i % 10) * 28;
                int y = 55 + (i / 10) * 32;
                graphics.renderItem(items.get(i), x, y);
            }
        }
    }
}
