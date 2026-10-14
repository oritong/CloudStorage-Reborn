package com.oritong.cloudstorage.tests;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.block.CloudChestBlockEntity;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.network.MessageOpenCloudChest;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraft.world.level.material.Fluids;

import java.nio.file.Files;

/** Development-only end-to-end check of a cloud chest opening the external mod's synced screen. */
@EventBusSubscriber(modid = CloudStorage.MODID, value = Dist.CLIENT)
public final class BeyondDimensionsClientSmokeTest {
    private static volatile int phase;
    private static int ticks;
    private static int frames;
    private static int nextTransferTick;
    private static volatile boolean intervalChecked;
    private static CloudChestBlockEntity chest;
    private static IItemHandler destination;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.beyondDimensionsSmokeTest")) return;
        Minecraft client = Minecraft.getInstance();
        if (++ticks > 3600) throw new AssertionError("Dimensional cloud screen did not complete within three minutes");
        if (phase == 0 && client.getOverlay() == null && client.screen != null && ticks > 40) {
            phase = 1;
            String world = "cloud-beyond-smoke-" + System.currentTimeMillis();
            client.createWorldOpenFlows().createFreshLevel(world,
                    new LevelSettings("Dimensional cloud test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), client.screen);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("cloudstorage_reborn.beyondDimensionsSmokeTest")) return;
        if (event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        var player = event.getServer().getPlayerList().getPlayers().getFirst();
        int serverTick = event.getServer().getTickCount();
        if (phase == 1) {
            var level = player.serverLevel();
            var netClass = Class.forName("com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet");
            Object net = netClass.getMethod("createNewNetForPlayer", Player.class, long.class, int.class).invoke(null, player, 1000L, 100);
            netClass.getMethod("setPrimaryNetForPlayer", Player.class, netClass).invoke(null, player, net);
            Object storage = netClass.getMethod("getUnifiedStorage").invoke(net);
            destination = (IItemHandler) Class.forName("com.wintercogs.beyonddimensions.api.capability.helper.unordered.ItemUnifiedStorageHandler")
                    .getConstructor(Class.forName("com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage")).newInstance(storage);
            var cloud = new CloudIndex(player.getUUID(), 0xABCDEF, 27);
            ItemStack diamonds = new ItemStack(Items.DIAMOND, 12);
            diamonds.set(DataComponents.CUSTOM_NAME, Component.literal("Cloud transfer test"));
            cloud.getContainer().setItem(3, diamonds);
            cloud.getFluids().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
            CSWorldData.get(level).addPrivateCloud(cloud);
            BlockPos pos = player.blockPosition().above(3);
            level.setBlockAndUpdate(pos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
            chest = (CloudChestBlockEntity) level.getBlockEntity(pos);
            chest.setBalloonColorFor(player, cloud.getBalloonColor());
            if (CloudStorage.CONFIG.beyondDimensionsTransferIntervalSeconds.get() != 5)
                throw new AssertionError("Background transfer interval should default to five seconds");
            CloudStorage.CONFIG.beyondDimensionsTransferIntervalSeconds.set(60);
            CloudStorage.CONFIG.beyondDimensionsStorage.set(true);
            CloudStorage.sendNonLocal(new MessageOpenCloudChest(27), player);
            player.openMenu(chest.getMenuProvider());
            if (!cloud.getContainer().isEmpty() || cloud.getFluids().storedAmount() != 0 || destination.getStackInSlot(0).getCount() != 12)
                throw new AssertionError("Cloud items did not enter the personal network before opening its screen");
            CloudStorage.CONFIG.beyondDimensionsTransferIntervalSeconds.set(5);
            phase = 7;
        } else if (phase == 7 && serverTick % 100 == 1) {
            // Start just after a scheduled scan so a regression to one-second scanning is observable.
            var cloud = CSWorldData.get(player.level()).getPrivateCloud(player.getUUID(), 0xABCDEF);
            cloud.getContainer().setItem(11, new ItemStack(Items.GOLD_INGOT, 7));
            nextTransferTick = serverTick + 99;
            intervalChecked = false;
            phase = 2;
        } else if (phase == 2) {
            checkInterval(serverTick, Items.GOLD_INGOT, 7, 5);
        } else if (phase == 3) {
            CloudStorage.CONFIG.beyondDimensionsTransferIntervalSeconds.set(1);
            phase = 8;
        } else if (phase == 8 && serverTick % 20 == 1) {
            var cloud = CSWorldData.get(player.level()).getPrivateCloud(player.getUUID(), 0xABCDEF);
            cloud.getContainer().setItem(12, new ItemStack(Items.COPPER_INGOT, 4));
            nextTransferTick = serverTick + 19;
            intervalChecked = false;
            phase = 9;
        } else if (phase == 9) {
            checkInterval(serverTick, Items.COPPER_INGOT, 4, 1);
        } else if (phase == 10) {
            // Menu invalidation must still take effect immediately, even with a long scan interval.
            CloudStorage.CONFIG.beyondDimensionsTransferIntervalSeconds.set(60);
            CloudStorage.CONFIG.beyondDimensionsStorage.set(false);
            phase = 6;
        } else if (phase == 6 && player.containerMenu == player.inventoryMenu) {
            // Integration must close its existing menu when the server turns it off.
            phase = 4;
            var cloud = CSWorldData.get(player.level()).getPrivateCloud(player.getUUID(), 0xABCDEF);
            cloud.getContainer().setItem(4, new ItemStack(Items.APPLE, 6));
            CloudStorage.sendNonLocal(new MessageOpenCloudChest(27), player);
            player.openMenu(chest.getMenuProvider());
            if (!(player.containerMenu instanceof CloudChestMenu)) throw new AssertionError("Disabling integration did not restore ordinary menus");
        }
    }

    private static void checkInterval(int serverTick, net.minecraft.world.item.Item item, int expectedCount, int seconds) {
        int actualCount = 0;
        for (int slot = 0; slot < destination.getSlots(); slot++) {
            ItemStack stack = destination.getStackInSlot(slot);
            if (stack.is(item)) actualCount += stack.getCount();
        }
        if (serverTick < nextTransferTick && actualCount != 0)
            throw new AssertionError("Background transfer ran before the configured interval");
        if (serverTick > nextTransferTick + 2 && actualCount != expectedCount)
            throw new AssertionError("Background transfer ignored the configured interval");
        if (actualCount == expectedCount && !intervalChecked) {
            intervalChecked = true;
            CloudStorage.LOGGER.info("BEYOND_DIMENSIONS_INTERVAL_PASSED: {} seconds", seconds);
        }
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("cloudstorage_reborn.beyondDimensionsSmokeTest")) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen == null) return;
        if (phase == 2) {
            if (!client.player.containerMenu.getClass().getName().endsWith(".DimensionsNetMenu")) return;
            Object storage = client.player.containerMenu.getClass().getField("storage").get(client.player.containerMenu);

            // Client storage uses the base unordered handler, so inspect its public item key/count API instead.
            Class<?> key = Class.forName("com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey");
            Class<?> keyApi = Class.forName("com.wintercogs.beyonddimensions.api.storage.key.IStackKey");
            ItemStack diamonds = new ItemStack(Items.DIAMOND);
            diamonds.set(DataComponents.CUSTOM_NAME, Component.literal("Cloud transfer test"));
            Object amount = storage.getClass().getMethod("getStackByKey", keyApi).invoke(storage, key.getConstructor(ItemStack.class).newInstance(diamonds));
            if ((long) amount.getClass().getMethod("amount").invoke(amount) != 12L) return;
            Object gold = storage.getClass().getMethod("getStackByKey", keyApi).invoke(storage, key.getConstructor(ItemStack.class).newInstance(new ItemStack(Items.GOLD_INGOT)));
            if ((long) gold.getClass().getMethod("amount").invoke(gold) != 7L) return;
            Class<?> fluidKey = Class.forName("com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey");
            Object water = storage.getClass().getMethod("getStackByKey", keyApi).invoke(storage,
                    fluidKey.getConstructor(FluidStack.class).newInstance(new FluidStack(Fluids.WATER, 1)));
            if ((long) water.getClass().getMethod("amount").invoke(water) != 1000L) return;
            if (++frames < 40) return;
            capture(client, "cloud-beyond-smoke.png");
            CloudStorage.LOGGER.info("BEYOND_DIMENSIONS_SCREEN_PASSED: transferred items and fluids reached the client's dimensional storage screen");
            frames = 0;
            phase = 3;
        } else if (phase == 9 && intervalChecked) {
            Object storage = client.player.containerMenu.getClass().getField("storage").get(client.player.containerMenu);
            Class<?> key = Class.forName("com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey");
            Class<?> keyApi = Class.forName("com.wintercogs.beyonddimensions.api.storage.key.IStackKey");
            Object copper = storage.getClass().getMethod("getStackByKey", keyApi).invoke(storage,
                    key.getConstructor(ItemStack.class).newInstance(new ItemStack(Items.COPPER_INGOT)));
            if ((long) copper.getClass().getMethod("amount").invoke(copper) != 4L) return;
            phase = 10;
        } else if (phase == 4 && client.player.containerMenu instanceof CloudChestMenu menu) {
            if (menu.slots.stream().noneMatch(slot -> slot.getItem().is(Items.APPLE) && slot.getItem().getCount() == 6)) return;
            if (++frames < 40) return;
            capture(client, "cloud-beyond-disabled-smoke.png");
            if (destination.getStackInSlot(0).getCount() != 12) throw new AssertionError("Turning integration off changed the transferred inventory");
            CloudStorage.LOGGER.info("BEYOND_DIMENSIONS_CLIENT_SMOKE_TEST_PASSED: external and ordinary menus synchronized and rendered");
            phase = 5;
            client.stop();
        }
    }

    private static void capture(Minecraft client, String filename) throws Exception {
        var output = client.gameDirectory.toPath().resolve("screenshots/" + filename);
        Files.createDirectories(output.getParent());
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            image.writeToFile(output);
        }
    }
}
