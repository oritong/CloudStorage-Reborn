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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.file.Files;

@EventBusSubscriber(modid = CloudStorage.MODID, value = Dist.CLIENT)
public final class CloudCraftingClientSmokeTest {
    private static volatile int phase;
    private static int ticks, frames, originalScale;
    private static CloudIndex cloud;
    private static BlockPos chestPos;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.craftingSmokeTest")) return;
        var client = Minecraft.getInstance();
        if (++ticks > 3600) throw new AssertionError("Cloud crafting client timed out at phase " + phase);
        if (phase == 0 && client.getOverlay() == null && client.screen != null && ticks > 40) {
            originalScale = client.options.guiScale().get();
            client.options.guiScale().set(2);
            client.resizeDisplay();
            phase = 1;
            client.createWorldOpenFlows().createFreshLevel("cloud-crafting-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("Cloud crafting test", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), client.screen);
        }
    }

    @SubscribeEvent public static void serverTick(ServerTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.craftingSmokeTest") || event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        var player = event.getServer().getPlayerList().getPlayers().getFirst();
        if (phase == 1) {
            CloudStorage.CONFIG.beyondDimensionsStorage.set(false);
            cloud = new CloudIndex(player.getUUID(), 0xA6C232, 90);
            cloud.unlock(CloudFunction.CRAFTING_TABLE);
            cloud.getContainer().setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
            cloud.getContainer().setItem(60, new ItemStack(Items.DIAMOND, 12));
            cloud.addFluidStorage(4000);
            cloud.getFluids().fill(new FluidStack(Fluids.WATER, 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            cloud.getCrafting().setItem(0, new ItemStack(Items.OAK_PLANKS));
            cloud.getCrafting().setItem(3, new ItemStack(Items.OAK_PLANKS));
            CSWorldData.get(player.level()).addPrivateCloud(cloud);
            chestPos = player.blockPosition().above(3);
            player.serverLevel().setBlockAndUpdate(chestPos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
            ((CloudChestBlockEntity) player.level().getBlockEntity(chestPos)).setBalloonColorFor(player, cloud.getBalloonColor());
            open(player);
            phase = 2;
        } else if (phase == 6 && player.getInventory().countItem(Items.STICK) == 20 && cloud.getCrafting().isEmpty()) {
            cloud.getCrafting().setItem(8, new ItemStack(Items.APPLE, 3));
            player.containerMenu.broadcastChanges();
            phase = 7;
        } else if (phase == 8 && !(player.containerMenu instanceof CloudChestMenu)) {
            if (cloud.getCrafting().countItem(Items.APPLE) != 3) throw new AssertionError("Default close discarded crafting ingredients");
            open(player);
            phase = 9;
        } else if (phase == 11 && !(player.containerMenu instanceof CloudChestMenu)) {
            if (cloud.getCrafting().countItem(Items.APPLE) != 0 || cloud.getContainer().countItem(Items.APPLE) != 3)
                throw new AssertionError("Auto clear did not return ingredients on real close");
            phase = 12;
        }
    }

    private static void open(net.minecraft.server.level.ServerPlayer player) {
        CloudStorage.sendNonLocal(new MessageOpenCloudChest(cloud.getContainerSize()), player);
        player.openMenu(((CloudChestBlockEntity) player.level().getBlockEntity(chestPos)).getMenuProvider());
    }

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("cloudstorage_reborn.craftingSmokeTest")) return;
        var client = Minecraft.getInstance();
        if (phase == 12) {
            CloudStorage.LOGGER.info("CLOUD_CRAFTING_CLIENT_SMOKE_TEST_PASSED: actual sidebar/buttons, adaptive rows, mixed terminal, slot sync, auto pull/shift craft, persistence and auto clear");
            client.options.guiScale().set(originalScale);
            client.resizeDisplay();
            phase = 13;
            client.stop();
            return;
        }
        if (!(client.screen instanceof CloudChestScreen screen) || ++frames < 20) return;
        var menu = screen.getMenu();
        if (phase == 2 && menu.getFunctionMask() == 1 && !menu.isCraftingOpen()) {
            capture(client, "cloud-crafting-sidebar.png");
            phase = 3;
            frames = 0;
            click(screen, -16, 20);
        } else if (phase == 3 && menu.isCraftingOpen() && menu.slots.get(menu.craftingResultSlot()).getItem().is(Items.STICK)) {
            int expectedRows = Math.max(1, Math.min(6, (screen.height - 16 - 192) / 18));
            if (screen.getVisibleRows() != expectedRows) return;
            if (guiHeight(screen) > screen.height - 16) throw new AssertionError("Crafting panel did not compress cloud rows");
            capture(client, "cloud-crafting-compact.png");
            client.options.guiScale().set(1);
            client.resizeDisplay();
            phase = 4;
            frames = 0;
        } else if (phase == 4 && screen.getVisibleRows() == 6) {
            capture(client, "cloud-crafting-expanded.png");
            phase = 5;
            frames = 0;
            click(screen, 181, 18 + screen.getVisibleRows() * 18 + 41);
        } else if (phase == 5 && menu.isAutoPull()) {
            phase = 6;
            frames = 0;
            client.gameMode.handleInventoryMouseClick(menu.containerId, menu.craftingResultSlot(), 0, ClickType.QUICK_MOVE, client.player);
        } else if (phase == 7 && menu.getCrafting().countItem(Items.APPLE) == 3 && client.player.getInventory().countItem(Items.STICK) == 20) {
            phase = 8;
            frames = 0;
            client.player.closeContainer();
        } else if (phase == 9 && menu.getCrafting().countItem(Items.APPLE) == 3 && !menu.isCraftingOpen()) {
            click(screen, -16, 20);
            phase = 10;
            frames = 0;
        } else if (phase == 10 && menu.isCraftingOpen()) {
            click(screen, 181, 18 + screen.getVisibleRows() * 18 + 23);
            phase = 14;
            frames = 0;
        } else if (phase == 14 && menu.isAutoClear()) {
            phase = 11;
            client.player.closeContainer();
        }
    }

    private static int guiHeight(CloudChestScreen screen) {
        return 114 + screen.getVisibleRows() * 18 + (screen.getMenu().isCraftingOpen() ? 78 : 0);
    }
    private static void click(CloudChestScreen screen, int x, int y) {
        int left = (screen.width - 195) / 2;
        int top = (screen.height - guiHeight(screen)) / 2;
        screen.mouseClicked(left + x, top + y, 0);
        screen.mouseReleased(left + x, top + y, 0);
    }
    private static void capture(Minecraft client, String filename) throws Exception {
        var output = client.gameDirectory.toPath().resolve("screenshots/" + filename);
        Files.createDirectories(output.getParent());
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) { image.writeToFile(output); }
    }
}
