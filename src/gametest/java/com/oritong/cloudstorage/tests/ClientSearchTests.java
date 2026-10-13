package com.oritong.cloudstorage.tests;

import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.network.MessageCloudSearchContents;
import com.oritong.cloudstorage.network.MessageCloudSearchMatches;
import com.oritong.cloudstorage.network.MessageRequestCloudSearch;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.BitSet;

@GameTestHolder("cloudstorage_reborn")
@PrefixGameTestTemplate(false)
public class ClientSearchTests {
    @GameTest(template = "empty")
    public static void clientMatchesWithoutJecOnServer(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var container = new SimpleContainer(90);
        for (int i = 0; i < 65; i++) container.setItem(i, new ItemStack(Items.GOLD_INGOT));
        container.setItem(80, new ItemStack(Items.DIAMOND, 7));
        var menu = new CloudChestMenu(9, player.getInventory(), container);
        var snapshot = menu.beginClientSearch(1, "zuanshi");
        helper.assertTrue(snapshot.items().size() == 66, "Search snapshot missed items beyond the visible page");

        for (int offset = 0; offset < snapshot.items().size(); offset += MessageCloudSearchContents.BATCH_SIZE) {
            var contents = new MessageCloudSearchContents(menu.containerId, 1, snapshot.search(), offset, snapshot.items().size(),
                    snapshot.items().subList(offset, Math.min(offset + MessageCloudSearchContents.BATCH_SIZE, snapshot.items().size())));
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                MessageCloudSearchContents.CODEC.encode(buffer, contents);
                var decoded = MessageCloudSearchContents.CODEC.decode(buffer);
                helper.assertTrue(decoded.offset() == offset && decoded.items().size() == contents.items().size(), "Search batches lost their positions");
                for (int i = 0; i < contents.items().size(); i++) {
                    helper.assertTrue(ItemStack.matches(decoded.items().get(i), contents.items().get(i)), "Search snapshot changed item data in transit");
                }
            } finally {
                buffer.release();
            }
        }

        var matches = new BitSet();
        matches.set(65);
        var request = new MessageRequestCloudSearch(9, 1, "zuanshi");
        var result = new MessageCloudSearchMatches(9, 1, matches);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            MessageRequestCloudSearch.CODEC.encode(buffer, request);
            helper.assertTrue(request.equals(MessageRequestCloudSearch.CODEC.decode(buffer)), "Client search request did not round-trip");
            buffer.clear();
            MessageCloudSearchMatches.CODEC.encode(buffer, result);
            helper.assertTrue(result.equals(MessageCloudSearchMatches.CODEC.decode(buffer)), "Client match positions did not round-trip");
        } finally {
            buffer.release();
        }

        helper.assertTrue(menu.finishClientSearch(player, 1, matches), "Server could not apply client matches");
        helper.assertTrue(container.getItem(0).is(Items.DIAMOND) && container.getItem(0).getCount() == 7, "Off-screen matching item was not moved to the front");
        helper.assertTrue(!menu.isSlotGray(0) && menu.isSlotGray(1), "Search highlighting disagreed with client matches");
        menu.search(player, "");
        helper.assertTrue(container.getItem(80).is(Items.DIAMOND) && container.getItem(0).is(Items.GOLD_INGOT), "Clearing client search did not restore original positions");

        menu.beginClientSearch(2, "zs");
        container.getItem(80).shrink(1);
        helper.assertTrue(!menu.finishClientSearch(player, 2, matches), "A stale snapshot was applied after the inventory changed");
        helper.assertTrue(container.getItem(80).getCount() == 6 && container.getItem(0).is(Items.GOLD_INGOT), "Rejected search changed the inventory");
        menu.beginClientSearch(3, "zs");
        helper.assertTrue(!menu.finishClientSearch(player, 2, matches), "Results from an older query were applied");
        helper.assertTrue(menu.finishClientSearch(player, 3, matches), "Refreshed snapshot could not be applied");
        menu.search(player, "");

        menu.beginClientSearch(4, "zs");
        matches.set(66);
        helper.assertTrue(!menu.finishClientSearch(player, 4, matches) && menu.getPendingClientSearch(4) == null, "Out-of-range match positions were accepted");
        menu.beginClientSearch(5, "zs");
        menu.search(player, "@minecraft");
        matches.clear(66);
        helper.assertTrue(!menu.finishClientSearch(player, 5, matches), "Normal search failed to cancel pending client matches");
        menu.search(player, "");
        helper.assertTrue(CloudChestMenu.matchesSearch(player, "@minecraft", new ItemStack(Items.DIAMOND)), "Mod ID search no longer works");
        helper.assertTrue(CloudChestMenu.matchesSearch(player, "#minecraft:planks", new ItemStack(Items.OAK_PLANKS)), "Tag search no longer works");
        menu.removed(player);
        helper.succeed();
    }
}
