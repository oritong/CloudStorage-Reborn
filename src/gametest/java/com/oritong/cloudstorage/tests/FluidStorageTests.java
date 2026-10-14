package com.oritong.cloudstorage.tests;

import com.mojang.authlib.GameProfile;
import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.compat.BeyondDimensionsCompat;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.CloudFluidContainerInteraction;
import com.oritong.cloudstorage.inventory.CloudFluidStorage;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.network.MessageCloudTerminalAction;
import com.oritong.cloudstorage.network.MessageCloudTerminalContents;
import com.oritong.cloudstorage.network.MessageCloudQuickMove;
import com.oritong.cloudstorage.inventory.CloudTerminalEntry;
import com.oritong.cloudstorage.inventory.CloudTerminalOrder;
import com.oritong.cloudstorage.world.CSWorldData;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.List;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

@GameTestHolder("cloudstorage_reborn")
@PrefixGameTestTemplate(false)
public final class FluidStorageTests {
    private static Player player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "FluidTester"));
    }

    @GameTest(template = "empty")
    public static void persistenceCapacityAndIsolation(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var data = CSWorldData.get(helper.getLevel());
        UUID owner = UUID.randomUUID();
        var cloud = new CloudIndex(owner, 0x934522, 27);
        var fluid = new FluidStack(Fluids.WATER, 25000);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Distinct water"));
        helper.assertTrue(cloud.getFluids().capacity() == 0 && cloud.getFluids().typeCapacity() == 0
                && cloud.getFluids().fill(fluid, EXECUTE) == 0, "A chest granted fluid capacity without a tank");
        cloud.addFluidStorage(25000);
        helper.assertTrue(cloud.getFluids().fill(fluid, SIMULATE) == 25000 && cloud.getFluids().storedAmount() == 0, "Simulation mutated cloud fluids");
        cloud.getFluids().fill(fluid, EXECUTE);
        cloud.getFluids().drain(fluid.copyWithAmount(1000), EXECUTE);
        helper.assertTrue(cloud.getFluids().fill(new FluidStack(Fluids.LAVA, 1000), EXECUTE) == 0, "One uploaded tank accepted a second fluid type");
        cloud.getFluids().fill(fluid.copyWithAmount(1000), EXECUTE);
        cloud.addFluidStorage(2000);
        helper.assertTrue(cloud.getFluids().fill(new FluidStack(Fluids.LAVA, 3000), EXECUTE) == 2000, "Fluids did not share the total capacity");
        cloud.resize(54);
        helper.assertTrue(cloud.getFluids().capacity() == 27000 && cloud.getFluids().typeCapacity() == 2
                && cloud.getFluids().storedAmount() == 27000, "Item expansion changed uploaded tank capacity");
        cloud.addFluidStorage(27000);
        data.addPrivateCloud(cloud);
        var other = new CloudIndex(UUID.randomUUID(), cloud.getBalloonColor(), 27);
        data.addPrivateCloud(other);
        var color = new CloudIndex(owner, 0x934523, 27);
        data.addPrivateCloud(color);
        var shared = new CloudIndex(owner, cloud.getBalloonColor(), 27);
        shared.addFluidStorage(4000);
        shared.getFluids().fill(new FluidStack(Fluids.LAVA, 4000), EXECUTE);
        data.addPublicCloud(shared);
        var saved = data.save(new CompoundTag(), registries);
        var restored = CSWorldData.load(saved, registries);
        var storage = restored.getPrivateCloud(owner, cloud.getBalloonColor()).getFluids();
        helper.assertTrue(storage.storedAmount() == 27000 && storage.capacity() == 54000 && storage.typeCapacity() == 3
                && FluidStack.isSameFluidSameComponents(storage.getFluidInTank(0), fluid), "Saved tank capacity, type quota or contents changed");
        helper.assertTrue(restored.getPrivateCloud(other.getPlayerUUID(), other.getBalloonColor()).getFluids().storedAmount() == 0
                && restored.getPrivateCloud(owner, color.getBalloonColor()).getFluids().storedAmount() == 0, "Private owners or colors shared fluids");
        helper.assertTrue(restored.getPublicCloud(shared.getBalloonColor()).getFluids().storedAmount() == 4000, "Public cloud fluid was mixed with private storage");
        cloud.resize(1);
        helper.assertTrue(cloud.getFluids().storedAmount() == 27000 && cloud.getFluids().capacity() == 54000
                && cloud.getFluids().fill(fluid, SIMULATE) == 25000, "Item resizing changed fluid storage");
        CompoundTag legacy = new CompoundTag();
        cloud.writeToNBT(legacy, registries);
        legacy.remove("Fluids");
        helper.assertTrue(new CloudIndex(legacy, registries).getFluids().storedAmount() == 0, "Existing worlds without fluid data failed to load");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bucketsAndStackedContainers(GameTestHelper helper) {
        var player = player(helper);
        var menu = new CloudChestMenu(12, player.getInventory(), new net.minecraft.world.SimpleContainer(27));
        var storage = new CloudFluidStorage(() -> 1500);
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(CloudFluidContainerInteraction.interact(player, menu, storage, FluidStack.EMPTY, true), "Water bucket did not empty");
        helper.assertTrue(menu.getCarried().is(Items.BUCKET) && storage.storedAmount() == 1000, "Deposit lost or duplicated a bucket");
        menu.setCarried(new ItemStack(Items.LAVA_BUCKET));
        helper.assertTrue(!CloudFluidContainerInteraction.interact(player, menu, storage, FluidStack.EMPTY, true)
                && menu.getCarried().is(Items.LAVA_BUCKET) && storage.storedAmount() == 1000, "Indivisible bucket was consumed with only 500 mB free");
        storage.fill(new FluidStack(Fluids.LAVA, 500), EXECUTE);
        menu.setCarried(new ItemStack(Items.BUCKET));
        helper.assertTrue(!CloudFluidContainerInteraction.interact(player, menu, storage, new FluidStack(Fluids.LAVA, Integer.MAX_VALUE), false)
                && menu.getCarried().is(Items.BUCKET) && storage.storedAmount() == 1500, "Bucket withdrew less than a full bucket");
        menu.setCarried(new ItemStack(Items.BUCKET, 3));
        helper.assertTrue(CloudFluidContainerInteraction.interact(player, menu, storage, new FluidStack(Fluids.WATER, Integer.MAX_VALUE), false), "Stacked buckets did not fill");
        helper.assertTrue(menu.getCarried().is(Items.BUCKET) && menu.getCarried().getCount() == 2
                && player.getInventory().countItem(Items.WATER_BUCKET) == 1 && storage.storedAmount() == 500, "Stacked operation did not return exactly one filled bucket");
        player.getAbilities().instabuild = true;
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(CloudFluidContainerInteraction.interact(player, menu, storage, FluidStack.EMPTY, true)
                && menu.getCarried().is(Items.BUCKET) && storage.storedAmount() == 1500, "Creative operation created a reusable full bucket");
        menu.setCarried(new ItemStack(Items.DIAMOND));
        helper.assertTrue(!CloudFluidContainerInteraction.interact(player, menu, storage, FluidStack.EMPTY, true)
                && storage.storedAmount() == 1500 && menu.getCarried().is(Items.DIAMOND), "Unsupported item changed storage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void reusableContainerAndComponents(GameTestHelper helper) {
        var player = player(helper);
        var menu = new CloudChestMenu(13, player.getInventory(), new net.minecraft.world.SimpleContainer(27));
        var storage = new CloudFluidStorage(() -> 500);
        ItemStack container = testContainer();
        container.set(DataComponents.CUSTOM_NAME, Component.literal("Reusable tank"));
        FluidStack water = new FluidStack(Fluids.WATER, 1500);
        water.set(DataComponents.CUSTOM_NAME, Component.literal("Tagged water"));
        FluidUtil.getFluidHandler(container).orElseThrow().fill(water, EXECUTE);
        menu.setCarried(container);
        helper.assertTrue(CloudFluidContainerInteraction.interact(player, menu, storage, FluidStack.EMPTY, true), "Reusable fluid container did not partly empty");
        var remaining = FluidUtil.getFluidContained(menu.getCarried()).orElseThrow();
        helper.assertTrue(remaining.getAmount() == 1000 && storage.storedAmount() == 500
                && FluidStack.isSameFluidSameComponents(remaining, storage.getFluidInTank(0))
                && menu.getCarried().getHoverName().getString().equals("Reusable tank"), "Partial transfer changed container or fluid components");
        helper.assertTrue(CloudFluidContainerInteraction.interact(player, menu, storage, water, false), "Reusable tank did not refill from selected fluid");
        helper.assertTrue(FluidUtil.getFluidContained(menu.getCarried()).orElseThrow().getAmount() == 1500 && storage.storedAmount() == 0, "Partial withdrawal lost fluid");
        var mixed = new CloudFluidStorage(() -> 5000);
        mixed.fill(new FluidStack(Fluids.LAVA, 1000), EXECUTE);
        mixed.fill(water, EXECUTE);
        menu.setCarried(testContainer());
        helper.assertTrue(CloudFluidContainerInteraction.interact(player, menu, mixed, water, false), "Selected component-bearing fluid did not fill");
        helper.assertTrue(FluidStack.isSameFluidSameComponents(FluidUtil.getFluidContained(menu.getCarried()).orElseThrow(), water)
                && mixed.getFluidInTank(0).getFluid() == Fluids.LAVA && mixed.storedAmount() == 1000, "Withdrawal took the first fluid instead of the selected fluid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void menuValidationAndSnapshotSync(GameTestHelper helper) {
        var player = player(helper);
        var cloud = new CloudIndex(player.getUUID(), 0x451279, 27);
        cloud.addFluidStorage(27000);
        var menu = new CloudChestMenu(14, player.getInventory(), cloud, CSWorldData.get(helper.getLevel()));
        player.containerMenu = menu;
        cloud.getContainer().setItem(10, new ItemStack(Items.DIAMOND, 6));
        menu.broadcastChanges();
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(15, menu.getTerminalRevision(), -1, 1, false));
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, 90, -1, 1, false));
        helper.assertTrue(cloud.getFluids().storedAmount() == 0, "Wrong menu or stale fluid snapshot was accepted");
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, menu.getTerminalRevision(), -1, 1, false));
        helper.assertTrue(cloud.getFluids().storedAmount() == 1000 && menu.getCarried().is(Items.BUCKET), "Valid menu deposit failed");
        helper.assertTrue(menu.getTerminalEntries().size() == 2 && !menu.getTerminalEntries().getFirst().isFluid()
                && menu.getTerminalEntries().getLast().isFluid(), "Items and fluids were not available in one snapshot");
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, menu.getTerminalRevision(), -2, 0, false));
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, menu.getTerminalRevision(), 256, 0, false));
        helper.assertTrue(cloud.getFluids().storedAmount() == 1000, "Out-of-range fluid index changed storage");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(cloud.getContainer().getItem(10).getCount() == 6 && !menu.slots.getFirst().mayPlace(new ItemStack(Items.APPLE))
                && !menu.slots.getFirst().mayPickup(player), "Hidden backing slots bypassed mixed terminal validation");
        menu.setCarried(ItemStack.EMPTY);
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, menu.getTerminalRevision(), 0, 1, false));
        helper.assertTrue(menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 3
                && cloud.getContainer().getItem(10).getCount() == 3, "Mixed terminal half-stack pickup failed");
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, menu.getTerminalRevision(), 0, 0, false));
        helper.assertTrue(menu.getCarried().isEmpty() && cloud.getContainer().getItem(10).getCount() == 6, "Mixed terminal item insertion failed");
        long stale = menu.getTerminalRevision();
        cloud.getContainer().setItem(1, new ItemStack(Items.APPLE, 4));
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, stale, 0, 0, true));
        helper.assertTrue(cloud.getContainer().getItem(1).getCount() == 4 && cloud.getContainer().getItem(10).getCount() == 6, "Changed source ordering redirected a stale click");
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, menu.getTerminalRevision(), 1, 0, true));
        helper.assertTrue(cloud.getContainer().getItem(10).isEmpty() && player.getInventory().countItem(Items.DIAMOND) == 6, "Mixed terminal Shift transfer failed");
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, menu.getTerminalRevision(), -1, 0, false));
        helper.assertTrue(menu.getCarried().isEmpty() && cloud.getContainer().getItem(0).is(Items.WATER_BUCKET)
                && cloud.getFluids().storedAmount() == 1000, "Left-click stored the fluid instead of the container item");
        menu.setCarried(new ItemStack(Items.ENDER_PEARL, 40));
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, menu.getTerminalRevision(), -1, 0, true));
        helper.assertTrue(menu.getCarried().isEmpty() && cloud.getContainer().getItem(2).getCount() == 16
                && cloud.getContainer().getItem(3).getCount() == 16 && cloud.getContainer().getItem(4).getCount() == 8,
                "Mixed insertion ignored item stack limits or stopped at the first empty slot");
        menu.setCarried(new ItemStack(Items.BUCKET));
        player.containerMenu = player.inventoryMenu;
        menu.handleTerminalAction(player, new MessageCloudTerminalAction(14, menu.getTerminalRevision(), 1, 0, false));
        helper.assertTrue(cloud.getFluids().storedAmount() == 1000, "Closed menu still allowed withdrawal");
        var clientMenu = new CloudChestMenu(14, player.getInventory(), new net.minecraft.world.SimpleContainer(90));
        var large = new CloudFluidStorage(() -> 9_000_000_000L, () -> 301, () -> true);
        large.fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), EXECUTE);
        large.fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), EXECUTE);
        for (int i = 0; i < 300; i++) {
            var fluid = new FluidStack(Fluids.LAVA, i + 1);
            fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Fluid " + i));
            large.fill(fluid, EXECUTE);
        }
        var entries = new java.util.ArrayList<CloudTerminalEntry>();
        entries.add(CloudTerminalEntry.item(80, new ItemStack(Items.DIAMOND, 8)));
        for (int i = 0; i < large.snapshot().size(); i++) entries.add(CloudTerminalEntry.fluid(i, large.snapshot().get(i)));
        for (int i = 0; i < 20; i++) entries.add(CloudTerminalEntry.item(i, new ItemStack(Items.GOLD_INGOT, 2)));
        for (int offset = 0; offset < entries.size(); offset += MessageCloudTerminalContents.BATCH_SIZE) {
            var packet = new MessageCloudTerminalContents(14, large.revision(), 90, large.capacity(), large.typeCapacity(), offset, entries.size(),
                    entries.subList(offset, Math.min(entries.size(), offset + MessageCloudTerminalContents.BATCH_SIZE)));
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                MessageCloudTerminalContents.CODEC.encode(buf, packet);
                clientMenu.receiveTerminal(MessageCloudTerminalContents.CODEC.decode(buf));
            } finally { buf.release(); }
            if (offset == 0) helper.assertTrue(clientMenu.getTerminalEntries().isEmpty(), "Incomplete snapshot exposed partial mixed ordering");
        }
        helper.assertTrue(clientMenu.getTerminalEntries().size() == 322 && clientMenu.getTerminalEntries().get(1).amount() == 4_294_967_294L
                && clientMenu.getFluidCapacity() == 9_000_000_000L && clientMenu.getFluidTypeCapacity() == 301
                && ItemStack.matches(clientMenu.getTerminalEntries().getFirst().item(), entries.getFirst().item())
                && FluidStack.isSameFluidSameComponents(clientMenu.getTerminalEntries().get(301).fluid(), entries.get(301).fluid()), "Batched mixed snapshot lost long counts, uploaded type quotas or components");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedTerminalRestocking(GameTestHelper helper) {
        var namedApple = new ItemStack(Items.APPLE, 3);
        namedApple.set(DataComponents.CUSTOM_NAME, Component.literal("Distinct apple"));
        var water = new CloudTerminalEntry(0, ItemStack.EMPTY, new FluidStack(Fluids.WATER, 1000), 1000);
        var lava = new CloudTerminalEntry(1, ItemStack.EMPTY, new FluidStack(Fluids.LAVA, 500), 500);
        var initial = List.of(CloudTerminalEntry.item(80, new ItemStack(Items.APPLE, 4)),
                CloudTerminalEntry.item(60, new ItemStack(Items.APPLE, 9)), CloudTerminalEntry.item(40, namedApple),
                CloudTerminalEntry.item(30, new ItemStack(Items.GOLD_INGOT, 6)), water, lava);
        var remaining = List.of(initial.get(1), initial.get(2), initial.get(3), lava, water);
        var view = CloudTerminalOrder.refreshLocked(initial, remaining, entry -> true);
        helper.assertTrue(view.size() == initial.size() && view.get(0).amount() == 0
                && view.get(1).source() == 60 && view.get(1).amount() == 9,
                "A depleted position claimed another live stack of the same item");
        var returned = List.of(CloudTerminalEntry.item(0, new ItemStack(Items.APPLE, 4)),
                initial.get(1), initial.get(2), initial.get(3), lava, water);
        view = CloudTerminalOrder.refreshLocked(view, returned, entry -> true);
        helper.assertTrue(view.size() == initial.size() && view.get(0).source() == 0 && view.get(0).amount() == 4
                && view.get(1).source() == 60 && view.get(1).amount() == 9,
                "Returning an item to a different source appended a stale duplicate");
        for (int cycle = 0; cycle < 30; cycle++) {
            view = CloudTerminalOrder.refreshLocked(view, List.of(lava, water), entry -> true);
            helper.assertTrue(view.subList(0, 4).stream().allMatch(entry -> entry.amount() == 0), "Withdrawal left a live item in the display");
            int base = cycle % 10 * 4;
            var current = List.of(CloudTerminalEntry.item(base, new ItemStack(Items.APPLE, 4)),
                    CloudTerminalEntry.item(base + 1, new ItemStack(Items.APPLE, 9)), CloudTerminalEntry.item(base + 2, namedApple),
                    CloudTerminalEntry.item(base + 3, new ItemStack(Items.GOLD_INGOT, 6)), lava, water);
            view = CloudTerminalOrder.refreshLocked(view, current, entry -> true);
            helper.assertTrue(view.size() == initial.size() && view.stream().noneMatch(entry -> entry.amount() == 0)
                    && view.stream().filter(entry -> !entry.isFluid()).mapToLong(CloudTerminalEntry::amount).sum() == 22
                    && view.get(0).source() == base && view.get(1).source() == base + 1
                    && ItemStack.isSameItemSameComponents(view.get(2).item(), namedApple)
                    && view.get(4).fluid().getFluid() == Fluids.WATER && view.get(5).fluid().getFluid() == Fluids.LAVA,
                    "Repeated restocking grew the list, duplicated a stack, or reordered resources");
        }
        var distinct = CloudTerminalOrder.refreshLocked(List.of(initial.getFirst()),
                List.of(CloudTerminalEntry.item(80, namedApple), initial.get(3)), entry -> entry.item().is(Items.APPLE));
        helper.assertTrue(distinct.size() == 2 && distinct.getFirst().amount() == 0
                && ItemStack.isSameItemSameComponents(distinct.get(1).item(), namedApple),
                "Restocking merged different data components or appended an item excluded by search");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rapidQuickMoveValidation(GameTestHelper helper) {
        var player = player(helper);
        var cloud = new CloudIndex(player.getUUID(), 0x193845, 90);
        var menu = new CloudChestMenu(17, player.getInventory(), cloud, CSWorldData.get(helper.getLevel()));
        player.containerMenu = menu;
        cloud.getContainer().setItem(80, new ItemStack(Items.DIAMOND, 6));
        cloud.getContainer().setItem(60, new ItemStack(Items.GOLD_INGOT, 9));
        menu.broadcastChanges();
        var diamond = new MessageCloudQuickMove(17, 80, new ItemStack(Items.DIAMOND, 6));
        var gold = new MessageCloudQuickMove(17, 60, new ItemStack(Items.GOLD_INGOT, 9));
        menu.handleQuickMove(player, new MessageCloudQuickMove(18, 80, diamond.expected()));
        menu.handleQuickMove(player, new MessageCloudQuickMove(17, -1, diamond.expected()));
        menu.handleQuickMove(player, new MessageCloudQuickMove(17, 90, diamond.expected()));
        var renamed = diamond.expected().copy();
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Different components"));
        menu.handleQuickMove(player, new MessageCloudQuickMove(17, 80, renamed));
        menu.setCarried(new ItemStack(Items.BUCKET));
        menu.handleQuickMove(player, diamond);
        menu.setCarried(ItemStack.EMPTY);
        helper.assertTrue(cloud.getContainer().getItem(80).getCount() == 6 && cloud.getContainer().getItem(60).getCount() == 9,
                "Invalid drag request changed storage");
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            MessageCloudQuickMove.CODEC.encode(buf, diamond);
            menu.handleQuickMove(player, MessageCloudQuickMove.CODEC.decode(buf));
        } finally { buf.release(); }
        // Both requests were created from the same snapshot. The first must not invalidate the second.
        menu.handleQuickMove(player, gold);
        menu.handleQuickMove(player, diamond);
        helper.assertTrue(cloud.getContainer().getItem(80).isEmpty() && cloud.getContainer().getItem(60).isEmpty()
                && player.getInventory().countItem(Items.DIAMOND) == 6 && player.getInventory().countItem(Items.GOLD_INGOT) == 9,
                "Rapid transfers were dropped, redirected, or duplicated");
        cloud.getContainer().setItem(80, new ItemStack(Items.APPLE, 4));
        menu.handleQuickMove(player, diamond);
        var apple = new MessageCloudQuickMove(17, 80, new ItemStack(Items.APPLE, 4));
        menu.handleQuickMove(player(helper), apple);
        player.containerMenu = player.inventoryMenu;
        menu.handleQuickMove(player, apple);
        helper.assertTrue(cloud.getContainer().getItem(80).getCount() == 4, "Stale contents, another player, or a closed menu allowed a transfer");
        player.containerMenu = menu;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(8, new ItemStack(Items.APPLE, 63));
        menu.handleQuickMove(player, apple);
        helper.assertTrue(cloud.getContainer().getItem(80).getCount() == 3 && player.getInventory().getItem(8).getCount() == 64,
                "A partial quick move lost its remainder");
        menu.handleQuickMove(player, apple);
        helper.assertTrue(cloud.getContainer().getItem(80).getCount() == 3, "A repeated old drag request changed the remaining stack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void optionalDimensionalFluidTransfer(GameTestHelper helper) throws ReflectiveOperationException {
        var storage = new CloudFluidStorage(() -> 5000);
        storage.fill(new FluidStack(Fluids.LAVA, 2500), EXECUTE);
        var tank = new FluidTank(1500);
        helper.assertTrue(BeyondDimensionsCompat.transferFluids(storage, tank) == 1500 && storage.storedAmount() == 1000
                && BeyondDimensionsCompat.transferFluids(storage, tank) == 0, "Partial dimensional fluid transfer lost or duplicated a remainder");
        if (!ModList.get().isLoaded("beyonddimensions")) { helper.succeed(); return; }
        var player = player(helper);
        var data = CSWorldData.get(helper.getLevel());
        var cloud = new CloudIndex(player.getUUID(), 0x764389, 27);
        cloud.addFluidStorage(1000);
        cloud.getFluids().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
        data.addPrivateCloud(cloud);
        String pkg = "com.wintercogs.beyonddimensions.";
        Class<?> netClass = Class.forName(pkg + "api.dimensionnet.DimensionsNet");
        Object net = netClass.getMethod("createNewNetForPlayer", Player.class, long.class, int.class).invoke(null, player, 500L, 1);
        netClass.getMethod("setPrimaryNetForPlayer", Player.class, netClass).invoke(null, player, net);
        Object external = netClass.getMethod("getUnifiedStorage").invoke(net);
        Class<?> storageClass = Class.forName(pkg + "api.dimensionnet.UnifiedStorage");
        IFluidHandler target = (IFluidHandler) Class.forName(pkg + "api.capability.helper.unordered.FluidUnifiedStorageHandler")
                .getConstructor(storageClass).newInstance(external);
        boolean previous = CloudStorage.CONFIG.beyondDimensionsStorage.get();
        try {
            CloudStorage.CONFIG.beyondDimensionsStorage.set(true);
            helper.assertTrue(BeyondDimensionsCompat.createMenu(15, player.getInventory(), cloud) == null
                    && target.getFluidInTank(0).getAmount() == 500 && cloud.getFluids().storedAmount() == 500, "Fluid remainder did not keep the ordinary cloud menu accessible");
            storageClass.getMethod("setSlotCapacity", long.class).invoke(external, 2000L);
            helper.assertTrue(BeyondDimensionsCompat.createMenu(16, player.getInventory(), cloud) != null
                    && target.getFluidInTank(0).getAmount() == 1000 && cloud.getFluids().storedAmount() == 0, "Real dimensional network fluid transfer failed");
        } finally { CloudStorage.CONFIG.beyondDimensionsStorage.set(previous); }
        helper.succeed();
    }

    private static ItemStack testContainer() {
        ItemStack stack = new ItemStack(Items.STICK);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("CloudFluidTest", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** A development-only reusable 2,000 mB container exercising the standard modded item capability. */
    @EventBusSubscriber(modid = CloudStorage.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class TestContainerCapability {
        @SubscribeEvent
        public static void register(RegisterCapabilitiesEvent event) {
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> {
                var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                return tag.getBoolean("CloudFluidTest") ? new TestTank(stack) : null;
            }, Items.STICK);
        }
    }

    private static final class TestTank extends FluidTank implements IFluidHandlerItem {
        private final ItemStack container;
        private TestTank(ItemStack stack) {
            super(2000);
            container = stack;
            readFromNBT(ServerLifecycleHooks.getCurrentServer().registryAccess(), stack.get(DataComponents.CUSTOM_DATA).copyTag());
        }
        @Override public ItemStack getContainer() { return container; }
        @Override protected void onContentsChanged() {
            CompoundTag tag = container.get(DataComponents.CUSTOM_DATA).copyTag();
            tag.remove("Fluid");
            writeToNBT(ServerLifecycleHooks.getCurrentServer().registryAccess(), tag);
            container.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}
