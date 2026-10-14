package com.oritong.cloudstorage.tests;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.block.CloudChestBlockEntity;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.client.gui.CloudChestScreen;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.CloudTerminalEntry;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.network.MessageOpenCloudChest;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import com.oritong.cloudstorage.inventory.SlotCloudChest;
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
import java.util.List;
import java.util.function.Predicate;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

/** Tests real clicks, mixed search/scrolling, and Shift-locked positions against an integrated server. */
@EventBusSubscriber(modid = CloudStorage.MODID, value = Dist.CLIENT)
public final class FluidClientSmokeTest {
    private static volatile int phase;
    private static int ticks;
    private static int frames;
    private static CloudIndex cloud;
    private static List<CloudTerminalEntry> locked;
    private static Object mouseTweaksHandler;
    private static Object mouseTweaksLeft;
    private static Class<?> mouseTweaksButtonClass;
    private static int mouseTweaksCycles;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.fluidSmokeTest")) return;
        var client = Minecraft.getInstance();
        if (++ticks > 3600) throw new AssertionError("Mixed terminal test timed out at phase " + phase);
        if (phase == 0 && client.getOverlay() == null && client.screen != null && ticks > 40) {
            phase = 1;
            client.createWorldOpenFlows().createFreshLevel("cloud-mixed-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("Mixed cloud terminal test", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
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
            cloud = new CloudIndex(player.getUUID(), 0xAB4D63, 90);
            cloud.getContainer().setItem(80, new ItemStack(Items.DIAMOND, 6));
            cloud.getContainer().setItem(60, new ItemStack(Items.GOLD_INGOT, 9));
            cloud.getFluids().fill(new FluidStack(Fluids.WATER, 6000), EXECUTE);
            cloud.getFluids().fill(new FluidStack(Fluids.LAVA, 2500), EXECUTE);
            CSWorldData.get(player.level()).addPrivateCloud(cloud);
            BlockPos pos = player.blockPosition().above(3);
            player.serverLevel().setBlockAndUpdate(pos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
            var chest = (CloudChestBlockEntity) player.level().getBlockEntity(pos);
            chest.setBalloonColorFor(player, cloud.getBalloonColor());
            CloudStorage.sendNonLocal(new MessageOpenCloudChest(90), player);
            player.openMenu(chest.getMenuProvider());
            player.containerMenu.setCarried(new ItemStack(Items.WATER_BUCKET));
            player.containerMenu.broadcastChanges();
            phase = 2;
        } else if (phase == 3 && cloud.getFluids().storedAmount() == 9500 && player.containerMenu.getCarried().is(Items.BUCKET)) {
            phase = 4;
        } else if (phase == 5 && cloud.getFluids().storedAmount() == 8500 && player.containerMenu.getCarried().is(Items.WATER_BUCKET)) {
            player.containerMenu.setCarried(new ItemStack(Items.BUCKET, 2));
            player.containerMenu.broadcastChanges();
            phase = 6;
        } else if (phase == 7 && cloud.getFluids().storedAmount() == 7500 && player.containerMenu.getCarried().is(Items.BUCKET)
                && player.getInventory().countItem(Items.LAVA_BUCKET) == 1) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
            phase = 8;
        } else if (phase == 9 && cloud.getContainer().getItem(80).isEmpty() && player.getInventory().countItem(Items.DIAMOND) == 6) {
            // Deposit an item through the GUI while Shift remains held, alongside a newly uploaded fluid.
            player.containerMenu.setCarried(new ItemStack(Items.APPLE, 4));
            cloud.getFluids().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
            cloud.getFluids().fill(shiftFluid(500), EXECUTE);
            player.containerMenu.broadcastChanges();
            phase = 90;
        } else if (phase == 91 && cloud.getContainer().countItem(Items.APPLE) == 0 && player.getInventory().countItem(Items.APPLE) == 4) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(Items.APPLE)) player.getInventory().setItem(i, ItemStack.EMPTY);
            }
            // Occupy the old apple slot with an existing resource; returning apples must use a different source.
            cloud.getContainer().setItem(0, cloud.getContainer().removeItemNoUpdate(60));
            player.containerMenu.setCarried(new ItemStack(Items.APPLE, 4));
            cloud.getFluids().fill(shiftFluid(500), EXECUTE);
            player.containerMenu.broadcastChanges();
            phase = 92;
        } else if (phase == 13 && player.containerMenu.getCarried().is(Items.APPLE) && cloud.getContainer().countItem(Items.APPLE) == 0) {
            phase = 14;
        } else if (phase == 15 && player.containerMenu.getCarried().isEmpty() && cloud.getContainer().countItem(Items.APPLE) == 4) {
            for (int i = 0; i < 60; i++) {
                var fluid = new FluidStack(Fluids.WATER, 100 + i);
                fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Fluid variant " + i));
                cloud.getFluids().fill(fluid, EXECUTE);
            }
            player.containerMenu.broadcastChanges();
            phase = 16;
        } else if (phase == 100) {
            cloud.getContainer().clearContent();
            player.getInventory().clearContent();
            ItemStack diamond = new ItemStack(Items.DIAMOND, 6);
            diamond.set(DataComponents.CUSTOM_NAME, Component.literal("000 Diamond"));
            ItemStack gold = new ItemStack(Items.GOLD_INGOT, 9);
            gold.set(DataComponents.CUSTOM_NAME, Component.literal("001 Gold"));
            ItemStack apple = new ItemStack(Items.APPLE, 4);
            apple.set(DataComponents.CUSTOM_NAME, Component.literal("002 Apple"));
            cloud.getContainer().setItem(80, diamond);
            cloud.getContainer().setItem(60, gold);
            cloud.getContainer().setItem(0, apple);
            player.containerMenu.broadcastChanges();
            phase = 101;
        } else if ((phase == 102 || phase == 106) && cloud.getContainer().isEmpty() && player.getInventory().countItem(Items.DIAMOND) == 6
                && player.getInventory().countItem(Items.GOLD_INGOT) == 9 && player.getInventory().countItem(Items.APPLE) == 4) {
            phase = 103;
        } else if (phase == 104 && player.getInventory().countItem(Items.DIAMOND) == 0
                && player.getInventory().countItem(Items.GOLD_INGOT) == 0 && player.getInventory().countItem(Items.APPLE) == 0
                && cloud.getContainer().getItem(0).getCount() + cloud.getContainer().getItem(1).getCount() + cloud.getContainer().getItem(2).getCount() == 19) {
            phase = 105;
        }
    }

    @SubscribeEvent
    public static void hoveredRender(ScreenEvent.Render.Pre event) {
        if (!Boolean.getBoolean("cloudstorage_reborn.fluidSmokeTest") || phase != 2 && phase != 4
                || !(event.getScreen() instanceof CloudChestScreen screen)) return;
        int hovered = phase == 2 ? screen.getDisplayedEntries().size()
                : find(screen, entry -> entry.isFluid() && entry.fluid().getFluid() == Fluids.WATER);
        screen.render(event.getGuiGraphics(), (screen.width - 195) / 2 + 16 + hovered % 9 * 18,
                (screen.height - 222) / 2 + 26 + hovered / 9 * 18, event.getPartialTick());
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
        if (phase == 2 && menu.getFluidCapacity() == 90000 && menu.getTerminalEntries().size() == 4 && menu.getCarried().is(Items.WATER_BUCKET) && ++frames > 20) {
            var tooltip = screen.getContainerTooltip(null);
            if (tooltip.size() != 2 || !tooltip.get(0).getString().contains(menu.getCarried().getHoverName().getString())
                    || !tooltip.get(1).getString().contains(new FluidStack(Fluids.WATER, 1000).getHoverName().getString()))
                throw new AssertionError("Carried-container tooltip did not distinguish the item from its fluid");
            capture(client, "cloud-mixed-terminal.png");
            phase = 3;
            frames = 0;
            click(screen, screen.getDisplayedEntries().size(), 1);
        } else if (phase == 4 && water(menu) == 7000 && menu.getCarried().is(Items.BUCKET) && ++frames > 20) {
            var waterEntry = screen.getDisplayedEntries().get(find(screen, entry -> entry.isFluid() && entry.fluid().getFluid() == Fluids.WATER));
            if (screen.getContainerTooltip(waterEntry).size() != 1 || !screen.getContainerTooltip(null).isEmpty())
                throw new AssertionError("Empty-container tooltip did not update after depositing");
            capture(client, "cloud-mixed-deposit.png");
            frames = 0;
            phase = 5;
            click(screen, find(screen, entry -> entry.isFluid() && entry.fluid().getFluid() == Fluids.WATER), 0);
        } else if (phase == 6 && water(menu) == 6000 && menu.getCarried().is(Items.BUCKET) && menu.getCarried().getCount() == 2) {
            phase = 7;
            click(screen, find(screen, entry -> entry.isFluid() && entry.fluid().getFluid() == Fluids.LAVA), 0);
        } else if (phase == 8 && menu.getCarried().isEmpty() && menu.getTerminalEntries().stream().filter(CloudTerminalEntry::isFluid).mapToLong(CloudTerminalEntry::amount).sum() == 7500 && ++frames > 20) {
            frames = 0;
            screen.keyPressed(340, 0, 1);
            locked = List.copyOf(screen.getDisplayedEntries());
            phase = 9;
            click(screen, find(screen, entry -> entry.item().is(Items.DIAMOND)), 0);
        } else if (phase == 90 && menu.getCarried().is(Items.APPLE) && water(menu) == 7000
                && menu.getTerminalEntries().stream().anyMatch(entry -> entry.isFluid() && FluidStack.isSameFluidSameComponents(entry.fluid(), shiftFluid(500)))) {
            phase = 10;
            click(screen, screen.getDisplayedEntries().size(), 0);
        } else if (phase == 10 && water(menu) == 7000 && menu.getTerminalEntries().stream().anyMatch(entry -> entry.item().is(Items.APPLE)) && ++frames > 20) {
            var view = screen.getDisplayedEntries();
            if (view.size() != locked.size() + 2) throw new AssertionError("Shift hid or duplicated a newly deposited item or fluid");
            assertLockedPrefix(view);
            if (view.stream().noneMatch(entry -> entry.item().is(Items.DIAMOND) && entry.amount() == 0)
                    || view.subList(locked.size(), view.size()).stream().noneMatch(entry -> entry.item().is(Items.APPLE) && entry.amount() == 4)
                    || view.subList(locked.size(), view.size()).stream().noneMatch(entry -> entry.isFluid()
                        && FluidStack.isSameFluidSameComponents(entry.fluid(), shiftFluid(500)) && entry.amount() == 500)
                    || view.stream().noneMatch(entry -> entry.isFluid() && entry.fluid().getFluid() == Fluids.WATER && entry.amount() == 7000))
                throw new AssertionError("Shift did not retain depleted positions, append new entries, or update counts");
            capture(client, "cloud-mixed-shift-locked.png");
            locked = List.copyOf(view);
            frames = 0;
            phase = 91;
            click(screen, find(screen, entry -> entry.item().is(Items.APPLE)), 0);
        } else if (phase == 92 && menu.getCarried().is(Items.APPLE)
                && menu.getTerminalEntries().stream().anyMatch(entry -> entry.isFluid()
                    && FluidStack.isSameFluidSameComponents(entry.fluid(), shiftFluid(1000)) && entry.amount() == 1000)) {
            var view = screen.getDisplayedEntries();
            assertLockedPrefix(view);
            if (view.size() != locked.size() || view.stream().noneMatch(entry -> entry.item().is(Items.APPLE) && entry.amount() == 0))
                throw new AssertionError("New Shift entry was not accessible or lost its position after withdrawal");
            phase = 93;
            click(screen, screen.getDisplayedEntries().size(), 0);
        } else if (phase == 93 && menu.getCarried().isEmpty() && menu.getTerminalEntries().stream().anyMatch(entry -> entry.item().is(Items.APPLE)) && ++frames > 20) {
            var view = screen.getDisplayedEntries();
            assertLockedPrefix(view);
            if (view.size() != locked.size() || view.stream().noneMatch(entry -> entry.item().is(Items.APPLE) && entry.amount() == 4))
                throw new AssertionError("Restocking a new Shift entry duplicated it or changed its position");
            frames = 0;
            screen.keyReleased(340, 0, 0);
            phase = 11;
        } else if (phase == 11 && ++frames > 20) {
            if (screen.getDisplayedEntries().stream().noneMatch(entry -> entry.item().is(Items.APPLE))
                    || screen.getDisplayedEntries().stream().anyMatch(entry -> entry.item().is(Items.DIAMOND)))
                throw new AssertionError("Releasing Shift did not update the mixed list");
            capture(client, "cloud-mixed-shift-released.png");
            search(screen, "@minecraft");
            phase = 12;
            frames = 0;
        } else if (phase == 12 && ++frames > 10) {
            if (screen.getDisplayedEntries().size() != 5 || screen.getDisplayedEntries().stream().noneMatch(CloudTerminalEntry::isFluid)
                    || screen.getDisplayedEntries().stream().allMatch(CloudTerminalEntry::isFluid)) throw new AssertionError("Mixed mod-ID search excluded a resource type");
            search(screen, "#minecraft:water");
            screen.render(new net.minecraft.client.gui.GuiGraphics(client, client.renderBuffers().bufferSource()), 0, 0, 0);
            if (screen.getDisplayedEntries().size() != 1 || !screen.getDisplayedEntries().getFirst().isFluid()) throw new AssertionError("Fluid tag search failed");
            search(screen, "");
            phase = 13;
            frames = 0;
            click(screen, find(screen, entry -> entry.item().is(Items.APPLE)), 0);
        } else if (phase == 14 && menu.getCarried().is(Items.APPLE)) {
            phase = 15;
            click(screen, screen.getDisplayedEntries().size(), 0);
        } else if (phase == 16 && menu.getTerminalEntries().size() == 65 && ++frames > 20) {
            screen.mouseScrolled(left + 50, top + 30, 0, -1);
            if (screen.getScrollRow() != 1) throw new AssertionError("Mixed terminal mouse wheel failed");
            screen.mouseClicked(left + 180, top + 120, 0);
            screen.mouseDragged(left + 180, top + 122, 0, 0, 2);
            screen.mouseReleased(left + 180, top + 122, 0);
            phase = 17;
            frames = 0;
        } else if (phase == 17 && ++frames > 20) {
            capture(client, "cloud-mixed-scroll.png");
            CloudStorage.LOGGER.info("FLUID_CLIENT_SMOKE_TEST_PASSED: mixed items/fluids, actual transfers, immediate Shift additions and restocking, unified search and scrolling");
            if (Boolean.getBoolean("cloudstorage_reborn.mouseTweaksSmokeTest")) {
                frames = 0;
                while (screen.getScrollRow() > 0) screen.mouseScrolled(left + 50, top + 30, 0, 1);
                phase = 100;
                return;
            }
            phase = 18;
            client.stop();
        } else if (phase == 101 && menu.getCarried().isEmpty()
                && menu.getTerminalEntries().stream().anyMatch(entry -> entry.item().is(Items.DIAMOND)) && ++frames > 20) {
            screen.keyPressed(340, 0, 1);
            locked = List.copyOf(screen.getDisplayedEntries());
            int first = find(screen, entry -> entry.item().is(Items.DIAMOND));
            mouseTweaksButtonClass = Class.forName("yalter.mousetweaks.MouseButton");
            mouseTweaksLeft = mouseTweaksButtonClass.getField("LEFT").get(null);
            Class<?> main = Class.forName("yalter.mousetweaks.Main");
            main.getMethod("onMouseClicked", Screen.class, double.class, double.class, mouseTweaksButtonClass)
                    .invoke(null, screen, (double) left + 16 + first % 9 * 18, (double) top + 26 + first / 9 * 18, mouseTweaksLeft);
            var handlerField = main.getDeclaredField("handler");
            handlerField.setAccessible(true);
            mouseTweaksHandler = handlerField.get(null);
            if (mouseTweaksHandler == null || !mouseTweaksHandler.getClass().getSimpleName().equals("IMTModGuiContainer3ExHandler"))
                throw new AssertionError("Mouse Tweaks did not select the optional custom screen interface");
            // The initial press goes through the screen; Mouse Tweaks handles subsequent crossed slots.
            screen.mouseClicked(left + 16 + first % 9 * 18, top + 26 + first / 9 * 18, 0);
            // Use Mouse Tweaks' actual custom handler, including coordinate lookup, for a burst without waiting for snapshots.
            for (var item : List.of(Items.DIAMOND, Items.GOLD_INGOT, Items.APPLE)) {
                int index = find(screen, entry -> entry.item().is(item));
                Slot slot = mouseTweaksSlot(left + 16 + index % 9 * 18, top + 26 + index / 9 * 18);
                if (slot == null || !slot.getItem().is(item)) throw new AssertionError("Mouse Tweaks mapped " + item
                        + " at displayed index " + index + ", scroll row " + screen.getScrollRow() + " to "
                        + (slot == null ? "null" : slot.getClass().getName() + ": " + slot.getItem())
                        + "; displayed entry: " + screen.getDisplayedEntries().get(index));
                mouseTweaksTransfer(slot);
                if (item == Items.DIAMOND) mouseTweaksTransfer(slot);
            }
            int fluid = find(screen, CloudTerminalEntry::isFluid);
            Slot fluidSlot = mouseTweaksSlot(left + 16 + fluid % 9 * 18, top + 26 + fluid / 9 * 18);
            if (!(boolean) mouseTweaksHandler.getClass().getMethod("isIgnored", Slot.class).invoke(mouseTweaksHandler, fluidSlot))
                throw new AssertionError("Mouse Tweaks treated a fluid as a movable item");
            mouseTweaksTransfer(fluidSlot);
            frames = 0;
            phase = 102;
        } else if (phase == 103 && menu.getTerminalEntries().stream().noneMatch(entry -> !entry.isFluid()) && ++frames > 20) {
            if (!menu.getCarried().isEmpty()) throw new AssertionError("Mouse Tweaks quick move changed the cursor stack");
            for (var item : List.of(Items.DIAMOND, Items.GOLD_INGOT, Items.APPLE)) {
                Slot inventorySlot = menu.slots.stream().filter(slot -> !(slot instanceof SlotCloudChest) && slot.getItem().is(item)).findFirst().orElseThrow();
                Slot found = mouseTweaksSlot(left + inventorySlot.x + 8, top + inventorySlot.y + 8);
                if (found != inventorySlot) throw new AssertionError("Mouse Tweaks could not resolve the player inventory");
                mouseTweaksTransfer(found);
            }
            frames = 0;
            phase = 104;
        } else if (phase == 105 && menu.getTerminalEntries().stream().filter(entry -> !entry.isFluid()).count() == 3 && ++frames > 20) {
            if (menu.getTerminalEntries().stream().filter(entry -> !entry.isFluid()).mapToLong(CloudTerminalEntry::amount).sum() != 19)
                throw new AssertionError("Mouse Tweaks return transfers lost or duplicated items");
            var view = screen.getDisplayedEntries();
            assertLockedPrefix(view);
            if (view.size() != locked.size() || view.stream().anyMatch(entry -> entry.amount() == 0))
                throw new AssertionError("Repeated Shift transfers accumulated stale zero entries");
            if (++mouseTweaksCycles < 8) {
                frames = 0;
                phase = 106;
                for (var item : List.of(Items.DIAMOND, Items.GOLD_INGOT, Items.APPLE)) {
                    int index = find(screen, entry -> entry.item().is(item));
                    mouseTweaksTransfer(mouseTweaksSlot(left + 16 + index % 9 * 18, top + 26 + index / 9 * 18));
                }
                return;
            }
            capture(client, "cloud-mousetweaks-quick-move.png");
            CloudStorage.LOGGER.info("MOUSE_TWEAKS_CLIENT_SMOKE_TEST_PASSED: actual custom handler, virtual coordinates, eight Shift withdrawal/deposit cycles without stale placeholders, duplicate protection and ignored fluids");
            client.stop();
        }
    }

    private static void assertLockedPrefix(List<CloudTerminalEntry> view) {
        if (view.size() < locked.size()) throw new AssertionError("Shift removed a locked position");
        for (int i = 0; i < locked.size(); i++) {
            var a = locked.get(i); var b = view.get(i);
            if (a.isFluid() != b.isFluid() || (a.isFluid() ? !FluidStack.isSameFluidSameComponents(a.fluid(), b.fluid())
                    : !ItemStack.isSameItemSameComponents(a.item(), b.item())))
                throw new AssertionError("Shift reordered mixed entries");
        }
    }
    private static Slot mouseTweaksSlot(double x, double y) throws ReflectiveOperationException {
        return (Slot) mouseTweaksHandler.getClass().getMethod("getSlotUnderMouse", double.class, double.class).invoke(mouseTweaksHandler, x, y);
    }
    private static void mouseTweaksTransfer(Slot slot) throws ReflectiveOperationException {
        mouseTweaksHandler.getClass().getMethod("clickSlot", Slot.class, mouseTweaksButtonClass, boolean.class)
                .invoke(mouseTweaksHandler, slot, mouseTweaksLeft, true);
    }
    private static FluidStack shiftFluid(int amount) {
        var fluid = new FluidStack(Fluids.LAVA, amount);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Shift-added lava"));
        return fluid;
    }
    private static long water(CloudChestMenu menu) {
        return menu.getTerminalEntries().stream().filter(entry -> entry.isFluid() && entry.fluid().getFluid() == Fluids.WATER).findFirst().orElseThrow().amount();
    }
    private static int find(CloudChestScreen screen, Predicate<CloudTerminalEntry> predicate) {
        var entries = screen.getDisplayedEntries();
        for (int i = 0; i < entries.size(); i++) if (predicate.test(entries.get(i))) return i;
        throw new AssertionError("Missing displayed entry");
    }
    private static void click(CloudChestScreen screen, int index, int button) {
        double x = (screen.width - 195) / 2 + 16 + index % 9 * 18;
        double y = (screen.height - 222) / 2 + 26 + index / 9 * 18;
        screen.mouseClicked(x, y, button);
        screen.mouseReleased(x, y, button);
    }
    private static void search(CloudChestScreen screen, String query) {
        screen.mouseClicked((screen.width - 195) / 2 + 90, (screen.height - 222) / 2 + 8, 0);
        screen.keyPressed(269, 0, 0);
        for (int i = 0; i < 50; i++) screen.keyPressed(259, 0, 0);
        for (char c : query.toCharArray()) screen.charTyped(c, 0);
    }
    private static void capture(Minecraft client, String filename) throws Exception {
        var output = client.gameDirectory.toPath().resolve("screenshots/" + filename);
        Files.createDirectories(output.getParent());
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) { image.writeToFile(output); }
    }
}
