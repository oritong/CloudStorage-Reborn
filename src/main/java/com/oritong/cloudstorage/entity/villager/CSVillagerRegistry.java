package com.oritong.cloudstorage.entity.villager;


import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.block.CSPOIRegistry;
import com.oritong.cloudstorage.entity.BalloonEntity;
import com.oritong.cloudstorage.entity.CSEntityRegistry;
import com.oritong.cloudstorage.item.BalloonItem;
import com.oritong.cloudstorage.item.CSItemRegistry;
import com.oritong.cloudstorage.misc.CSSoundRegistry;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.behavior.MoveToSkySeeingSpot;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.function.Predicate;

public class CSVillagerRegistry {

    public static final DeferredRegister<VillagerProfession> DEF_REG = DeferredRegister.create(Registries.VILLAGER_PROFESSION, CloudStorage.MODID);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> BALLOON_SALESMAN = DEF_REG.register("balloon_salesman", () -> buildVillagerProfession());

    public static void initTrades(List<VillagerTrades.ItemListing> level1, List<VillagerTrades.ItemListing> level2, List<VillagerTrades.ItemListing> level3, List<VillagerTrades.ItemListing> level4, List<VillagerTrades.ItemListing> level5) {
        level1.add(new BuyingItemTrade(Items.STRING, 10, 3, 11, 2));
        level1.add(new SellingItemTrade(CSItemRegistry.BALLOON_BIT.get(), 2, 3, 13, 2));
        level1.add(new SellingItemTrade(CSItemRegistry.COTTON_CANDY.get(), 2, 7, 13, 3));
        level1.add(new BuyingItemTrade(Items.GLASS_BOTTLE, 8, 2, 11, 2));
        level2.add(new BuyingItemTrade(CSItemRegistry.GUIDE_BOOK.get(), 8, 2, 11, 2));
        level2.add(new SellingItemTrade(CSItemRegistry.BALLOON.get(), 2, 5, 13, 4));
        level2.add(new SellingRandomlyDyedItem(CSItemRegistry.BALLOON.get(), 1, 2, 13, 4));
        level2.add(new SellingRandomlyDyedItem(CSItemRegistry.BALLOON.get(), 4, 2, 13, 4));
        level3.add(new SellingItemTrade(CSBlockRegistry.CLOUD.get(), 2, 4, 11, 8));
        level3.add(new BuyingItemTrade(CSItemRegistry.ANGRY_CLOUD_IN_A_BOTTLE.get(), 8, 4, 4, 9));
        level3.add(new SellingItemTrade(Items.FIREWORK_STAR, 3, 9, 4, 9));
        level3.add(new SellingItemTrade(Items.CYAN_BANNER, 3, 4, 4, 9));
        level3.add(new SellingItemTrade(Items.SPYGLASS, 5, 1, 2, 9));
        level3.add(new SellingMap(10, true, "item.cloudstorage_reborn.big_balloon_map", net.minecraft.world.level.saveddata.maps.MapDecorationTypes.TARGET_POINT, 1, 13));
        level3.add(new SellingMap(10, false, "item.cloudstorage_reborn.sky_temple_map", net.minecraft.world.level.saveddata.maps.MapDecorationTypes.TARGET_X, 1, 13));
        level4.add(new SellingItemTrade(CSItemRegistry.PROPELLER_HAT.get(), 11, 1, 1, 15));
        level4.add(new SellingItemTrade(CSBlockRegistry.STATIC_CLOUD.get(), 3, 5, 6, 13));
        level4.add(new BuyingItemTrade(CSItemRegistry.HAPPY_CLOUD_IN_A_BOTTLE.get(), 8, 6, 7, 12));
        level5.add(new SellingItemTrade(CSItemRegistry.BALLOON_BUDDY.get(), 15, 1, 10, 15));
        level5.add(new SellingRandomlyDyedItem(CSItemRegistry.BALLOON_BUDDY.get(), 13, 1, 3, 15));
        level5.add(new SellingRandomlyDyedItem(CSItemRegistry.BALLOON.get(), 6, 2, 13, 5));
    }


    public static void onBalloonCelebrate(ServerLevel level, Villager villager) {
        RandomSource random = villager.getRandom();
        if (random.nextInt(75) == 0) {
            villager.playCelebrateSound();
            if(MoveToSkySeeingSpot.hasNoBlocksAbove(level, villager, villager.blockPosition())) {
                int[] colors = getBalloonColorsForVillager(villager.getVillagerData().getType());
                for (int i = 0; i < random.nextInt(2) + 1; i++) {
                    int color = colors[colors.length <= 1 ? 0 : random.nextInt(colors.length)];
                    BalloonEntity balloon = CSEntityRegistry.BALLOON.get().create(villager.level());
                    balloon.copyPosition(villager);
                    balloon.setStringLength(BalloonEntity.DEFAULT_STRING_LENGTH + random.nextInt(1));
                    balloon.setBalloonColor(color);
                    villager.level().addFreshEntity(balloon);
                }
            }
        }
    }

    public static int[] getBalloonColorsForVillager(VillagerType type) {
        if (type == VillagerType.PLAINS) {
            return new int[]{BalloonItem.DEFAULT_COLOR, 0XFED83D};
        } else if (type == VillagerType.DESERT) {
            return new int[]{0X80C71F, 0X3AB3DA, 0XFED83D};
        } else if (type == VillagerType.SAVANNA) {
            return new int[]{0XFED83D, 0X835432, 0XF9801D};
        } else if (type == VillagerType.SNOW) {
            return new int[]{0X3AB3DA, 0X3C44AA};
        } else if (type == VillagerType.TAIGA) {
            return new int[]{BalloonItem.DEFAULT_COLOR, 0X3AB3DA};
        } else {
            return new int[]{BalloonItem.DEFAULT_COLOR};
        }
    }

    private static VillagerProfession buildVillagerProfession() {
        Predicate<Holder<PoiType>> heldJobSite = (poiType) -> {
            return poiType.is(CSPOIRegistry.BALLOON_STAND.getKey());
        };
        Predicate<Holder<PoiType>> acquirableJobSite = (poiType) -> {
            return poiType.is(CSPOIRegistry.BALLOON_STAND.getKey());
        };
        if(!CloudStorage.CONFIG.balloonSalesmanVillager.getDefault()){
            heldJobSite = Predicates.alwaysFalse();
            acquirableJobSite = Predicates.alwaysFalse();
        }
        return new VillagerProfession("balloon_salesman", heldJobSite, acquirableJobSite, ImmutableSet.of(), ImmutableSet.of(), CSSoundRegistry.BALLOON_HURT.get());
    }

}
