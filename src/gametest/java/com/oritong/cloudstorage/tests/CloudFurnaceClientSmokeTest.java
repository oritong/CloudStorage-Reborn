package com.oritong.cloudstorage.tests;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.block.CloudChestBlockEntity;
import com.oritong.cloudstorage.client.gui.CloudChestScreen;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.misc.CloudFunction;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.network.MessageOpenCloudChest;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Files;

@EventBusSubscriber(modid = CloudStorage.MODID, value = Dist.CLIENT)
public final class CloudFurnaceClientSmokeTest {
    private static volatile int phase;
    private static int ticks, frames, originalScale;
    private static CloudIndex cloud;
    private static BlockPos chestPos;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.furnaceSmokeTest")) return;
        var client = Minecraft.getInstance();
        if (++ticks > 3600) throw new AssertionError("Cloud furnace client timed out at phase " + phase);
        if (phase == 0 && client.getOverlay() == null && client.screen != null && ticks > 40) {
            originalScale = client.options.guiScale().get();
            client.options.guiScale().set(2);
            client.resizeDisplay();
            phase = 1;
            client.createWorldOpenFlows().createFreshLevel("cloud-furnace-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("Cloud furnace test", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), client.screen);
        }
    }
    @SubscribeEvent public static void serverTick(ServerTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.furnaceSmokeTest") || event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        var player = event.getServer().getPlayerList().getPlayers().getFirst();
        if (phase == 1) {
            CloudStorage.CONFIG.beyondDimensionsStorage.set(false);
            cloud = new CloudIndex(player.getUUID(), 0xA6C633, 90);
            cloud.uploadFunction(CloudFunction.CRAFTING_TABLE);
            cloud.uploadFunction(CloudFunction.HOPPER);
            cloud.uploadFunction(CloudFunction.FURNACE);
            cloud.getContainer().setItem(60, new ItemStack(Items.DIAMOND, 12));
            player.getInventory().setItem(0, new ItemStack(Items.RAW_IRON, 64));
            player.getInventory().setItem(1, new ItemStack(Items.COAL, 64));
            CSWorldData.get(player.level()).addPrivateCloud(cloud);
            chestPos = player.blockPosition().above(3);
            player.serverLevel().setBlockAndUpdate(chestPos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
            ((CloudChestBlockEntity) player.level().getBlockEntity(chestPos)).setBalloonColorFor(player, cloud.getBalloonColor());
            open(player); phase = 2;
        } else if (phase == 5) {
            upgrade(16);
            cloud.getFurnace().setItem(1, new ItemStack(Items.RAW_GOLD, 32));
            phase = 6;
        } else if (phase == 7) { upgrade(32); phase = 8; }
        else if (phase == 9) { upgrade(64); phase = 10; }
        else if (phase == 13 && cloud.getContainer().countItem(Items.IRON_INGOT) > 0 && cloud.getContainer().countItem(Items.GOLD_INGOT) > 0) {
            int total = cloud.getFurnace().countItem(Items.RAW_IRON) + cloud.getFurnace().countItem(Items.RAW_GOLD)
                    + cloud.getFurnace().countItem(Items.IRON_INGOT) + cloud.getFurnace().countItem(Items.GOLD_INGOT)
                    + cloud.getContainer().countItem(Items.IRON_INGOT) + cloud.getContainer().countItem(Items.GOLD_INGOT);
            if (total != 96) throw new AssertionError("Real smelting lost or duplicated items: " + total);
            phase = 14;
        } else if (phase == 15 && !(player.containerMenu instanceof CloudChestMenu)) {
            open(player); phase = 16;
        }
    }
    private static void upgrade(int count) { while (cloud.getFurnace().count() < count) cloud.uploadFunction(CloudFunction.FURNACE); }
    private static void open(net.minecraft.server.level.ServerPlayer player) {
        CloudStorage.sendNonLocal(new MessageOpenCloudChest(cloud.getContainerSize()), player);
        player.openMenu(((CloudChestBlockEntity) player.level().getBlockEntity(chestPos)).getMenuProvider());
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("cloudstorage_reborn.furnaceSmokeTest")) return;
        var client = Minecraft.getInstance();
        if (!(client.screen instanceof CloudChestScreen screen) || ++frames < 20) return;
        var menu = screen.getMenu();
        var furnace = menu.getFurnace();
        if (phase == 2 && menu.getFunctionMask() == 7 && furnace.count() == 1) {
            click(screen, -16, 76); phase = 3; frames = 0;
        } else if (phase == 3 && menu.isFurnaceOpen()) {
            checkSlots(menu, 1);
            if (furnace.flags() != 0) throw new AssertionError("Furnace switches did not start disabled");
            capture(client, "cloud-furnace-1.png");
            for (var slot : menu.slots) if (slot.container == client.player.getInventory() && slot.getContainerSlot() < 2)
                client.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.QUICK_MOVE, client.player);
            phase = 4;
        } else if (phase == 4 && furnace.getItem(0).is(Items.RAW_IRON) && furnace.getItem(9).is(Items.COAL)) {
            phase = 5; frames = 0;
        } else if (phase == 6 && furnace.count() == 16 && furnace.getItem(1).is(Items.RAW_GOLD)) {
            checkSlots(menu, 2); capture(client, "cloud-furnace-16.png"); phase = 7; frames = 0;
        } else if (phase == 8 && furnace.count() == 32) {
            checkSlots(menu, 4); capture(client, "cloud-furnace-32.png"); phase = 9; frames = 0;
        } else if (phase == 10 && furnace.count() == 64) {
            checkSlots(menu, 9);
            if (height(screen) > screen.height) throw new AssertionError("Furnace panel extended outside the screen");
            capture(client, "cloud-furnace-64-compact.png");
            client.options.guiScale().set(1); client.resizeDisplay(); phase = 11; frames = 0;
        } else if (phase == 11 && screen.getVisibleRows() == 6) {
            capture(client, "cloud-furnace-64.png");
            for (int i = 0; i < 3; i++) click(screen, 181, 18 + screen.getVisibleRows() * 18 + 24 + 18 * i);
            phase = 12; frames = 0;
        } else if (phase == 12 && furnace.flags() == 7) {
            capture(client, "cloud-furnace-enabled.png"); phase = 13; frames = 0;
        } else if (phase == 14 && menu.getTerminalEntries().stream().anyMatch(e -> e.item().is(Items.IRON_INGOT))
                && menu.getTerminalEntries().stream().anyMatch(e -> e.item().is(Items.GOLD_INGOT))) {
            capture(client, "cloud-furnace-results.png"); client.player.closeContainer(); phase = 15; frames = 0;
        } else if (phase == 16 && furnace.count() == 64 && furnace.flags() == 7) {
            click(screen, -16, 76); phase = 17; frames = 0;
        } else if (phase == 17 && menu.isFurnaceOpen()) {
            checkSlots(menu, 9);
            CloudStorage.LOGGER.info("CLOUD_FURNACE_CLIENT_SMOKE_TEST_PASSED: all tiers, live upgrades, real shift input/fuel, parallel recipes, auto output, switches, compact layout and reopen persistence");
            client.options.guiScale().set(originalScale); client.resizeDisplay(); phase = 18; client.stop();
        }
    }
    private static void checkSlots(CloudChestMenu menu, int expected) {
        long active = menu.slots.subList(menu.furnaceStart(), menu.furnaceStart() + 27).stream().filter(s -> s.isActive()).count();
        if (active != expected * 3) throw new AssertionError("Wrong active furnace slot count: " + active);
        if (menu.slots.subList(menu.furnaceStart(), menu.furnaceStart() + 27).stream().anyMatch(s -> s.isActive() && s.x + 17 >= 174))
            throw new AssertionError("Furnace slots overlap the toggle buttons");
    }
    private static int height(CloudChestScreen screen) { return 114 + screen.getVisibleRows() * 18 + screen.getMenu().getPanelHeight(); }
    private static void click(CloudChestScreen screen, int x, int y) {
        int left = (screen.width - 195) / 2, top = (screen.height - height(screen)) / 2;
        screen.mouseClicked(left + x, top + y, 0); screen.mouseReleased(left + x, top + y, 0);
    }
    private static void capture(Minecraft client, String filename) throws Exception {
        var path = client.gameDirectory.toPath().resolve("screenshots/" + filename);
        Files.createDirectories(path.getParent());
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) { image.writeToFile(path); }
    }
}
