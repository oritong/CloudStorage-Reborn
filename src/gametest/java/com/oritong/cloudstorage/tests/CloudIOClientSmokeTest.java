package com.oritong.cloudstorage.tests;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.block.CloudChestBlock;
import com.oritong.cloudstorage.block.CloudChestBlockEntity;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.client.gui.CloudChestScreen;
import com.oritong.cloudstorage.inventory.CloudIOSide;
import com.oritong.cloudstorage.misc.CloudFunction;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.network.MessageOpenCloudChest;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
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
public final class CloudIOClientSmokeTest {
    private static volatile int phase;
    private static int ticks, frames, originalScale;
    private static CloudIndex cloud;
    private static ItemEntity dropped;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.ioSmokeTest")) return;
        var client = Minecraft.getInstance();
        if (++ticks > 3600) throw new AssertionError("Cloud IO client timed out at phase " + phase);
        if (phase == 0 && client.getOverlay() == null && client.screen != null && ticks > 40) {
            originalScale = client.options.guiScale().get();
            client.options.guiScale().set(2);
            client.resizeDisplay();
            phase = 1;
            client.createWorldOpenFlows().createFreshLevel("cloud-io-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("Cloud IO test", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), client.screen);
        }
    }

    @SubscribeEvent public static void serverTick(ServerTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.ioSmokeTest") || event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        var player = event.getServer().getPlayerList().getPlayers().getFirst();
        if (phase == 1) {
            CloudStorage.CONFIG.beyondDimensionsStorage.set(false);
            cloud = new CloudIndex(player.getUUID(), 0xC27632, 90);
            cloud.unlock(CloudFunction.CRAFTING_TABLE);
            cloud.unlock(CloudFunction.HOPPER);
            cloud.getContainer().setItem(80, new ItemStack(Items.DIAMOND, 12));
            CSWorldData.get(player.level()).addPrivateCloud(cloud);
            var pos = player.blockPosition().above(3);
            player.serverLevel().setBlockAndUpdate(pos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState().setValue(CloudChestBlock.HORIZONTAL_FACING, Direction.NORTH));
            var chest = (CloudChestBlockEntity) player.level().getBlockEntity(pos);
            chest.setBalloonColorFor(player, cloud.getBalloonColor());
            player.serverLevel().setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
            player.serverLevel().setBlockAndUpdate(pos.west(), Blocks.CHEST.defaultBlockState());
            player.serverLevel().setBlockAndUpdate(pos.south(), Blocks.HOPPER.defaultBlockState());
            player.serverLevel().setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
            CloudStorage.sendNonLocal(new MessageOpenCloudChest(cloud.getContainerSize()), player);
            player.openMenu(chest.getMenuProvider());
            phase = 2;
        } else if (phase == 9 && player.containerMenu instanceof com.oritong.cloudstorage.inventory.CloudChestMenu menu && menu.isCollectingDrops()) {
            var pos = menu.getChestPos();
            dropped = new ItemEntity(player.level(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, new ItemStack(Items.APPLE, 3));
            player.serverLevel().addFreshEntity(dropped);
            phase = 90;
        } else if (phase == 90 && dropped.isRemoved() && cloud.getContainer().countItem(Items.APPLE) == 3) {
            player.containerMenu.broadcastChanges();
            phase = 10;
        }
    }

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("cloudstorage_reborn.ioSmokeTest")) return;
        var client = Minecraft.getInstance();
        if (!(client.screen instanceof CloudChestScreen screen) || ++frames < 20) return;
        var menu = screen.getMenu();
        if (phase == 2 && menu.getFunctionMask() == 3) {
            click(screen, -16, 48, 0);
            phase = 3;
            frames = 0;
        } else if (phase == 3 && menu.isIOOpen() && menu.getChestPos() != null) {
            if (menu.isCraftingOpen() || height(screen) > screen.height - 16) throw new AssertionError("IO panel did not replace crafting or compress storage rows");
            capture(client, "cloud-io-empty.png");
            side(screen, CloudIOSide.FRONT, 0);
            phase = 4;
        } else if (phase == 4 && menu.getIOMode(CloudIOSide.FRONT) == 1) {
            side(screen, CloudIOSide.LEFT, 0);
            phase = 5;
        } else if (phase == 5 && menu.getIOMode(CloudIOSide.LEFT) == 1) {
            side(screen, CloudIOSide.LEFT, 1);
            phase = 6;
        } else if (phase == 6 && menu.getIOMode(CloudIOSide.LEFT) == 0) {
            side(screen, CloudIOSide.RIGHT, 1);
            phase = 7;
        } else if (phase == 7 && menu.getIOMode(CloudIOSide.RIGHT) == 3) {
            side(screen, CloudIOSide.BOTTOM, 1);
            side(screen, CloudIOSide.BOTTOM, 1);
            phase = 8;
        } else if (phase == 8 && menu.getIOMode(CloudIOSide.BOTTOM) == 2) {
            side(screen, CloudIOSide.TOP, 0);
            client.gameMode.handleInventoryButtonClick(menu.containerId, 304);
            capture(client, "cloud-io-configured.png");
            click(screen, 181, 18 + screen.getVisibleRows() * 18 + 24, 0);
            phase = 9;
            frames = 0;
        } else if (phase == 10 && menu.isCollectingDrops() && menu.getTerminalEntries().stream().anyMatch(entry -> entry.item().is(Items.APPLE))) {
            if (menu.getIOMode(CloudIOSide.TOP) != 0) throw new AssertionError("Top face changed through a forged server button click");
            capture(client, "cloud-io-collection.png");
            click(screen, -16, 20, 0);
            phase = 11;
            frames = 0;
        } else if (phase == 11 && menu.isCraftingOpen() && !menu.isIOOpen()) {
            click(screen, -16, 48, 0);
            phase = 12;
            frames = 0;
        } else if (phase == 12 && menu.isIOOpen() && menu.getIOMode(CloudIOSide.FRONT) == 1) {
            click(screen, 158, 18 + screen.getVisibleRows() * 18 + 71, 0);
            phase = 13;
        } else if (phase == 13 && menu.getIOSettings() == 4096) {
            click(screen, 158, 18 + screen.getVisibleRows() * 18 + 71, 0);
            phase = 14;
        } else if (phase == 14 && menu.getIOMode(CloudIOSide.BOTTOM) == 1 && menu.getIOMode(CloudIOSide.BACK) == 1) {
            click(screen, 158, 18 + screen.getVisibleRows() * 18 + 71, 1);
            phase = 15;
        } else if (phase == 15 && menu.getIOSettings() == 4096) {
            CloudStorage.LOGGER.info("CLOUD_IO_CLIENT_SMOKE_TEST_PASSED: two function buttons, adaptive panel, neighboring blocks, left/right face cycling, fixed top, batch configuration, collection, and crafting panel switching");
            client.options.guiScale().set(originalScale);
            client.resizeDisplay();
            phase = 16;
            client.stop();
        }
    }

    private static int height(CloudChestScreen screen) { return 114 + screen.getVisibleRows() * 18 + screen.getMenu().getPanelHeight(); }
    private static void side(CloudChestScreen screen, CloudIOSide side, int button) {
        click(screen, side.x + 11, 18 + screen.getVisibleRows() * 18 + side.y + 11, button);
    }
    private static void click(CloudChestScreen screen, int x, int y, int button) {
        screen.mouseClicked((screen.width - 195) / 2 + x, (screen.height - height(screen)) / 2 + y, button);
        screen.mouseReleased((screen.width - 195) / 2 + x, (screen.height - height(screen)) / 2 + y, button);
    }
    private static void capture(Minecraft client, String filename) throws Exception {
        var output = client.gameDirectory.toPath().resolve("screenshots/" + filename);
        Files.createDirectories(output.getParent());
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) { image.writeToFile(output); }
    }
}
