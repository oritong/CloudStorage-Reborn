package com.oritong.cloudstorage.tests;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.block.CloudChestBlockEntity;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.client.gui.CloudChestScreen;
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
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.file.Files;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

/** Real GUI clicks and network packets against an integrated server; excluded from release jars. */
@EventBusSubscriber(modid = CloudStorage.MODID, value = Dist.CLIENT)
public final class FluidClientSmokeTest {
    private static volatile int phase;
    private static int ticks;
    private static int frames;
    private static CloudIndex cloud;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.fluidSmokeTest")) return;
        var client = Minecraft.getInstance();
        if (++ticks > 3600) throw new AssertionError("Fluid GUI test timed out at phase " + phase);
        if (phase == 0 && client.getOverlay() == null && client.screen != null && ticks > 40) {
            phase = 1;
            client.createWorldOpenFlows().createFreshLevel("cloud-fluid-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("Cloud fluid test", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), client.screen);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.fluidSmokeTest") || event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        var player = event.getServer().getPlayerList().getPlayers().getFirst();
        if (phase == 1) {
            CloudStorage.CONFIG.beyondDimensionsStorage.set(false);
            cloud = new CloudIndex(player.getUUID(), 0xAB4D63, 27);
            cloud.getContainer().setItem(0, new ItemStack(Items.DIAMOND, 6));
            cloud.getFluids().fill(new FluidStack(Fluids.WATER, 6000), EXECUTE);
            cloud.getFluids().fill(new FluidStack(Fluids.LAVA, 2500), EXECUTE);
            CSWorldData.get(player.level()).addPrivateCloud(cloud);
            BlockPos pos = player.blockPosition().above(3);
            player.serverLevel().setBlockAndUpdate(pos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
            var chest = (CloudChestBlockEntity) player.level().getBlockEntity(pos);
            chest.setBalloonColorFor(player, cloud.getBalloonColor());
            CloudStorage.sendNonLocal(new MessageOpenCloudChest(27), player);
            player.openMenu(chest.getMenuProvider());
            player.containerMenu.setCarried(new ItemStack(Items.WATER_BUCKET));
            player.containerMenu.broadcastChanges();
            phase = 2;
        } else if (phase == 4 && cloud.getFluids().storedAmount() == 9500 && player.containerMenu.getCarried().is(Items.BUCKET)) {
            if (!((CloudChestMenu) player.containerMenu).isFluidView()) throw new AssertionError("Server did not switch to fluid view");
            phase = 5;
        } else if (phase == 6 && cloud.getFluids().storedAmount() == 8500 && player.containerMenu.getCarried().is(Items.WATER_BUCKET)) {
            player.containerMenu.setCarried(new ItemStack(Items.BUCKET, 2));
            player.containerMenu.broadcastChanges();
            phase = 7;
        } else if (phase == 8 && cloud.getFluids().storedAmount() == 7500 && player.containerMenu.getCarried().is(Items.BUCKET)
                && player.getInventory().countItem(Items.LAVA_BUCKET) == 1) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
            phase = 9;
        } else if (phase == 10 && !((CloudChestMenu) player.containerMenu).isFluidView()) {
            if (cloud.getContainer().getItem(0).getCount() != 6) throw new AssertionError("Fluid interaction changed hidden item storage");
            phase = 11;
        } else if (phase == 12) {
            for (int i = 0; i < 60; i++) {
                var fluid = new FluidStack(Fluids.WATER, 100 + i);
                fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Fluid variant " + i));
                cloud.getFluids().fill(fluid, EXECUTE);
            }
            player.containerMenu.broadcastChanges();
            phase = 13;
        }
    }

    @SubscribeEvent
    public static void hoveredRender(ScreenEvent.Render.Pre event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.fluidSmokeTest") || phase != 5
                || !(event.getScreen() instanceof CloudChestScreen screen)) return;
        screen.render(event.getGuiGraphics(), (screen.width - 195) / 2 + 16, (screen.height - 222) / 2 + 26, event.getPartialTick());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("cloudstorage_reborn.fluidSmokeTest")) return;
        var client = Minecraft.getInstance();
        if (!(client.screen instanceof CloudChestScreen screen)) return;
        var menu = screen.getMenu();
        int left = (screen.width - 195) / 2;
        int top = (screen.height - 222) / 2;
        if (phase == 2 && menu.getFluidCapacity() == 27000 && menu.getCarried().is(Items.WATER_BUCKET)) {
            screen.mouseClicked(left + 180, top + 195, 0);
            screen.mouseReleased(left + 180, top + 195, 0);
            phase = 3;
        } else if (phase == 3 && menu.isFluidView() && menu.getClientFluids().size() == 2 && ++frames > 20) {
            frames = 0;
            phase = 4;
            screen.mouseClicked(left + 52, top + 26, 1);
        } else if (phase == 5 && menu.getClientFluids().getFirst().amount() == 7000 && menu.getCarried().is(Items.BUCKET) && ++frames > 20) {
            capture(client, "cloud-fluids-deposit.png");
            frames = 0;
            phase = 6;
            screen.mouseClicked(left + 16, top + 26, 0);
        } else if (phase == 7 && menu.getClientFluids().getFirst().amount() == 6000 && menu.getCarried().is(Items.BUCKET) && menu.getCarried().getCount() == 2) {
            phase = 8;
            screen.mouseClicked(left + 34, top + 26, 0);
        } else if (phase == 9 && menu.getClientFluids().get(1).amount() == 1500 && menu.getCarried().isEmpty() && ++frames > 20) {
            capture(client, "cloud-fluids-withdraw.png");
            frames = 0;
            phase = 10;
            screen.mouseClicked(left + 180, top + 195, 0);
            screen.mouseReleased(left + 180, top + 195, 0);
        } else if (phase == 11 && !menu.isFluidView() && menu.slots.getFirst().getItem().is(Items.DIAMOND) && ++frames > 20) {
            capture(client, "cloud-fluids-items.png");
            frames = 0;
            phase = 12;
        } else if (phase == 13 && menu.getClientFluids().size() == 62) {
            screen.mouseClicked(left + 180, top + 195, 0);
            screen.mouseReleased(left + 180, top + 195, 0);
            screen.mouseScrolled(left + 50, top + 30, 0, -1);
            screen.mouseClicked(left + 180, top + 120, 0);
            screen.mouseDragged(left + 180, top + 122, 0, 0, 2);
            screen.mouseReleased(left + 180, top + 122, 0);
            phase = 14;
        } else if (phase == 14 && ++frames > 40) {
            capture(client, "cloud-fluids-scroll.png");
            CloudStorage.LOGGER.info("FLUID_CLIENT_SMOKE_TEST_PASSED: real GUI deposit, selected withdrawal, stacked buckets, item view and batched fluid scrolling");
            phase = 15;
            client.stop();
        }
    }

    private static void capture(Minecraft client, String filename) throws Exception {
        var output = client.gameDirectory.toPath().resolve("screenshots/" + filename);
        Files.createDirectories(output.getParent());
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) { image.writeToFile(output); }
    }
}
