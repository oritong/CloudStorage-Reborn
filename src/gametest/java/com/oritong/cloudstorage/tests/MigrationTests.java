package com.oritong.cloudstorage.tests;

import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.block.StaticCloudChestBlockEntity;
import com.oritong.cloudstorage.entity.CSEntityRegistry;
import com.oritong.cloudstorage.item.*;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder("cloudstorage_reborn")
@PrefixGameTestTemplate(false)
public class MigrationTests {
    @GameTest(template = "empty")
    public static void storageRoundTrip(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var data = CSWorldData.get(helper.getLevel());
        var uuid = UUID.randomUUID();
        var cloud = new CloudIndex(uuid, 0x123456, 27);
        ItemStack balloon = ((BalloonItem) CSItemRegistry.BALLOON.get()).createBalloon(0x13579B, 1);
        cloud.getContainer().setItem(20, balloon);
        ItemStack named = new ItemStack(Items.DIAMOND, 5);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Stored diamond"));
        cloud.getContainer().setItem(2, named);
        data.addPrivateCloud(cloud);
        data.setDirty();
        var restoredData = CSWorldData.load(data.save(new CompoundTag(), registries), registries);
        var restored = restoredData.getPrivateCloud(uuid, 0x123456);
        helper.assertTrue(restored != null, "Private cloud did not survive serialization");
        helper.assertTrue(ItemStack.isSameItemSameComponents(balloon, restored.getContainer().getItem(20)), "Balloon components changed after saving");
        helper.assertTrue(restored.getContainer().getItem(2).getCount() == 5, "Stored item count changed");
        helper.assertTrue(restored.getContainer().getItem(2).getHoverName().getString().equals("Stored diamond"), "Stored item name changed");
        restored.resize(54);
        helper.assertTrue(BalloonItem.getBalloonColor(restored.getContainer().getItem(20)) == 0x13579B, "Expanding cloud lost its contents");
        CompoundTag old = new CompoundTag();
        old.putString("id", "cloudstorage_reborn:balloon");
        old.putByte("Count", (byte) 3);
        CompoundTag oldTag = new CompoundTag();
        CompoundTag display = new CompoundTag();
        display.putInt("color", 0x2468AC);
        oldTag.put("display", display);
        oldTag.putBoolean("static", true);
        old.put("tag", oldTag);
        var migrated = StackData.load(registries, old);
        helper.assertTrue(migrated.getCount() == 3 && BalloonItem.getBalloonColor(migrated) == 0x2468AC && BalloonItem.isStatic(migrated), "Legacy cloud item migration lost item state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void automationAndEntities(GameTestHelper helper) {
        int color = UUID.randomUUID().hashCode() & 0xFFFFFF;
        var cloud = new CloudIndex(UUID.randomUUID(), color, 27);
        cloud.unlock(com.oritong.cloudstorage.misc.CloudFunction.HOPPER);
        CSWorldData.get(helper.getLevel()).addPublicCloud(cloud);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, CSBlockRegistry.STATIC_CLOUD_CHEST.get().defaultBlockState());
        var chest = (StaticCloudChestBlockEntity) helper.getLevel().getBlockEntity(pos);
        chest.setBalloonColorFor(null, color);
        chest.automation().setMode(helper.makeMockPlayer(GameType.SURVIVAL), com.oritong.cloudstorage.inventory.CloudIOSide.BOTTOM, 3);
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        helper.assertTrue(handler != null && handler.getSlots() == 27, "Static chest capability is missing");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(), "Automation rejected item insertion");
        helper.assertTrue(cloud.getContainer().getItem(0).getCount() == 8, "Automation wrote to the wrong inventory");
        helper.assertTrue(handler.extractItem(0, 3, false).getCount() == 3, "Automation extraction failed");
        cloud.resize(54);
        helper.assertTrue(handler.getSlots() == 54, "Cached automation handler did not see cloud expansion");
        helper.assertTrue(handler.insertItem(53, new ItemStack(Items.GOLD_INGOT), false).isEmpty(), "Expanded automation inventory is stale");
        helper.assertTrue(cloud.getContainer().getItem(53).is(Items.GOLD_INGOT), "Expanded slot write was lost");
        helper.assertTrue(CSEntityRegistry.BALLOON.get().create(helper.getLevel()) != null, "Balloon synchronization failed");
        helper.assertTrue(CSEntityRegistry.BADLOON.get().create(helper.getLevel()) != null, "Badloon synchronization failed");
        helper.assertTrue(CSEntityRegistry.BLOVIATOR.get().create(helper.getLevel()) != null, "Bloviator synchronization failed");
        helper.assertTrue(CSEntityRegistry.BALLOON_BUDDY.get().create(helper.getLevel()) != null, "Buddy synchronization failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemStateAndRecipes(GameTestHelper helper) {
        var level = helper.getLevel();
        var balloon = ((BalloonItem) CSItemRegistry.BALLOON.get()).createBalloon(0x123ABC, 0);
        var grid = new ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) grid.add(i == 4 ? balloon : new ItemStack(Items.ARROW));
        var input = CraftingInput.of(3, 3, grid);
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
        var arrows = recipe.value().assemble(input, level.registryAccess());
        helper.assertTrue(arrows.is(CSItemRegistry.BALLOON_ARROW.get()) && arrows.getCount() == 8, "Balloon arrow recipe failed");
        helper.assertTrue(BalloonItem.getBalloonColor(arrows) == 0x123ABC, "Balloon arrow recipe lost its color");
        var dyeInput = CraftingInput.of(2, 1, List.of(balloon, new ItemStack(Items.BLUE_DYE)));
        helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, dyeInput, level).isPresent(), "Balloon dye recipe is missing");
        var dyeRecipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, dyeInput, level).orElseThrow();
        var dyed = dyeRecipe.value().assemble(dyeInput, level.registryAccess());
        helper.assertTrue(dyed.has(DataComponents.DYED_COLOR) && BalloonItem.getBalloonColor(dyed) != 0x123ABC, "Balloon dye recipe did not write a color component");
        var lootBalloon = ((BalloonItem) CSItemRegistry.BALLOON.get()).createBalloon(-1, 2);
        var lootDye = CraftingInput.of(2, 1, List.of(lootBalloon, new ItemStack(Items.BLUE_DYE)));
        helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, lootDye, level).isEmpty(), "Loot balloons should not be dyeable");
        var blower = new ItemStack(CSItemRegistry.CLOUD_BLOWER.get());
        CloudBlowerItem.setMode(blower, 1);
        CloudBlowerItem.setFuel(blower, 42);
        var copy = ItemStack.parseOptional(level.registryAccess(), (CompoundTag) blower.save(level.registryAccess()));
        helper.assertTrue(CloudBlowerItem.getMode(copy) == 1 && CloudBlowerItem.getFuel(copy) == 42, "Blower state was not saved");
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).containsKey(ResourceLocation.parse("cloudstorage_reborn:drift")), "Drift song was not loaded");
        helper.assertTrue(level.getServer().getAdvancements().get(ResourceLocation.parse("cloudstorage_reborn:cloudstorage_reborn/root")) != null, "Advancements did not load");
        helper.assertTrue(level.getServer().getAdvancements().getAllAdvancements().stream().filter(a -> a.id().getNamespace().equals("cloudstorage_reborn")).count() == 20, "Some mod advancements did not load");
        var buddyAdvancement = level.getServer().getAdvancements().get(ResourceLocation.parse("cloudstorage_reborn:cloudstorage_reborn/all_balloon_buddies")).value();
        for (int personality = 3; personality <= 10; personality++) {
            var buddy = new ItemStack(CSItemRegistry.BALLOON_BUDDY.get());
            int face = personality;
            StackData.update(buddy, tag -> tag.putInt("Personality", face));
            var criterion = (net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance) buddyAdvancement.criteria().get("personality" + personality).triggerInstance();
            helper.assertTrue(criterion.items().getFirst().test(buddy), "Buddy advancement no longer matches its personality data");
        }
        helper.assertTrue(!level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, BalloonItem.LOOT_TABLE)).getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST)).isEmpty(), "Loot balloon table did not load");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void menusAndWorldResources(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        int color = UUID.randomUUID().hashCode() & 0xFFFFFF;
        var cloud = new CloudIndex(player.getUUID(), color, 63);
        CSWorldData.get(level).addPrivateCloud(cloud);
        var container = cloud.getContainer();
        container.setItem(0, new ItemStack(Items.DIAMOND, 7));
        container.setItem(3, new ItemStack(Items.DIAMOND, 5));
        container.setItem(10, ((BalloonItem) CSItemRegistry.BALLOON.get()).createBalloon(0x123456, 0));
        container.setItem(40, ((BalloonItem) CSItemRegistry.BALLOON.get()).createBalloon(0x654321, 0));
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, CSBlockRegistry.CLOUD_CHEST.get().defaultBlockState());
        var chest = (com.oritong.cloudstorage.block.CloudChestBlockEntity) level.getBlockEntity(pos);
        chest.setBalloonColorFor(player, color);
        var menu = (com.oritong.cloudstorage.inventory.CloudChestMenu) chest.getMenu(1, player.getInventory());
        helper.assertTrue(menu != null && menu.slots.stream().filter(slot -> slot instanceof com.oritong.cloudstorage.inventory.SlotCloudChest).count() == 54
                && menu.craftingStart() == 90, "Private cloud menu did not open with 54 storage slots and 36 player slots");
        menu.sort(com.oritong.cloudstorage.inventory.ItemSorting::defaultCompare);
        int diamonds = 0;
        int balloons = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);
            if (stack.is(Items.DIAMOND)) diamonds += stack.getCount();
            if (stack.is(CSItemRegistry.BALLOON.get())) balloons++;
        }
        helper.assertTrue(diamonds == 12 && balloons == 2, "Sorting lost items or merged differently colored balloons");
        menu.search(player, "@cloudstorage_reborn");
        menu.search(player, "");
        menu.scrollTo(1, false);
        helper.assertTrue(menu.getScrollAmount() == 1, "Large cloud menu did not scroll");
        menu.removed(player);
        for (var name : List.of("big_balloon_0", "big_balloon_1", "big_balloon_2", "sky_temple_0", "sky_temple_1", "sky_temple_2", "balloon_stand_plains", "balloon_stand_desert", "balloon_stand_savanna", "balloon_stand_snowy", "balloon_stand_taiga")) {
            var template = level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath("cloudstorage_reborn", name));
            helper.assertTrue(template.isPresent() && template.get().getSize().getY() > 0, "Structure template failed to load: " + name);
        }
        helper.succeed();
    }
}
