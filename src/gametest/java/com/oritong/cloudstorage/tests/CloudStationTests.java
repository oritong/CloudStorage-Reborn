package com.oritong.cloudstorage.tests;

import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.misc.CloudFunction;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.mojang.authlib.GameProfile;
import java.util.UUID;

@GameTestHolder("cloudstorage_reborn")
@PrefixGameTestTemplate(false)
public final class CloudStationTests {
    private static Player player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CloudStations"));
    }

    @GameTest(template = "empty")
    public static void stationUploadsUnlockOnce(GameTestHelper helper) {
        var cloud = new CloudIndex(UUID.randomUUID(), 0x33AACC, 9);
        helper.assertTrue(cloud.uploadFunction(CloudFunction.SMITHING_TABLE), "First smithing table did not unlock the station");
        helper.assertTrue(!cloud.uploadFunction(CloudFunction.SMITHING_TABLE), "Second smithing table granted another station");
        helper.assertTrue(cloud.uploadFunction(CloudFunction.STONECUTTER), "First stonecutter did not unlock the station");
        helper.assertTrue(!cloud.uploadFunction(CloudFunction.STONECUTTER), "Second stonecutter granted another station");
        helper.assertTrue(cloud.functionMask() == (CloudFunction.SMITHING_TABLE.mask() | CloudFunction.STONECUTTER.mask()), "Station function mask is wrong");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void smithingRecipeConsumesInputs(GameTestHelper helper) {
        Player player = player(helper);
        var cloud = new CloudIndex(player.getUUID(), 0x556677, 9);
        cloud.uploadFunction(CloudFunction.SMITHING_TABLE);
        var menu = new CloudChestMenu(41, player.getInventory(), cloud, CSWorldData.get(helper.getLevel()));
        player.containerMenu = menu;
        menu.clickMenuButton(player, 103);
        menu.layoutSlots(6);
        cloud.getStations().smithing().setItem(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        cloud.getStations().smithing().setItem(1, new ItemStack(Items.DIAMOND_SWORD));
        cloud.getStations().smithing().setItem(2, new ItemStack(Items.NETHERITE_INGOT));
        menu.broadcastChanges();
        helper.assertTrue(menu.slots.get(menu.smithingStart() + 3).getItem().is(Items.NETHERITE_SWORD), "Smithing output did not use the vanilla recipe manager");
        menu.clicked(menu.smithingStart() + 3, 0, ClickType.PICKUP, player);
        helper.assertTrue(player.containerMenu.getCarried().is(Items.NETHERITE_SWORD), "Smithing result could not be taken");
        helper.assertTrue(cloud.getStations().smithing().isEmpty(), "Smithing inputs were not consumed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stonecutterRecipeAndPersistence(GameTestHelper helper) {
        Player player = player(helper);
        var cloud = new CloudIndex(player.getUUID(), 0x778899, 9);
        cloud.uploadFunction(CloudFunction.STONECUTTER);
        cloud.getStations().stonecutter().setItem(0, new ItemStack(Items.STONE));
        var menu = new CloudChestMenu(42, player.getInventory(), cloud, CSWorldData.get(helper.getLevel()));
        player.containerMenu = menu;
        menu.clickMenuButton(player, 104);
        menu.layoutSlots(6);
        menu.broadcastChanges();
        helper.assertTrue(!menu.slots.get(menu.stonecutterStart() + 1).getItem().isEmpty(), "Stonecutter did not list a vanilla recipe");
        menu.clickMenuButton(player, 501);
        menu.broadcastChanges();
        helper.assertTrue(menu.slots.get(menu.stonecutterStart() + 1).getItem().getCount() > 0, "Stonecutter recipe selection cleared the result");
        menu.clicked(menu.stonecutterStart() + 1, 0, ClickType.PICKUP, player);
        helper.assertTrue(cloud.getStations().stonecutter().getItem(0).isEmpty(),
                "Stonecutter did not consume its input: " + cloud.getStations().stonecutter().getItem(0));
        helper.assertTrue(!player.containerMenu.getCarried().isEmpty() && player.containerMenu.getCarried().getItem() != Items.STONE,
                "Stonecutter did not return its selected result: " + player.containerMenu.getCarried());
        cloud.getStations().stonecutter().setItem(0, new ItemStack(Items.STONE));
        var saved = new net.minecraft.nbt.CompoundTag();
        cloud.writeToNBT(saved, helper.getLevel().registryAccess());
        var restored = new CloudIndex(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.hasFunction(CloudFunction.STONECUTTER) && restored.getStations().stonecutter().getItem(0).is(Items.STONE),
                "Stonecutter input or station unlock did not persist");
        helper.succeed();
    }
}
