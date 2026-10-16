package com.oritong.cloudstorage.tests;

import com.mojang.authlib.GameProfile;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.block.CloudChestBlockEntity;
import com.oritong.cloudstorage.block.StaticCloudChestBlockEntity;
import com.oritong.cloudstorage.entity.BalloonCargoEntity;
import com.oritong.cloudstorage.entity.BalloonEntity;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.item.BalloonItem;
import com.oritong.cloudstorage.item.CSItemRegistry;
import com.oritong.cloudstorage.misc.CloudFunction;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("cloudstorage_reborn")
@PrefixGameTestTemplate(false)
public final class CloudCraftingTests {
    private static Player player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CloudCrafter"));
    }
    private static CloudIndex cloud(Player player, int slots) {
        var cloud = new CloudIndex(player.getUUID(), 0xB67234, slots);
        cloud.unlock(CloudFunction.CRAFTING_TABLE);
        return cloud;
    }
    private static CloudChestMenu menu(GameTestHelper helper, Player player, CloudIndex cloud) {
        var menu = new CloudChestMenu(31, player.getInventory(), cloud, CSWorldData.get(helper.getLevel()));
        player.containerMenu = menu;
        menu.clickMenuButton(player, 100);
        return menu;
    }
    private static void sticks(CloudIndex cloud, int amount) {
        cloud.getCrafting().setItem(0, new ItemStack(Items.OAK_PLANKS, amount));
        cloud.getCrafting().setItem(3, new ItemStack(Items.OAK_PLANKS, amount));
    }
    private static void shift(CloudChestMenu menu, Player player) {
        menu.clicked(menu.craftingResultSlot(), 0, ClickType.QUICK_MOVE, player);
    }
    private static void fillInventory(Player player) {
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
    }

    @GameTest(template = "empty")
    public static void workbenchUploadUnlocksOnce(GameTestHelper helper) throws ReflectiveOperationException {
        Player player = player(helper);
        var data = CSWorldData.get(helper.getLevel());
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        int color = 0xB68832;
        for (boolean shared : new boolean[]{ false, true }) {
            if (shared) while (data.getPublicCloud(color) != null) color++;
            for (int repeat = 0; repeat < 2; repeat++) {
                helper.getLevel().setBlockAndUpdate(pos, Blocks.CRAFTING_TABLE.defaultBlockState());
                var balloonItem = (BalloonItem) CSItemRegistry.BALLOON.get();
                var stack = balloonItem.createBalloon(color, shared ? 1 : 0);
                helper.assertTrue(balloonItem.placeBalloon(helper.getLevel(), stack, pos, Direction.UP, player, false)
                        && stack.isEmpty() && helper.getLevel().getBlockState(pos).isAir(), "Workbench did not lift off");
                var cargo = helper.getLevel().getEntitiesOfClass(BalloonCargoEntity.class, new AABB(pos).inflate(2),
                        entity -> !entity.isRemoved() && player.getUUID().equals(entity.getPlayerUUID())).getFirst();
                var saved = new CompoundTag();
                cargo.saveWithoutId(saved);
                helper.assertTrue(saved.getCompound("BlockState").getString("Name").equals("minecraft:crafting_table"),
                        "In-flight workbench identity was lost");
                var balloon = (BalloonEntity) cargo.getBalloon();
                var upload = BalloonEntity.class.getDeclaredMethod("upload");
                upload.setAccessible(true);
                upload.invoke(balloon);
                upload.invoke(balloon);
                var cloud = shared ? data.getPublicCloud(color) : data.getPrivateCloud(player.getUUID(), color);
                helper.assertTrue(cloud != null && cloud.functionMask() == 1 && cloud.hasStorage()
                        && cloud.getContainerSize() == 0 && cloud.getFluids().capacity() == 0 && cargo.isRemoved(),
                        "Workbench upload granted duplicate capacity or failed to unlock");
                balloon.discard();
            }
            helper.getLevel().setBlockAndUpdate(pos, (shared ? CSBlockRegistry.STATIC_CLOUD_CHEST.get()
                    : CSBlockRegistry.CLOUD_CHEST.get()).defaultBlockState());
            if (shared) {
                var chest = (StaticCloudChestBlockEntity) helper.getLevel().getBlockEntity(pos);
                chest.setBalloonColorFor(null, color);
                helper.assertTrue(!chest.hasNoInvSpace(player) && chest.getMenu(31, player.getInventory()) instanceof CloudChestMenu,
                        "Workbench-only public cloud cannot open");
            } else {
                var chest = (CloudChestBlockEntity) helper.getLevel().getBlockEntity(pos);
                chest.setBalloonColorFor(player, color);
                helper.assertTrue(!chest.hasNoInvSpace(player) && chest.getMenu(31, player.getInventory()) instanceof CloudChestMenu,
                        "Workbench-only private cloud cannot open");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gridPersistsAndStaysIsolated(GameTestHelper helper) {
        var player = player(helper);
        var cloud = cloud(player, 27);
        var named = new ItemStack(Items.DIAMOND, 7);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Saved ingredient"));
        cloud.getCrafting().setItem(8, named);
        var menu = menu(helper, player, cloud);
        menu.clickMenuButton(player, 100);
        menu.clicked(menu.craftingStart() + 8, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty(), "Closed crafting slots remained clickable");
        menu.removed(player);
        var saved = new CompoundTag();
        cloud.setAutoPull(true);
        cloud.setCraftToCloud(true);
        cloud.writeToNBT(saved, helper.getLevel().registryAccess());
        var restored = new CloudIndex(saved, helper.getLevel().registryAccess());
        var otherColor = new CloudIndex(player.getUUID(), cloud.getBalloonColor() + 1, 27);
        var otherPlayer = new CloudIndex(UUID.randomUUID(), cloud.getBalloonColor(), 27);
        helper.assertTrue(ItemStack.matches(restored.getCrafting().getItem(8), named) && restored.functionMask() == 1
                && restored.autoPull() && restored.craftToCloud() && !restored.autoClear()
                && otherColor.getCrafting().isEmpty() && otherPlayer.getCrafting().isEmpty() && otherPlayer.functionMask() == 0,
                "Crafting grid, components, settings or isolation failed to persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void autoClearKeepsAnythingThatDoesNotFit(GameTestHelper helper) {
        var player = player(helper);
        var cloud = cloud(player, 1);
        cloud.getContainer().setItem(0, new ItemStack(Items.DIAMOND, 63));
        cloud.getCrafting().setItem(0, new ItemStack(Items.DIAMOND, 5));
        cloud.getCrafting().setItem(1, new ItemStack(Items.APPLE, 3));
        var menu = menu(helper, player, cloud);
        menu.clickMenuButton(player, 200);
        menu.removed(player);
        helper.assertTrue(cloud.getContainer().countItem(Items.DIAMOND) == 64
                && cloud.getCrafting().getItem(0).getCount() == 4 && cloud.getCrafting().getItem(1).getCount() == 3
                && player.getInventory().isEmpty(), "Auto clear lost ingredients or sent them to the player");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void autoPullUsesCloudIngredientsAndTagAlternatives(GameTestHelper helper) {
        var player = player(helper);
        var cloud = cloud(player, 27);
        sticks(cloud, 1);
        cloud.getContainer().setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
        var menu = menu(helper, player, cloud);
        menu.clickMenuButton(player, 201);
        shift(menu, player);
        helper.assertTrue(player.getInventory().countItem(Items.STICK) == 20 && cloud.getContainer().isEmpty()
                && cloud.getCrafting().isEmpty(), "Auto pull did not replenish ingredients between crafts");
        for (int i = 0; i < 9; i++) if (i != 4) cloud.getCrafting().setItem(i, new ItemStack(Items.OAK_PLANKS));
        cloud.getContainer().setItem(0, new ItemStack(Items.BIRCH_PLANKS, 8));
        shift(menu, player);
        helper.assertTrue(player.getInventory().countItem(Items.CHEST) == 2 && cloud.getCrafting().isEmpty()
                && cloud.getContainer().isEmpty(), "Tag-compatible materials were not used for an identical chest recipe");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftOutputFallsBackWithoutLosingItems(GameTestHelper helper) {
        var player = player(helper);
        var cloud = cloud(player, 1);
        var menu = menu(helper, player, cloud);
        fillInventory(player);
        cloud.getContainer().setItem(0, new ItemStack(Items.STICK, 62));
        sticks(cloud, 1);
        shift(menu, player);
        helper.assertTrue(cloud.getCrafting().getItem(0).getCount() == 1 && cloud.getContainer().countItem(Items.STICK) == 62,
                "Insufficient combined space consumed a recipe or produced a partial output");
        cloud.getContainer().setItem(0, new ItemStack(Items.STICK, 60));
        shift(menu, player);
        helper.assertTrue(cloud.getCrafting().isEmpty() && cloud.getContainer().countItem(Items.STICK) == 64,
                "Full player inventory did not fall back to cloud storage");
        player.getInventory().clearContent();
        cloud.getContainer().clearContent();
        menu.clickMenuButton(player, 202);
        sticks(cloud, 1);
        shift(menu, player);
        helper.assertTrue(cloud.getContainer().countItem(Items.STICK) == 4 && player.getInventory().countItem(Items.STICK) == 0,
                "Cloud-first output ignored its priority");
        cloud.getContainer().setItem(0, new ItemStack(Items.STONE, 64));
        sticks(cloud, 1);
        shift(menu, player);
        helper.assertTrue(player.getInventory().countItem(Items.STICK) == 4 && cloud.getCrafting().isEmpty(),
                "Full cloud storage did not fall back to inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void remainderAndSharedResultCannotDuplicate(GameTestHelper helper) {
        var player = player(helper);
        var cloud = cloud(player, 27);
        var first = menu(helper, player, cloud);
        for (int i = 0; i < 3; i++) cloud.getCrafting().setItem(i, new ItemStack(Items.MILK_BUCKET));
        cloud.getCrafting().setItem(3, new ItemStack(Items.SUGAR));
        cloud.getCrafting().setItem(4, new ItemStack(Items.EGG));
        cloud.getCrafting().setItem(5, new ItemStack(Items.SUGAR));
        for (int i = 6; i < 9; i++) cloud.getCrafting().setItem(i, new ItemStack(Items.WHEAT));
        first.broadcastChanges();
        var secondPlayer = player(helper);
        var second = menu(helper, secondPlayer, cloud);
        first.clicked(first.craftingResultSlot(), 0, ClickType.PICKUP, player);
        second.clicked(second.craftingResultSlot(), 0, ClickType.PICKUP, secondPlayer);
        helper.assertTrue(first.getCarried().is(Items.CAKE) && first.getCarried().getCount() == 1
                && second.getCarried().isEmpty() && cloud.getCrafting().countItem(Items.BUCKET) == 3,
                "Recipe remainders were lost or another viewer took a stale result");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void idleRefillAndSavedPatternStayStable(GameTestHelper helper) {
        var player = player(helper);
        var cloud = cloud(player, 27);
        sticks(cloud, 1);
        cloud.setAutoPull(true);
        var menu = menu(helper, player, cloud);
        shift(menu, player);
        var other = menu(helper, player(helper), cloud);
        long revision = cloud.getCrafting().revision();
        for (int i = 0; i < 10; i++) { menu.broadcastChanges(); other.broadcastChanges(); }
        helper.assertTrue(cloud.getCrafting().revision() == revision, "Idle viewers repeatedly invalidated the shared crafting grid");
        var saved = new CompoundTag();
        cloud.writeToNBT(saved, helper.getLevel().registryAccess());
        var restored = new CloudIndex(saved, helper.getLevel().registryAccess());
        restored.getContainer().setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
        var reopened = menu(helper, player, restored);
        helper.assertTrue(reopened.getCrafting().countItem(Items.OAK_PLANKS) == 2
                && reopened.slots.get(reopened.craftingResultSlot()).getItem().is(Items.STICK),
                "An exhausted refill pattern failed to survive world save and restore");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftCraftingHasABoundedBatch(GameTestHelper helper) {
        var player = player(helper);
        var cloud = cloud(player, 27);
        for (int i = 0; i < 27; i++) cloud.getContainer().setItem(i, new ItemStack(Items.OAK_PLANKS, 64));
        sticks(cloud, 1);
        var menu = menu(helper, player, cloud);
        menu.clickMenuButton(player, 201);
        shift(menu, player);
        helper.assertTrue(player.getInventory().countItem(Items.STICK) == 256
                && cloud.getContainer().countItem(Items.OAK_PLANKS) + cloud.getCrafting().countItem(Items.OAK_PLANKS) == 1602,
                "Shift crafting exceeded its 64-craft batch or violated ingredient conservation");
        helper.succeed();
    }
}
