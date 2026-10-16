package com.oritong.cloudstorage.tests;

import com.mojang.authlib.GameProfile;
import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.block.AbstractCloudChestBlockEntity;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.entity.BalloonCargoEntity;
import com.oritong.cloudstorage.entity.BalloonEntity;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.CloudIOSide;
import com.oritong.cloudstorage.item.BalloonItem;
import com.oritong.cloudstorage.item.CSItemRegistry;
import com.oritong.cloudstorage.misc.CloudFunction;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.material.Fluids;

import java.util.UUID;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

@GameTestHolder("cloudstorage_reborn")
@PrefixGameTestTemplate(false)
public final class CloudIOTests {
    private static Player player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CloudIOTester"));
    }
    private record Setup(Player player, CloudIndex cloud, AbstractCloudChestBlockEntity chest, BlockPos pos) { }
    private static Setup setup(GameTestHelper helper, boolean shared, int slots) {
        return setup(helper, shared, slots, true);
    }
    private static Setup setup(GameTestHelper helper, boolean shared, int slots, boolean unlocked) {
        var player = player(helper);
        var data = CSWorldData.get(helper.getLevel());
        int color;
        do { color = UUID.randomUUID().hashCode() & 0xFFFFFF; } while (data.getPublicCloud(color) != null);
        var cloud = new CloudIndex(player.getUUID(), color, slots);
        if (unlocked) cloud.unlock(CloudFunction.HOPPER);
        if (shared) data.addPublicCloud(cloud); else data.addPrivateCloud(cloud);
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, (shared ? CSBlockRegistry.STATIC_CLOUD_CHEST.get() : CSBlockRegistry.CLOUD_CHEST.get()).defaultBlockState());
        var chest = (AbstractCloudChestBlockEntity) helper.getLevel().getBlockEntity(pos);
        chest.setBalloonColorFor(player, color);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2);
        return new Setup(player, cloud, chest, pos);
    }
    private static CloudChestMenu menu(GameTestHelper helper, Setup setup) {
        var menu = new CloudChestMenu(43, setup.player.getInventory(), setup.cloud, CSWorldData.get(helper.getLevel()), setup.chest);
        setup.player.containerMenu = menu;
        helper.assertTrue(menu.clickMenuButton(setup.player, 101) && menu.isIOOpen(), "Hopper panel did not open");
        return menu;
    }

    @GameTest(template = "empty")
    public static void hopperUploadPreservesContentsWithoutGrantingSlots(GameTestHelper helper) throws ReflectiveOperationException {
        var player = player(helper);
        var data = CSWorldData.get(helper.getLevel());
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (boolean shared : new boolean[]{false, true}) {
            int color;
            do { color = UUID.randomUUID().hashCode() & 0xFFFFFF; } while (data.getPublicCloud(color) != null);
            for (int repeat = 0; repeat < 2; repeat++) {
                helper.getLevel().setBlockAndUpdate(pos, Blocks.HOPPER.defaultBlockState());
                ((Container) helper.getLevel().getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 5));
                var item = (BalloonItem) CSItemRegistry.BALLOON.get();
                var stack = item.createBalloon(color, shared ? 1 : 0);
                helper.assertTrue(item.placeBalloon(helper.getLevel(), stack, pos, Direction.UP, player, false) && stack.isEmpty(), "Hopper failed to lift off");
                var cargo = helper.getLevel().getEntitiesOfClass(BalloonCargoEntity.class, new AABB(pos).inflate(2),
                        entity -> !entity.isRemoved() && player.getUUID().equals(entity.getPlayerUUID())).getFirst();
                var balloon = (BalloonEntity) cargo.getBalloon();
                var upload = BalloonEntity.class.getDeclaredMethod("upload");
                upload.setAccessible(true);
                upload.invoke(balloon);
                upload.invoke(balloon);
                var cloud = shared ? data.getPublicCloud(color) : data.getPrivateCloud(player.getUUID(), color);
                helper.assertTrue(cloud != null && cloud.functionMask() == 2 && cloud.getContainerSize() == 0 && cargo.isRemoved(),
                        "Hopper did not unlock once, or its five slots were incorrectly added");
                var saved = new CompoundTag();
                cloud.writeToNBT(saved, helper.getLevel().registryAccess());
                helper.assertTrue(new CloudIndex(saved, helper.getLevel().registryAccess()).hasFunction(CloudFunction.HOPPER), "Hopper unlock did not persist");
                balloon.discard();
            }
        }
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), entity -> entity.getItem().is(Items.DIAMOND))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) + dropped == 20, "Filled hopper uploads lost or duplicated their contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void facesGateItemsAndCachedHandlers(GameTestHelper helper) {
        var s = setup(helper, false, 27, false);
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, s.pos, Direction.DOWN);
        helper.assertTrue(handler != null && handler.getSlots() == 0, "An unconfigured side allowed IO");
        s.chest.automation().setMode(s.player, CloudIOSide.BOTTOM, 3);
        helper.assertTrue(handler.getSlots() == 0 && handler.insertItem(0, new ItemStack(Items.IRON_INGOT), false).getCount() == 1,
                "A chest without an uploaded hopper allowed IO");
        s.cloud.unlock(CloudFunction.HOPPER);
        helper.assertTrue(handler.getSlots() == 27, "A cached handler did not recognize a new hopper unlock");
        s.chest.automation().setMode(s.player, CloudIOSide.BOTTOM, 0);
        var menu = menu(helper, s);
        menu.clickMenuButton(s.player, 305);
        helper.assertTrue(handler.getSlots() == 27 && handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), true).isEmpty()
                && s.cloud.getContainer().isEmpty(), "Input simulation modified storage or a cached handler failed to update");
        handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false);
        helper.assertTrue(handler.extractItem(0, 3, false).isEmpty(), "Input-only face allowed extraction");
        menu.clickMenuButton(s.player, 305);
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT), false).getCount() == 1
                && handler.extractItem(0, 3, true).getCount() == 3 && s.cloud.getContainer().countItem(Items.IRON_INGOT) == 8,
                "Output-only face accepted input or simulation consumed items");
        handler.extractItem(0, 3, false);
        menu.clickMenuButton(s.player, 305);
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 2), false).isEmpty()
                && handler.extractItem(0, 1, false).getCount() == 1 && s.cloud.getContainer().countItem(Items.IRON_INGOT) == 6, "Bidirectional face lost items");
        s.cloud.resize(54);
        helper.assertTrue(handler.getSlots() == 54 && handler.insertItem(53, new ItemStack(Items.GOLD_INGOT), false).isEmpty(), "Cloud expansion left a stale IO inventory");
        menu.clickMenuButton(s.player, 305);
        helper.assertTrue(handler.getSlots() == 0 && handler.extractItem(0, 1, false).isEmpty(), "A disabled cached handler remained usable");
        menu.clickMenuButton(s.player, 304);
        helper.assertTrue(menu.getIOMode(CloudIOSide.TOP) == 0 && helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, s.pos, Direction.UP) == null
                && helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, s.pos, null) == null,
                "The top or unsided capability bypassed IO restrictions");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void privateBindingAndPhysicalSettingsPersist(GameTestHelper helper) {
        var s = setup(helper, false, 27);
        var other = player(helper);
        var secondCloud = new CloudIndex(other.getUUID(), s.cloud.getBalloonColor(), 27);
        secondCloud.unlock(CloudFunction.HOPPER);
        CSWorldData.get(helper.getLevel()).addPrivateCloud(secondCloud);
        s.chest.setBalloonColorFor(other, s.cloud.getBalloonColor());
        var menu = menu(helper, s);
        menu.clickMenuButton(s.player, 305);
        menu.clickMenuButton(s.player, 323);
        s.chest.lastValidPlayer = other;
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, s.pos, Direction.DOWN);
        handler.insertItem(0, new ItemStack(Items.DIAMOND, 4), false);
        helper.assertTrue(s.cloud.getContainer().countItem(Items.DIAMOND) == 4 && secondCloud.getContainer().isEmpty(), "Nearby player changed the automation inventory");
        var saved = s.chest.saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.putInt("CloudIO", saved.getInt("CloudIO") | (3 << (CloudIOSide.TOP.ordinal() * 2)));
        s.chest.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(s.chest.automation().collectDrops() && s.chest.automation().mode(CloudIOSide.BOTTOM) == 1
                && s.chest.automation().mode(CloudIOSide.TOP) == 0, "Saved IO settings or forced top lock failed");
        handler.insertItem(0, new ItemStack(Items.DIAMOND), false);
        helper.assertTrue(s.cloud.getContainer().countItem(Items.DIAMOND) == 5 && secondCloud.getContainer().isEmpty(), "Saved automation owner was lost");
        var pos2 = s.pos.east(2);
        helper.getLevel().setBlockAndUpdate(pos2, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
        var chest2 = (AbstractCloudChestBlockEntity) helper.getLevel().getBlockEntity(pos2);
        chest2.setBalloonColorFor(s.player, s.cloud.getBalloonColor());
        helper.assertTrue(chest2.automation().settings() == 0, "Face configuration leaked to another chest using the same cloud");
        s.player.setPos(s.pos.getX() + 30, s.pos.getY(), s.pos.getZ());
        helper.assertTrue(!menu.clickMenuButton(s.player, 305) && !menu.clickMenuButton(other, 305), "Remote or wrong-player IO edits were accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void batchConfigurationAndFluidSides(GameTestHelper helper) {
        var s = setup(helper, true, 27);
        s.cloud.addFluidStorage(4000);
        var menu = menu(helper, s);
        menu.clickMenuButton(s.player, 320);
        var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, s.pos, Direction.DOWN);
        helper.assertTrue(handler != null && handler.fill(new FluidStack(Fluids.WATER, 1000), SIMULATE) == 1000
                && s.cloud.getFluids().storedAmount() == 0 && handler.fill(new FluidStack(Fluids.WATER, 1000), EXECUTE) == 1000
                && handler.drain(1000, EXECUTE).isEmpty(), "Input-only fluid side or simulation failed");
        menu.clickMenuButton(s.player, 320);
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), EXECUTE) == 0
                && handler.drain(500, SIMULATE).getAmount() == 500 && s.cloud.getFluids().storedAmount() == 1000
                && handler.drain(500, EXECUTE).getAmount() == 500, "Output-only fluid side violated its mode");
        menu.clickMenuButton(s.player, 320);
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), EXECUTE) == 1000 && handler.drain(500, EXECUTE).getAmount() == 500,
                "Bidirectional fluid IO failed");
        menu.clickMenuButton(s.player, 321);
        helper.assertTrue(menu.getIOMode(CloudIOSide.BOTTOM) == 2, "Reverse batch cycling failed");
        menu.clickMenuButton(s.player, 300);
        menu.clickMenuButton(s.player, 320);
        helper.assertTrue(menu.getIOSettings() == 0 && handler.getTanks() == 0, "Mixed face settings did not clear on batch cycling");
        menu.clickMenuButton(s.player, 320);
        menu.clickMenuButton(s.player, 322);
        helper.assertTrue(menu.getIOSettings() == 0 && helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, s.pos, Direction.UP) == null,
                "Clear-all or the fixed top face failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void collectionKeepsOverflowAndIgnoresOtherLocations(GameTestHelper helper) {
        var s = setup(helper, false, 1);
        s.cloud.getContainer().setItem(0, new ItemStack(Items.DIAMOND, 63));
        var above = new ItemEntity(helper.getLevel(), s.pos.getX() + 0.5, s.pos.getY() + 1.1, s.pos.getZ() + 0.5, new ItemStack(Items.DIAMOND, 5));
        var outside = new ItemEntity(helper.getLevel(), s.pos.getX() + 1.5, s.pos.getY() + 1.1, s.pos.getZ() + 0.5, new ItemStack(Items.DIAMOND, 3));
        helper.getLevel().addFreshEntity(above);
        helper.getLevel().addFreshEntity(outside);
        s.chest.tickCount = 8;
        s.chest.automation().tick();
        helper.assertTrue(s.cloud.getContainer().countItem(Items.DIAMOND) == 63, "Item collection started enabled");
        var menu = menu(helper, s);
        menu.clickMenuButton(s.player, 323);
        s.chest.automation().tick();
        helper.assertTrue(s.cloud.getContainer().countItem(Items.DIAMOND) == 64 && above.getItem().getCount() == 4
                && outside.getItem().getCount() == 3 && !above.isRemoved() && menu.getIOMode(CloudIOSide.TOP) == 0,
                "Collection lost overflow, picked items beside the chest, or enabled top IO");
        s.cloud.getContainer().clearContent();
        menu.clickMenuButton(s.player, 323);
        s.chest.automation().tick();
        helper.assertTrue(above.getItem().getCount() == 4 && s.cloud.getContainer().isEmpty(), "Disabled collection continued storing items");
        menu.clickMenuButton(s.player, 323);
        s.chest.automation().tick();
        helper.assertTrue(above.isRemoved() && s.cloud.getContainer().countItem(Items.DIAMOND) == 4 && outside.isAlive(), "Successful collection duplicated or lost a dropped stack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void realHoppersMoveItemsBetweenConfiguredFaces(GameTestHelper helper) {
        var s = setup(helper, true, 27);
        s.chest.automation().setMode(s.player, CloudIOSide.LEFT, 1);
        s.chest.automation().setMode(s.player, CloudIOSide.BOTTOM, 2);
        var inputPos = s.pos.relative(s.chest.automation().front().getClockWise());
        var inputState = Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, s.chest.automation().front().getCounterClockWise());
        helper.getLevel().setBlockAndUpdate(inputPos, inputState);
        helper.getLevel().setBlockAndUpdate(s.pos.below(), Blocks.HOPPER.defaultBlockState());
        var source = (Container) helper.getLevel().getBlockEntity(inputPos);
        var target = (Container) helper.getLevel().getBlockEntity(s.pos.below());
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 8));
        helper.runAfterDelay(24, () -> {
            int total = source.countItem(Items.IRON_INGOT) + target.countItem(Items.IRON_INGOT) + s.cloud.getContainer().countItem(Items.IRON_INGOT);
            helper.assertTrue(total == 8 && target.countItem(Items.IRON_INGOT) > 0, "Real hopper input/output failed or duplicated items");
            helper.succeed();
        });
    }
}
