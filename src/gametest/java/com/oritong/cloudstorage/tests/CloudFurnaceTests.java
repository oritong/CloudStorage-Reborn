package com.oritong.cloudstorage.tests;

import com.mojang.authlib.GameProfile;
import com.oritong.cloudstorage.block.AbstractCloudChestBlockEntity;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.entity.BalloonCargoEntity;
import com.oritong.cloudstorage.entity.BalloonEntity;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.CloudFurnace;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("cloudstorage_reborn")
@PrefixGameTestTemplate(false)
public final class CloudFurnaceTests {
    private static Player player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CloudSmelter"));
    }
    private static CloudIndex cloud(int furnaces, int slots) {
        var cloud = new CloudIndex(UUID.randomUUID(), 0xA332EF, slots);
        for (int i = 0; i < furnaces; i++) cloud.uploadFunction(CloudFunction.FURNACE);
        return cloud;
    }
    private static void tick(GameTestHelper helper, CloudIndex cloud, int ticks) {
        for (int i = 0; i < ticks; i++) cloud.getFurnace().tick(helper.getLevel(), cloud.getContainer());
    }
    private static void upload(GameTestHelper helper, Player owner, BlockPos pos, Block block, int color, boolean shared) throws ReflectiveOperationException {
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var item = (BalloonItem) CSItemRegistry.BALLOON.get();
        helper.assertTrue(item.placeBalloon(helper.getLevel(), item.createBalloon(color, shared ? 1 : 0), pos, Direction.UP, owner, false), "Functional block did not lift off");
        var cargo = helper.getLevel().getEntitiesOfClass(BalloonCargoEntity.class, new AABB(pos).inflate(2),
                e -> !e.isRemoved() && owner.getUUID().equals(e.getPlayerUUID())).getFirst();
        var balloon = (BalloonEntity) cargo.getBalloon();
        var method = BalloonEntity.class.getDeclaredMethod("upload");
        method.setAccessible(true);
        method.invoke(balloon);
        method.invoke(balloon);
        helper.assertTrue(cargo.isRemoved(), "Upload was not finalized");
        balloon.discard();
    }

    @GameTest(template = "empty")
    public static void uploadLimitsAndDuplicateBlocks(GameTestHelper helper) throws ReflectiveOperationException {
        var owner = player(helper);
        var data = CSWorldData.get(helper.getLevel());
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (boolean shared : new boolean[]{false, true}) {
            int color = UUID.randomUUID().hashCode() & 0xFFFFFF;
            while (data.getPublicCloud(color) != null) color++;
            var cloud = new CloudIndex(owner.getUUID(), color, 27);
            if (shared) data.addPublicCloud(cloud); else data.addPrivateCloud(cloud);
            for (int i = 1; i <= 65; i++) {
                upload(helper, owner, pos, Blocks.FURNACE, color, shared);
                int expected = i >= 64 ? 9 : i >= 32 ? 4 : i >= 16 ? 2 : 1;
                helper.assertTrue(cloud.getFurnace().count() == Math.min(i, 64) && cloud.getFurnace().slotsPerGroup() == expected, "Furnace count or slot tier is wrong at " + i);
            }
            upload(helper, owner, pos, Blocks.CRAFTING_TABLE, color, shared);
            upload(helper, owner, pos, Blocks.CRAFTING_TABLE, color, shared);
            upload(helper, owner, pos, Blocks.HOPPER, color, shared);
            upload(helper, owner, pos, Blocks.HOPPER, color, shared);
            helper.assertTrue(cloud.functionMask() == 7 && cloud.getContainerSize() == 27
                    && cloud.getContainer().countItem(Items.FURNACE) == 1 && cloud.getContainer().countItem(Items.CRAFTING_TABLE) == 1
                    && cloud.getContainer().countItem(Items.HOPPER) == 1, "Duplicate functional blocks were lost or granted extra capacity");
            for (int i = 0; i < 27; i++) cloud.getContainer().setItem(i, new ItemStack(Items.STONE, 64));
            upload(helper, owner, pos, Blocks.FURNACE, color, shared);
            upload(helper, owner, pos, Blocks.CRAFTING_TABLE, color, shared);
            upload(helper, owner, pos, Blocks.HOPPER, color, shared);
            helper.assertTrue(cloud.getContainer().countItem(Items.STONE) == 27 * 64 && owner.getInventory().isEmpty(), "Full cloud duplicates replaced items or returned to the player");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void parallelSmeltingAcrossRecipesAndTiers(GameTestHelper helper) {
        for (int count : new int[]{1, 16, 32, 64}) {
            var cloud = cloud(count, 27);
            var furnace = cloud.getFurnace();
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, count == 1 ? 1 : count / 2));
            if (count > 1) furnace.setItem(1, new ItemStack(Items.RAW_GOLD, count / 2));
            furnace.setItem(9, new ItemStack(Items.COAL, 64));
            tick(helper, cloud, 199);
            helper.assertTrue(furnace.countItem(Items.IRON_INGOT) == 0 && furnace.active() == count, "A lane ran faster than a vanilla furnace or parallelism was lost");
            tick(helper, cloud, 1);
            helper.assertTrue(furnace.countItem(Items.IRON_INGOT) == (count == 1 ? 1 : count / 2)
                    && furnace.countItem(Items.GOLD_INGOT) == count / 2 && furnace.countItem(Items.COAL) == 64 - count
                    && furnace.countItem(Items.RAW_IRON) + furnace.countItem(Items.RAW_GOLD) == 0, "Parallel recipes lost items or consumed the wrong fuel at " + count);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void laneLimitAndFuelReuse(GameTestHelper helper) {
        var cloud = cloud(2, 0);
        var furnace = cloud.getFurnace();
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        furnace.setItem(9, new ItemStack(Items.COAL, 2));
        tick(helper, cloud, 200);
        helper.assertTrue(furnace.countItem(Items.IRON_INGOT) == 2 && furnace.countItem(Items.RAW_IRON) == 6, "Two uploaded furnaces did not grant exactly two lanes");
        tick(helper, cloud, 200);
        helper.assertTrue(furnace.countItem(Items.IRON_INGOT) == 4 && furnace.countItem(Items.RAW_IRON) == 4 && furnace.countItem(Items.COAL) == 0, "Burning fuel was not reused between recipes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullOutputsAndCloudKeepItems(GameTestHelper helper) {
        var cloud = cloud(1, 1);
        var furnace = cloud.getFurnace();
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        furnace.setItem(9, new ItemStack(Items.COAL));
        furnace.setItem(18, new ItemStack(Items.STONE, 64));
        tick(helper, cloud, 250);
        helper.assertTrue(furnace.countItem(Items.RAW_IRON) == 2 && furnace.countItem(Items.COAL) == 1, "A full output consumed inputs or new fuel");
        furnace.setItem(18, ItemStack.EMPTY);
        tick(helper, cloud, 200);
        helper.assertTrue(furnace.getItem(18).is(Items.IRON_INGOT) && cloud.getContainer().isEmpty(), "Default output was not kept in the furnace");
        cloud.getContainer().setItem(0, new ItemStack(Items.STONE, 64));
        furnace.toggle(0);
        tick(helper, cloud, 8);
        helper.assertTrue(furnace.countItem(Items.IRON_INGOT) == 1 && cloud.getContainer().countItem(Items.STONE) == 64, "Automatic output voided overflow");
        cloud.getContainer().clearContent();
        tick(helper, cloud, 8);
        helper.assertTrue(furnace.countItem(Items.IRON_INGOT) == 0 && cloud.getContainer().countItem(Items.IRON_INGOT) == 1, "Automatic output did not resume after making room");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taggedAutoFuelAndContainerRemainders(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(Items.COAL).is(CloudFurnace.AUTO_FUEL) && new ItemStack(Items.COAL_BLOCK).is(CloudFurnace.AUTO_FUEL)
                && !new ItemStack(Items.CHARCOAL).is(CloudFurnace.AUTO_FUEL), "Default fuel tag is wrong");
        var cloud = cloud(1, 4);
        var furnace = cloud.getFurnace();
        cloud.getContainer().setItem(0, new ItemStack(Items.COAL, 4));
        cloud.getContainer().setItem(1, new ItemStack(Items.CHARCOAL, 3));
        cloud.getContainer().setItem(2, new ItemStack(Items.COAL_BLOCK));
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        tick(helper, cloud, 8);
        helper.assertTrue(furnace.getItem(9).isEmpty() && cloud.getContainer().countItem(Items.COAL) == 4, "Automatic fuel started enabled");
        furnace.toggle(1);
        tick(helper, cloud, 8);
        helper.assertTrue(furnace.countItem(Items.COAL) == 3 && cloud.getContainer().countItem(Items.COAL) == 0
                && cloud.getContainer().countItem(Items.CHARCOAL) == 3 && cloud.getContainer().countItem(Items.COAL_BLOCK) == 1, "Automatic fuel ignored the tag or mixed fuel slots");
        var wet = cloud(1, 0);
        wet.getFurnace().setItem(0, new ItemStack(Items.WET_SPONGE));
        wet.getFurnace().setItem(9, new ItemStack(Items.LAVA_BUCKET));
        tick(helper, wet, 200);
        helper.assertTrue(wet.getFurnace().getItem(9).is(Items.WATER_BUCKET) && wet.getFurnace().getItem(18).is(Items.SPONGE), "Lava fuel or wet sponge bucket behavior lost a container");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void priorityFuelChoosesLargestCloudFuel(GameTestHelper helper) {
        var cloud = cloud(1, 4);
        var furnace = cloud.getFurnace();
        cloud.getContainer().setItem(0, new ItemStack(Items.COAL, 2));
        cloud.getContainer().setItem(1, new ItemStack(Items.CHARCOAL, 32));
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setPriorityFuel(true);
        tick(helper, cloud, 8);
        helper.assertTrue(furnace.getItem(9).is(Items.CHARCOAL) && furnace.getItem(9).getCount() == 31,
                "Priority fuel mode did not choose the largest valid fuel stack");
        helper.assertTrue(cloud.getContainer().countItem(Items.CHARCOAL) == 0 && cloud.getContainer().countItem(Items.COAL) == 2,
                "Priority fuel mode ignored total remaining quantities");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void progressPersistsAndRemovedInputsCannotDuplicate(GameTestHelper helper) {
        var cloud = cloud(2, 27);
        var furnace = cloud.getFurnace();
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 4));
        furnace.setItem(9, new ItemStack(Items.COAL, 2));
        tick(helper, cloud, 100);
        var saved = new CompoundTag();
        cloud.writeToNBT(saved, helper.getLevel().registryAccess());
        var restored = new CloudIndex(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.getFurnace().count() == 2 && restored.getFurnace().flags() == 0 && restored.getFurnace().progressScale() == 500, "Furnace count, settings or progress did not persist");
        tick(helper, restored, 100);
        helper.assertTrue(restored.getFurnace().countItem(Items.IRON_INGOT) == 2 && restored.getFurnace().countItem(Items.RAW_IRON) == 2, "Reloaded smelting restarted or duplicated a batch");
        var changed = cloud(4, 0);
        changed.getFurnace().setItem(0, new ItemStack(Items.RAW_IRON, 4));
        changed.getFurnace().setItem(9, new ItemStack(Items.COAL, 4));
        tick(helper, changed, 100);
        changed.getFurnace().removeItem(0, 3);
        tick(helper, changed, 100);
        helper.assertTrue(changed.getFurnace().countItem(Items.IRON_INGOT) == 1 && changed.getFurnace().countItem(Items.RAW_IRON) == 0, "Removing inputs duplicated in-flight recipes");
        helper.succeed();
    }

    private record Setup(Player player, CloudIndex cloud, AbstractCloudChestBlockEntity chest, BlockPos pos) { }
    private static Setup setup(GameTestHelper helper) {
        var player = player(helper);
        var cloud = new CloudIndex(player.getUUID(), UUID.randomUUID().hashCode() & 0xFFFFFF, 0);
        cloud.uploadFunction(CloudFunction.HOPPER);
        for (int i = 0; i < 16; i++) cloud.uploadFunction(CloudFunction.FURNACE);
        CSWorldData.get(helper.getLevel()).addPrivateCloud(cloud);
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
        var chest = (AbstractCloudChestBlockEntity) helper.getLevel().getBlockEntity(pos);
        chest.setBalloonColorFor(player, cloud.getBalloonColor());
        chest.automation().setMode(player, CloudIOSide.BOTTOM, 1);
        return new Setup(player, cloud, chest, pos);
    }

    @GameTest(template = "empty")
    public static void incomingSmeltingRespectsFacesAndSimulation(GameTestHelper helper) {
        var s = setup(helper);
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, s.pos, Direction.DOWN);
        helper.assertTrue(handler != null && handler.getSlots() == 0, "Incoming smelting started enabled");
        s.cloud.getFurnace().toggle(2);
        helper.assertTrue(handler.getSlots() == 1 && handler.insertItem(0, new ItemStack(Items.RAW_IRON, 8), true).isEmpty()
                && s.cloud.getFurnace().isEmpty(), "Zero-capacity furnace input simulation modified storage");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1, "A non-smeltable item entered the furnace");
        handler.insertItem(0, new ItemStack(Items.RAW_IRON, 8), false);
        helper.assertTrue(s.cloud.getFurnace().countItem(Items.RAW_IRON) == 8, "Automatic intake did not reach the input slots");
        s.chest.automation().setMode(s.player, CloudIOSide.BOTTOM, 2);
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.RAW_GOLD), false).getCount() == 1 && handler.extractItem(0, 1, false).isEmpty(), "Output face bypassed furnace input restrictions");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, s.pos, Direction.UP) == null, "Incoming smelting enabled top IO");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void menuTiersAndHiddenSlotProtection(GameTestHelper helper) {
        var player = player(helper);
        var cloud = new CloudIndex(player.getUUID(), 0xA68431, 27);
        cloud.uploadFunction(CloudFunction.FURNACE);
        cloud.getFurnace().setItem(0, new ItemStack(Items.RAW_IRON, 3));
        var menu = new CloudChestMenu(24, player.getInventory(), cloud, CSWorldData.get(helper.getLevel()));
        player.containerMenu = menu;
        menu.clicked(menu.furnaceStart(), 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty(), "Closed furnace slots remained clickable");
        menu.clickMenuButton(player, 102);
        menu.layoutSlots(1);
        helper.assertTrue(menu.slots.get(menu.furnaceStart()).isActive() && !menu.slots.get(menu.furnaceStart() + 1).isActive(), "Tier-one slots were not restricted");
        menu.clicked(menu.furnaceStart() + 1, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty(), "Hidden higher-tier slot accepted a click");
        menu.clickMenuButton(player, 400); menu.clickMenuButton(player, 401); menu.clickMenuButton(player, 402);
        helper.assertTrue(cloud.getFurnace().flags() == 7, "Furnace toggles failed");
        menu.clickMenuButton(player, 403);
        helper.assertTrue(cloud.getFurnace().fuelMode() == 2 && cloud.getFurnace().flags() == 13, "Priority fuel button did not select purple mode");
        menu.clickMenuButton(player, 401);
        helper.assertTrue(cloud.getFurnace().fuelMode() == 1 && cloud.getFurnace().flags() == 7, "Normal fuel toggle did not leave priority mode");
        menu.clicked(menu.furnaceStart(), 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 3 && cloud.getFurnace().getItem(0).isEmpty(), "Visible furnace inputs could not be retrieved");
        helper.assertTrue(menu.slots.get(menu.furnaceStart() + 9).getMaxStackSize(new ItemStack(Items.BUCKET, 64)) == 1,
                "Fuel slots accepted a stack of remainder buckets");
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        cloud.getFurnace().setItem(18, new ItemStack(Items.IRON_INGOT, 3));
        menu.quickMoveStack(player, menu.furnaceStart() + 18);
        helper.assertTrue(cloud.getContainer().countItem(Items.IRON_INGOT) == 3 && cloud.getFurnace().getItem(18).isEmpty(),
                "Shift output did not fall back to cloud storage when the inventory was full");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void realHopperAndBackgroundSmelting(GameTestHelper helper) {
        var s = setup(helper);
        s.chest.automation().setMode(s.player, CloudIOSide.LEFT, 1);
        s.cloud.getFurnace().toggle(2);
        s.cloud.getFurnace().setItem(9, new ItemStack(Items.COAL));
        var direction = s.chest.automation().front().getClockWise();
        var sourcePos = s.pos.relative(direction);
        helper.getLevel().setBlockAndUpdate(sourcePos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, direction.getOpposite()));
        var source = (Container) helper.getLevel().getBlockEntity(sourcePos);
        source.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        helper.runAfterDelay(240, () -> {
            int raw = source.countItem(Items.RAW_IRON) + s.cloud.getFurnace().countItem(Items.RAW_IRON);
            int cooked = s.cloud.getFurnace().countItem(Items.IRON_INGOT);
            helper.assertTrue(cooked > 0 && raw + cooked == 8, "Real hopper or closed-screen cloud processing lost items or failed to run");
            helper.succeed();
        });
    }
}
