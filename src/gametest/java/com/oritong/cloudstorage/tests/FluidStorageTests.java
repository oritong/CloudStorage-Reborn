package com.oritong.cloudstorage.tests;

import com.mojang.authlib.GameProfile;
import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.compat.BeyondDimensionsCompat;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.CloudFluidContainerInteraction;
import com.oritong.cloudstorage.inventory.CloudFluidStorage;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.network.MessageCloudFluidAction;
import com.oritong.cloudstorage.network.MessageCloudFluids;
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
        helper.assertTrue(cloud.getFluids().fill(fluid, SIMULATE) == 25000 && cloud.getFluids().storedAmount() == 0, "Simulation mutated cloud fluids");
        cloud.getFluids().fill(fluid, EXECUTE);
        helper.assertTrue(cloud.getFluids().fill(new FluidStack(Fluids.LAVA, 3000), EXECUTE) == 2000, "Fluids did not share the total capacity");
        cloud.resize(54);
        helper.assertTrue(cloud.getFluids().capacity() == 54000 && cloud.getFluids().storedAmount() == 27000, "Expansion lost fluid or failed to update capacity");
        data.addPrivateCloud(cloud);
        var other = new CloudIndex(UUID.randomUUID(), cloud.getBalloonColor(), 27);
        data.addPrivateCloud(other);
        var color = new CloudIndex(owner, 0x934523, 27);
        data.addPrivateCloud(color);
        var shared = new CloudIndex(owner, cloud.getBalloonColor(), 27);
        shared.getFluids().fill(new FluidStack(Fluids.LAVA, 4000), EXECUTE);
        data.addPublicCloud(shared);
        var saved = data.save(new CompoundTag(), registries);
        var restored = CSWorldData.load(saved, registries);
        var storage = restored.getPrivateCloud(owner, cloud.getBalloonColor()).getFluids();
        helper.assertTrue(storage.storedAmount() == 27000 && FluidStack.isSameFluidSameComponents(storage.getFluidInTank(0), fluid), "Saved fluid amount or components changed");
        helper.assertTrue(restored.getPrivateCloud(other.getPlayerUUID(), other.getBalloonColor()).getFluids().storedAmount() == 0
                && restored.getPrivateCloud(owner, color.getBalloonColor()).getFluids().storedAmount() == 0, "Private owners or colors shared fluids");
        helper.assertTrue(restored.getPublicCloud(shared.getBalloonColor()).getFluids().storedAmount() == 4000, "Public cloud fluid was mixed with private storage");
        cloud.resize(1);
        helper.assertTrue(cloud.getFluids().storedAmount() == 27000 && cloud.getFluids().fill(fluid, EXECUTE) == 0, "Lowered capacity deleted saved fluid or allowed more insertion");
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
        var menu = new CloudChestMenu(14, player.getInventory(), cloud, CSWorldData.get(helper.getLevel()));
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.handleFluidAction(player, new MessageCloudFluidAction(14, MessageCloudFluidAction.DEPOSIT, 0, 0));
        helper.assertTrue(cloud.getFluids().storedAmount() == 0, "Item view accepted fluid action");
        menu.handleFluidAction(player, new MessageCloudFluidAction(14, MessageCloudFluidAction.FLUIDS, 0, 0));
        menu.handleFluidAction(player, new MessageCloudFluidAction(15, MessageCloudFluidAction.DEPOSIT, 0, 0));
        menu.handleFluidAction(player, new MessageCloudFluidAction(14, MessageCloudFluidAction.DEPOSIT, 90, 0));
        helper.assertTrue(cloud.getFluids().storedAmount() == 0, "Wrong menu or stale fluid snapshot was accepted");
        menu.handleFluidAction(player, new MessageCloudFluidAction(14, MessageCloudFluidAction.DEPOSIT, 0, 0));
        helper.assertTrue(cloud.getFluids().storedAmount() == 1000 && menu.getCarried().is(Items.BUCKET), "Valid menu deposit failed");
        menu.handleFluidAction(player, new MessageCloudFluidAction(14, MessageCloudFluidAction.WITHDRAW, 1, -1));
        menu.handleFluidAction(player, new MessageCloudFluidAction(14, MessageCloudFluidAction.WITHDRAW, 1, 256));
        helper.assertTrue(cloud.getFluids().storedAmount() == 1000, "Out-of-range fluid index changed storage");
        cloud.getContainer().setItem(0, new ItemStack(Items.DIAMOND, 6));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(cloud.getContainer().getItem(0).getCount() == 6 && !menu.slots.getFirst().mayPlace(new ItemStack(Items.APPLE))
                && !menu.slots.getFirst().mayPickup(player), "Hidden item slots were usable in fluid view");
        player.containerMenu = player.inventoryMenu;
        menu.handleFluidAction(player, new MessageCloudFluidAction(14, MessageCloudFluidAction.WITHDRAW, 1, 0));
        helper.assertTrue(cloud.getFluids().storedAmount() == 1000, "Closed menu still allowed withdrawal");
        var clientMenu = new CloudChestMenu(14, player.getInventory(), new net.minecraft.world.SimpleContainer(27));
        var large = new CloudFluidStorage(() -> 9_000_000_000L);
        large.fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), EXECUTE);
        large.fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), EXECUTE);
        for (int i = 0; i < 20; i++) {
            var fluid = new FluidStack(Fluids.LAVA, i + 1);
            fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Fluid " + i));
            large.fill(fluid, EXECUTE);
        }
        var entries = large.snapshot();
        for (int offset = 0; offset < entries.size(); offset += MessageCloudFluids.BATCH_SIZE) {
            var packet = new MessageCloudFluids(14, large.revision(), large.capacity(), offset, entries.size(),
                    entries.subList(offset, Math.min(entries.size(), offset + MessageCloudFluids.BATCH_SIZE)));
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                MessageCloudFluids.CODEC.encode(buf, packet);
                clientMenu.receiveFluids(MessageCloudFluids.CODEC.decode(buf));
            } finally { buf.release(); }
            if (offset == 0) helper.assertTrue(clientMenu.getClientFluids().isEmpty(), "Incomplete snapshot exposed partial fluid ordering");
        }
        helper.assertTrue(clientMenu.getClientFluids().size() == 21 && clientMenu.getClientFluids().getFirst().amount() == 4_294_967_294L
                && clientMenu.getFluidCapacity() == 9_000_000_000L
                && FluidStack.isSameFluidSameComponents(clientMenu.getClientFluids().getLast().fluid(), entries.getLast().fluid()), "Batched snapshot lost long counts or components");
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
