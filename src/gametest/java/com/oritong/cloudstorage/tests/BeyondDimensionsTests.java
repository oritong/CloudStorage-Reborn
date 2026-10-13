package com.oritong.cloudstorage.tests;

import com.mojang.authlib.GameProfile;
import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.block.CloudChestBlockEntity;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.compat.BeyondDimensionsCompat;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.item.BalloonItem;
import com.oritong.cloudstorage.item.CSItemRegistry;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

@GameTestHolder("cloudstorage_reborn")
@PrefixGameTestTemplate(false)
public class BeyondDimensionsTests {
    private static long countMatching(IItemHandler target, ItemStack stack) {
        long count = 0;
        for (int slot = 0; slot < target.getSlots(); slot++) {
            ItemStack stored = target.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(stack, stored)) count += stored.getCount();
        }
        return count;
    }

    @GameTest(template = "empty")
    public static void partialTransfersPreserveSearchAndComponents(GameTestHelper helper) {
        var source = new SimpleContainer(27);
        ItemStack balloon = ((BalloonItem) CSItemRegistry.BALLOON.get()).createBalloon(0x235679, 0);
        balloon.setCount(8);
        balloon.set(DataComponents.CUSTOM_NAME, Component.literal("Keep this balloon"));
        source.setItem(17, balloon);
        source.setItem(23, new ItemStack(Items.DIAMOND, 3));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new CloudChestMenu(3, player.getInventory(), source);
        menu.search(player, "@cloudstorage_reborn");
        var target = new ItemStackHandler(1) {
            @Override public int getSlotLimit(int slot) { return 5; }
        };
        long accepted = BeyondDimensionsCompat.transfer(source, target);
        helper.assertTrue(accepted == 5 && target.getStackInSlot(0).getCount() == 5, "Partial transfer accepted the wrong count");
        helper.assertTrue(ItemStack.isSameItemSameComponents(balloon, target.getStackInSlot(0)), "Transfer lost color or custom name");
        helper.assertTrue(BeyondDimensionsCompat.transfer(source, target) == 0, "A full destination accepted more items");
        menu.search(player, "");
        helper.assertTrue(source.getItem(17).getCount() == 3 && source.getItem(23).getCount() == 3, "Clearing search duplicated transferred items or lost remainders");
        menu.search(player, "@cloudstorage_reborn");
        var roomy = new ItemStackHandler(1);
        helper.assertTrue(BeyondDimensionsCompat.transfer(source, roomy) == 3, "Final balloon transfer failed");
        menu.search(player, "");
        helper.assertTrue(source.getItem(17).isEmpty() && source.getItem(23).getCount() == 3, "Clearing search restored a fully transferred stack");
        menu.removed(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void optionalNetworkIntegration(GameTestHelper helper) throws ReflectiveOperationException {
        var level = helper.getLevel();
        Player owner = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "CloudOwner"));
        var data = CSWorldData.get(level);
        var cloud = new CloudIndex(owner.getUUID(), 0x345678, 27);
        ItemStack named = new ItemStack(Items.DIAMOND, 8);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Dimensional diamonds"));
        ItemStack expectedNamed = named.copy();
        cloud.getContainer().setItem(5, named);
        data.addPrivateCloud(cloud);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
        var chest = (CloudChestBlockEntity) level.getBlockEntity(pos);
        chest.setBalloonColorFor(owner, cloud.getBalloonColor());
        boolean previous = CloudStorage.CONFIG.beyondDimensionsStorage.get();
        try {
            CloudStorage.CONFIG.beyondDimensionsStorage.set(true);
            helper.assertTrue(chest.getMenu(4, owner.getInventory()) instanceof CloudChestMenu, "Missing network should keep the ordinary menu");
            helper.assertTrue(cloud.getContainer().getItem(5).getCount() == 8, "Missing optional mod/network changed cloud items");
            if (!ModList.get().isLoaded("beyonddimensions")) {
                helper.succeed();
                return;
            }
            String pkg = "com.wintercogs.beyonddimensions.";
            Class<?> netClass = Class.forName(pkg + "api.dimensionnet.DimensionsNet");
            Object net = netClass.getMethod("createNewNetForPlayer", Player.class, long.class, int.class).invoke(null, owner, 5L, 1);
            netClass.getMethod("setPrimaryNetForPlayer", Player.class, netClass).invoke(null, owner, net);
            Object storage = netClass.getMethod("getUnifiedStorage").invoke(net);
            Class<?> storageClass = Class.forName(pkg + "api.dimensionnet.UnifiedStorage");
            IItemHandler target = (IItemHandler) Class.forName(pkg + "api.capability.helper.unordered.ItemUnifiedStorageHandler")
                    .getConstructor(storageClass).newInstance(storage);
            helper.assertTrue(chest.getMenu(5, owner.getInventory()) instanceof CloudChestMenu, "Full network must leave a way to retrieve remaining cloud items");
            helper.assertTrue(target.getStackInSlot(0).getCount() == 5 && cloud.getContainer().getItem(5).getCount() == 3, "Real network did not preserve the partial remainder");
            storageClass.getMethod("setSlotCapacity", long.class).invoke(storage, 100L);
            storageClass.getMethod("setSlotMaxSize", int.class).invoke(storage, 10);
            var otherColor = new CloudIndex(owner.getUUID(), 0x456789, 27);
            otherColor.getContainer().setItem(6, new ItemStack(Items.GOLD_INGOT, 9));
            data.addPrivateCloud(otherColor);
            var publicCloud = new CloudIndex(owner.getUUID(), 0x567890, 27);
            publicCloud.getContainer().setItem(0, new ItemStack(Items.IRON_INGOT, 11));
            data.addPublicCloud(publicCloud);
            var lootCloud = new CloudIndex(owner.getUUID(), 0x678901, 27);
            lootCloud.getContainer().setItem(0, new ItemStack(Items.EMERALD, 12));
            data.addLootCloud(lootCloud);
            Player second = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "CloudSecond"));
            var secondCloud = new CloudIndex(second.getUUID(), cloud.getBalloonColor(), 27);
            secondCloud.getContainer().setItem(0, new ItemStack(Items.DIAMOND, 14));
            data.addPrivateCloud(secondCloud);
            var linked = chest.getMenu(6, owner.getInventory());
            helper.assertTrue(linked != null && linked.getClass().getName().endsWith(".DimensionsNetMenu"), "Cloud chest did not open the actual dimensional storage menu");
            helper.assertTrue(cloud.getContainer().isEmpty() && otherColor.getContainer().isEmpty(), "Not all of the owner's private colors were transferred");
            helper.assertTrue(target.getStackInSlot(0).getCount() == 8 && ItemStack.isSameItemSameComponents(expectedNamed, target.getStackInSlot(0)), "Real network changed item data or duplicated a transfer");
            helper.assertTrue(target.getStackInSlot(1).is(Items.GOLD_INGOT) && target.getStackInSlot(1).getCount() == 9, "Second private color missed the personal network");
            helper.assertTrue(publicCloud.getContainer().getItem(0).getCount() == 11 && lootCloud.getContainer().getItem(0).getCount() == 12 && secondCloud.getContainer().getItem(0).getCount() == 14, "Transfer touched a public, loot or different player's inventory");
            linked.removed(owner);
            // A member of the owner's network must not donate their private clouds to it.
            netClass.getMethod("addPlayer", UUID.class).invoke(net, second.getUUID());
            netClass.getMethod("setPrimaryNetForPlayer", Player.class, netClass).invoke(null, second, net);
            helper.assertTrue(BeyondDimensionsCompat.createMenu(7, second.getInventory(), secondCloud) == null && secondCloud.getContainer().getItem(0).getCount() == 14, "Shared-network membership bypassed the ownership check");
            var secondNet = netClass.getMethod("createNewNetForPlayer", Player.class, long.class, int.class);
            netClass.getMethod("clearPrimaryNetForPlayer", Player.class).invoke(null, second);
            Object ownSecondNet = secondNet.invoke(null, second, 100L, 10);
            netClass.getMethod("setPrimaryNetForPlayer", Player.class, netClass).invoke(null, second, ownSecondNet);
            var secondMenu = BeyondDimensionsCompat.createMenu(8, second.getInventory(), secondCloud);
            helper.assertTrue(secondMenu != null && secondCloud.getContainer().isEmpty(), "Second player could not use their own network");
            Object secondStorage = netClass.getMethod("getUnifiedStorage").invoke(ownSecondNet);
            var secondTarget = (IItemHandler) Class.forName(pkg + "api.capability.helper.unordered.ItemUnifiedStorageHandler").getConstructor(storageClass).newInstance(secondStorage);
            helper.assertTrue(secondTarget.getStackInSlot(0).getCount() == 14 && !ItemStack.isSameItemSameComponents(secondTarget.getStackInSlot(0), target.getStackInSlot(0)), "Two owners' networks were mixed");
            secondMenu.removed(second);
            CloudStorage.CONFIG.beyondDimensionsStorage.set(false);
            cloud.getContainer().setItem(10, new ItemStack(Items.APPLE, 4));
            helper.assertTrue(chest.getMenu(9, owner.getInventory()) instanceof CloudChestMenu && cloud.getContainer().getItem(10).getCount() == 4, "Disabling integration did not restore local cloud storage");
            helper.assertTrue(target.getStackInSlot(0).getCount() == 8, "Disabling integration pulled transferred items out of the network");
            CloudStorage.CONFIG.beyondDimensionsStorage.set(true);
            chest.getMenu(10, owner.getInventory()).removed(owner);
            CompoundTag savedClouds = data.save(new CompoundTag(), level.registryAccess());
            helper.assertTrue(CSWorldData.load(savedClouds, level.registryAccess()).getPrivateCloud(owner.getUUID(), cloud.getBalloonColor()).getContainer().isEmpty(), "Transferred items reappeared in saved cloud data");
            var savedNet = (CompoundTag) netClass.getMethod("save", CompoundTag.class, net.minecraft.core.HolderLookup.Provider.class).invoke(net, new CompoundTag(), level.registryAccess());
            Object restoredNet = netClass.getMethod("load", CompoundTag.class, net.minecraft.core.HolderLookup.Provider.class).invoke(null, savedNet, level.registryAccess());
            var restoredTarget = (IItemHandler) Class.forName(pkg + "api.capability.helper.unordered.ItemUnifiedStorageHandler").getConstructor(storageClass).newInstance(netClass.getMethod("getUnifiedStorage").invoke(restoredNet));
            helper.assertTrue(countMatching(restoredTarget, expectedNamed) == 8 && countMatching(restoredTarget, new ItemStack(Items.GOLD_INGOT)) == 9 && countMatching(restoredTarget, new ItemStack(Items.APPLE)) == 4, "Dimensional storage lost migrated items during serialization");
            helper.succeed();
        } finally {
            CloudStorage.CONFIG.beyondDimensionsStorage.set(previous);
        }
    }
}
