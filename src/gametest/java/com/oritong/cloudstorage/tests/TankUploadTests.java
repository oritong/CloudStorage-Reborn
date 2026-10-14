package com.oritong.cloudstorage.tests;

import com.mojang.authlib.GameProfile;
import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.block.CloudChestBlockEntity;
import com.oritong.cloudstorage.block.StaticCloudChestBlockEntity;
import com.oritong.cloudstorage.compat.BeyondDimensionsCompat;
import com.oritong.cloudstorage.compat.CloudTankUpload;
import com.oritong.cloudstorage.entity.BalloonCargoEntity;
import com.oritong.cloudstorage.entity.BalloonEntity;
import com.oritong.cloudstorage.entity.CSEntityRegistry;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.item.BalloonItem;
import com.oritong.cloudstorage.item.CSItemRegistry;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

@GameTestHolder("cloudstorage_reborn")
@PrefixGameTestTemplate(false)
public final class TankUploadTests {
    private static Player player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "TankTester"));
    }

    @GameTest(template = "empty")
    public static void configurationAndLegacyFluids(GameTestHelper helper) {
        var player = player(helper);
        var cloud = new CloudIndex(player.getUUID(), 0xDE3478, 0);
        long quantum = 2_147_483_647_000L;
        long contents = 2_147_484_647L;
        helper.assertTrue(cloud.addFluidStorage(quantum, new FluidStack(Fluids.WATER, 1), contents)
                && cloud.getContainerSize() == 0 && cloud.getFluids().capacity() == quantum
                && cloud.getFluids().typeCapacity() == 1 && cloud.getFluids().storedAmount() == contents,
                "A tank-only cloud lost long capacity, contents or its type quota");
        var data = CSWorldData.get(helper.getLevel());
        data.addPrivateCloud(cloud);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
        var chest = (CloudChestBlockEntity) helper.getLevel().getBlockEntity(pos);
        chest.setBalloonColorFor(player, cloud.getBalloonColor());
        helper.assertTrue(!chest.hasNoInvSpace(player) && chest.getMenu(19, player.getInventory()) instanceof CloudChestMenu,
                "A cloud containing only uploaded tanks could not open its terminal");
        boolean enabled = CloudStorage.CONFIG.fluidStorage.get();
        try {
            CloudStorage.CONFIG.fluidStorage.set(false);
            var menu = new CloudChestMenu(20, player.getInventory(), cloud, data);
            player.containerMenu = menu;
            menu.broadcastChanges();
            var destination = new FluidTank(2000);
            helper.assertTrue(cloud.getFluids().fill(new FluidStack(Fluids.WATER, 1000), EXECUTE) == 0
                    && cloud.getFluids().drain(1000, EXECUTE).isEmpty()
                    && BeyondDimensionsCompat.transferFluids(cloud.getFluids(), destination) == 0
                    && destination.getFluidAmount() == 0 && menu.getTerminalEntries().isEmpty()
                    && menu.getFluidCapacity() == 0 && menu.getFluidTypeCapacity() == 0,
                    "Disabled fluid storage remained usable or copied fluids to Beyond Dimensions");
            var saved = new CompoundTag();
            cloud.writeToNBT(saved, helper.getLevel().registryAccess());
            var restored = new CloudIndex(saved, helper.getLevel().registryAccess());
            helper.assertTrue(restored.getFluids().storedAmount() == contents && restored.getFluids().capacity() == quantum,
                    "Disabling and saving deleted fluid data");
            CloudStorage.CONFIG.fluidStorage.set(true);
            menu.broadcastChanges();
            helper.assertTrue(menu.getTerminalEntries().size() == 1 && menu.getFluidTypeCapacity() == 1
                    && restored.getFluids().drain(1000, EXECUTE).getAmount() == 1000,
                    "Re-enabling storage failed to restore access");
            saved.remove("FluidCapacity");
            saved.remove("FluidTypes");
            saved.putInt("Size", 27);
            var legacy = new CloudIndex(saved, helper.getLevel().registryAccess());
            helper.assertTrue(legacy.getFluids().capacity() == 0 && legacy.getFluids().typeCapacity() == 0
                    && legacy.getFluids().storedAmount() == contents && legacy.getFluids().fill(new FluidStack(Fluids.WATER, 1), EXECUTE) == 0
                    && legacy.getFluids().drain(1000, EXECUTE).getAmount() == 1000,
                    "Legacy chest capacity remained active or existing fluids became unrecoverable");
        } finally { CloudStorage.CONFIG.fluidStorage.set(enabled); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestUploadsOnlyAddItems(GameTestHelper helper) throws ReflectiveOperationException {
        var player = player(helper);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ((net.minecraft.world.Container) helper.getLevel().getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 5));
        var balloon = attach(helper, player, pos, 0xC15678, false);
        finish(balloon);
        var cloud = CSWorldData.get(helper.getLevel()).getPrivateCloud(player.getUUID(), 0xC15678);
        helper.assertTrue(cloud != null && cloud.getContainerSize() == 27 && cloud.getContainer().countItem(Items.DIAMOND) == 5
                && cloud.getFluids().capacity() == 0 && cloud.getFluids().typeCapacity() == 0,
                "A chest upload granted fluid space or lost its item contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void modernIndustrializationTanks(GameTestHelper helper) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("modern_industrialization")) { helper.succeed(); return; }
        var player = player(helper);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var data = CSWorldData.get(helper.getLevel());
        String[] tiers = {"iron", "bronze", "steel", "aluminum", "stainless_steel", "titanium", "quantum"};
        long expectedCapacity = 0;
        long expectedAmount = 0;
        for (int i = 0; i < tiers.length; i++) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:" + tiers[i] + "_tank"));
            helper.assertTrue(block != Blocks.AIR && block.defaultBlockState().is(CloudTankUpload.TAG), "MI tank missing from upload tag: " + tiers[i]);
            helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var be = helper.getLevel().getBlockEntity(pos);
            long capacity = ((Number) be.getClass().getMethod("getCapacity").invoke(be)).longValue();
            var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            long amount = handler.fill(new FluidStack(i == 1 ? Fluids.LAVA : Fluids.WATER,
                    i == 6 ? Integer.MAX_VALUE : 1000), EXECUTE);
            if (i == 6) {
                amount += handler.fill(new FluidStack(Fluids.WATER, 1000), EXECUTE);
                helper.assertTrue(capacity > Integer.MAX_VALUE && amount > Integer.MAX_VALUE, "Quantum tank long-value fixture failed");
            }
            if (i == 0) {
                boolean enabled = CloudStorage.CONFIG.fluidStorage.get();
                try {
                    CloudStorage.CONFIG.fluidStorage.set(false);
                    var stack = ((BalloonItem) CSItemRegistry.BALLOON.get()).createBalloon(0xA72345, 0);
                    helper.assertTrue(!((BalloonItem) stack.getItem()).placeBalloon(helper.getLevel(), stack, pos, Direction.UP, player, false)
                            && stack.getCount() == 1 && helper.getLevel().getBlockState(pos).is(block), "Disabled tank upload consumed a tank or balloon");
                } finally { CloudStorage.CONFIG.fluidStorage.set(enabled); }
            }
            var balloon = attach(helper, player, pos, 0xA72345, false);
            var cargo = (BalloonCargoEntity) balloon.getChild();
            var saved = new CompoundTag();
            cargo.saveWithoutId(saved);
            var restoredCargo = CSEntityRegistry.BALLOON_CARGO.get().create(helper.getLevel());
            restoredCargo.load(saved);
            helper.assertTrue(restoredCargo.getFluidCapacity() == capacity && restoredCargo.getFluidAmount() == amount
                    && restoredCargo.blockData != null, "In-flight tank data did not survive serialization");
            finish(balloon);
            expectedCapacity += capacity;
            expectedAmount += amount;
            var cloud = data.getPrivateCloud(player.getUUID(), 0xA72345);
            helper.assertTrue(cloud != null && cloud.getContainerSize() == 0 && cloud.getFluids().capacity() == expectedCapacity
                    && cloud.getFluids().typeCapacity() == i + 1 && cloud.getFluids().storedAmount() == expectedAmount && cargo.isRemoved(),
                    "MI tank upload lost capacity, type quota or contents: " + tiers[i]);
            balloon.discard();
        }
        var cloud = data.getPrivateCloud(player.getUUID(), 0xA72345);
        var restored = CSWorldData.load(data.save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess())
                .getPrivateCloud(player.getUUID(), 0xA72345);
        helper.assertTrue(restored.getFluids().capacity() == expectedCapacity && restored.getFluids().typeCapacity() == 7
                && restored.getFluids().storedAmount() == expectedAmount && cloud.getFluids().snapshot().size() == 2,
                "Uploaded MI tanks failed the world-save round trip");
        var iron = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:iron_tank"));
        helper.getLevel().setBlockAndUpdate(pos, iron.defaultBlockState());
        long ironCapacity = ((Number) helper.getLevel().getBlockEntity(pos).getClass().getMethod("getCapacity")
                .invoke(helper.getLevel().getBlockEntity(pos))).longValue();
        int sharedColor = unusedPublicColor(data);
        finish(attach(helper, player, pos, sharedColor, true));
        var shared = data.getPublicCloud(sharedColor);
        helper.getLevel().setBlockAndUpdate(pos, CSBlockRegistry.STATIC_CLOUD_CHEST.get().defaultBlockState());
        var chest = (StaticCloudChestBlockEntity) helper.getLevel().getBlockEntity(pos);
        chest.setBalloonColorFor(null, sharedColor);
        helper.assertTrue(shared != null && shared.getFluids().capacity() == ironCapacity && shared.getFluids().typeCapacity() == 1
                && !chest.hasNoInvSpace(player) && chest.getMenu(21, player.getInventory()) instanceof CloudChestMenu,
                "Static tank upload failed to create an accessible public fluid cloud");
        CloudStorage.LOGGER.info("MI_TANK_UPLOAD_TEST_PASSED: seven tiers, long quantum capacity and contents, in-flight saves, private/static clouds, disabled config");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mekanismTanks(GameTestHelper helper) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("mekanism")) { helper.succeed(); return; }
        var player = player(helper);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var data = CSWorldData.get(helper.getLevel());
        int color = 0xC93456;
        String[] tiers = {"basic", "advanced", "elite", "ultimate"};
        long expectedCapacity = 0;
        long expectedAmount = 0;
        int expectedBuckets = 0;
        for (int i = 0; i < tiers.length; i++) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:" + tiers[i] + "_fluid_tank"));
            helper.assertTrue(block != Blocks.AIR && block.defaultBlockState().is(CloudTankUpload.TAG), "Mekanism tank missing from upload tag: " + tiers[i]);
            helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var be = helper.getLevel().getBlockEntity(pos);
            var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            helper.assertTrue(handler != null && handler.getTanks() == 1, "Mekanism tank did not expose its standard fluid capability");
            int capacity = handler.getTankCapacity(0);
            var empty = CloudTankUpload.capture(helper.getLevel(), pos);
            helper.assertTrue(empty != null && empty.capacity() == capacity && empty.amount() == 0, "Empty Mekanism tank capacity was lost");
            var input = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
            int amount = i == 2 ? 0 : input.fill(new FluidStack(i == 1 ? Fluids.LAVA : Fluids.WATER, (i + 1) * 1000), EXECUTE);
            helper.assertTrue(i == 2 || amount == (i + 1) * 1000, "Mekanism fluid fixture did not fill: " + tiers[i] + ", accepted " + amount);
            be.getClass().getMethod("setStackInSlot", int.class, ItemStack.class, Direction.class)
                    .invoke(be, 1, new ItemStack(Items.BUCKET, i + 2), null);
            if (i == 0) {
                boolean enabled = CloudStorage.CONFIG.fluidStorage.get();
                try {
                    CloudStorage.CONFIG.fluidStorage.set(false);
                    var stack = ((BalloonItem) CSItemRegistry.BALLOON.get()).createBalloon(color, 0);
                    helper.assertTrue(!((BalloonItem) stack.getItem()).placeBalloon(helper.getLevel(), stack, pos, Direction.UP, player, false)
                            && stack.getCount() == 1 && helper.getLevel().getBlockState(pos).is(block), "Disabled Mekanism upload consumed storage");
                } finally { CloudStorage.CONFIG.fluidStorage.set(enabled); }
            }
            var balloon = attach(helper, player, pos, color, false);
            var cargo = (BalloonCargoEntity) balloon.getChild();
            var saved = new CompoundTag();
            cargo.saveWithoutId(saved);
            var restoredCargo = CSEntityRegistry.BALLOON_CARGO.get().create(helper.getLevel());
            restoredCargo.load(saved);
            helper.assertTrue(restoredCargo.getFluidCapacity() == capacity && restoredCargo.getFluidAmount() == amount
                    && restoredCargo.getContainerItems().stream().filter(stack -> stack.is(Items.BUCKET)).mapToInt(ItemStack::getCount).sum() == i + 2,
                    "In-flight Mekanism contents or container-slot items failed serialization");
            finish(balloon);
            expectedCapacity += capacity;
            expectedAmount += amount;
            expectedBuckets += i + 2;
            var cloud = data.getPrivateCloud(player.getUUID(), color);
            int droppedBuckets = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4),
                    entity -> entity.getItem().is(Items.BUCKET)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(cloud != null && cloud.getContainerSize() == 0 && cloud.getFluids().capacity() == expectedCapacity
                    && cloud.getFluids().typeCapacity() == i + 1 && cloud.getFluids().storedAmount() == expectedAmount
                    && droppedBuckets + player.getInventory().countItem(Items.BUCKET) == expectedBuckets && cargo.isRemoved(),
                    "Mekanism upload lost capacity, fluids or container-slot items: " + tiers[i]);
            finish(balloon);
            helper.assertTrue(cloud.getFluids().typeCapacity() == i + 1, "Mekanism tank uploaded twice");
            balloon.discard();
        }
        var cloud = data.getPrivateCloud(player.getUUID(), color);
        var restored = CSWorldData.load(data.save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess())
                .getPrivateCloud(player.getUUID(), color);
        helper.assertTrue(restored.getFluids().capacity() == expectedCapacity && restored.getFluids().typeCapacity() == 4
                && restored.getFluids().storedAmount() == expectedAmount && cloud.getFluids().snapshot().size() == 2,
                "Uploaded Mekanism tanks failed the world-save round trip");
        var basic = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:basic_fluid_tank"));
        helper.getLevel().setBlockAndUpdate(pos, basic.defaultBlockState());
        var be = helper.getLevel().getBlockEntity(pos);
        be.getClass().getMethod("setStackInSlot", int.class, ItemStack.class, Direction.class)
                .invoke(be, 1, new ItemStack(Items.BUCKET, 3), null);
        int sharedColor = unusedPublicColor(data);
        data.addPublicCloud(new CloudIndex(player.getUUID(), sharedColor, 27));
        finish(attach(helper, player, pos, sharedColor, true));
        var shared = data.getPublicCloud(sharedColor);
        helper.assertTrue(shared.getFluids().typeCapacity() == 1 && shared.getContainerSize() == 27
                && shared.getContainer().countItem(Items.BUCKET) == 3, "Static Mekanism tank upload did not preserve container-slot items in existing item storage");
        var creative = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:creative_fluid_tank"));
        helper.assertTrue(!creative.defaultBlockState().is(CloudTankUpload.TAG), "Creative Mekanism tank was included in the four survival tiers");
        CloudStorage.LOGGER.info("MEKANISM_TANK_UPLOAD_TEST_PASSED: four tiers, actual capacity, empty/filled tanks, container-slot items, in-flight/world saves, config and static clouds");
        helper.succeed();
    }

    private static int unusedPublicColor(CSWorldData data) {
        int color;
        do { color = java.util.concurrent.ThreadLocalRandom.current().nextInt(0x1000000); }
        while (data.getPublicCloud(color) != null);
        return color;
    }

    private static BalloonEntity attach(GameTestHelper helper, Player player, BlockPos pos, int color, boolean charged) {
        var stack = ((BalloonItem) CSItemRegistry.BALLOON.get()).createBalloon(color, charged ? 1 : 0);
        helper.assertTrue(((BalloonItem) stack.getItem()).placeBalloon(helper.getLevel(), stack, pos, Direction.UP, player, false)
                && stack.isEmpty(), "Balloon did not attach to uploaded storage");
        var cargo = helper.getLevel().getEntitiesOfClass(BalloonCargoEntity.class, new AABB(pos).inflate(2),
                entity -> !entity.isRemoved() && player.getUUID().equals(entity.getPlayerUUID())).getFirst();
        return (BalloonEntity) cargo.getBalloon();
    }

    private static void finish(BalloonEntity balloon) throws ReflectiveOperationException {
        var upload = BalloonEntity.class.getDeclaredMethod("upload");
        upload.setAccessible(true);
        upload.invoke(balloon);
    }
}
