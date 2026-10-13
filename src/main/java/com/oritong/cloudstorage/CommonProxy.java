package com.oritong.cloudstorage;

import com.oritong.cloudstorage.command.RetrieveBalloonsCommand;
import com.oritong.cloudstorage.entity.*;
import com.oritong.cloudstorage.entity.villager.CSVillagerRegistry;
import com.oritong.cloudstorage.inventory.CloudChestMenu;
import com.oritong.cloudstorage.inventory.ItemSorting;
import com.oritong.cloudstorage.item.BalloonItem;
import com.oritong.cloudstorage.item.CSItemRegistry;
import com.oritong.cloudstorage.misc.CSAdvancementTrigger;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.misc.CloudInfo;
import com.oritong.cloudstorage.network.MessageUpdateCloudInfo;
import com.oritong.cloudstorage.network.MessageCloudSearchContents;
import com.oritong.cloudstorage.world.CSWorldData;
import com.oritong.cloudstorage.world.SkyMobSpawner;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.*;

public class CommonProxy {


    public static CSAdvancementTrigger UPLOAD_TRIGGER = new CSAdvancementTrigger(ResourceLocation.parse("cloudstorage_reborn:upload"));
    public static CSAdvancementTrigger LUFTBALLONS_TRIGGER = new CSAdvancementTrigger(ResourceLocation.parse("cloudstorage_reborn:luftballons"));

    private Random random = new Random();
    private static final Map<ServerLevel, SkyMobSpawner> SKY_MOB_SPAWNER_MAP = new HashMap<ServerLevel, SkyMobSpawner>();
    public void clientInit() {
    }

    public void init() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onProjectileImpact);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onMobAttemptSpawn);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::registerCommands);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onMobVisiblity);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onVillagerTrades);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onServerTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.oritong.cloudstorage.compat.BeyondDimensionsCompat::onServerTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.oritong.cloudstorage.compat.BeyondDimensionsCompat::onServerStopped);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onPlayerCraft);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onPlayerPickup);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onPlayerOpenContainer);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onEntitySetTarget);
        var triggers = DeferredRegister.create(Registries.TRIGGER_TYPE, CloudStorage.MODID);
        triggers.register("upload", () -> UPLOAD_TRIGGER);
        triggers.register("luftballons", () -> LUFTBALLONS_TRIGGER);
        triggers.register(CloudStorage.MOD_BUS);
    }

    public Object getISTERProperties(boolean armor) {
        return null;
    }


    public void setVisibleCloudSlots(int i){
    }

    public int getVisibleCloudSlots(){
        return 0;
    }

    public Player getClientSidePlayer() {
        return null;
    }

    public boolean matchesSearchText(String text, String query) {
        return text.contains(query);
    }

    public void receiveCloudSearchContents(MessageCloudSearchContents message) {
    }

    public void processSortPacket(Player player, int type) {
        if(player.containerMenu instanceof CloudChestMenu){
            if(type == 0){
                ((CloudChestMenu) player.containerMenu).sort(ItemSorting::defaultCompare);
            }
        }
    }

    public void processSearchPacket(Player player, String search) {
        if(player.containerMenu instanceof CloudChestMenu){
            ((CloudChestMenu) player.containerMenu).search(player, search);
        }
    }

    public void processScrollPacket(Player player, int scrollAmount) {
        if(player.containerMenu instanceof CloudChestMenu){
            ((CloudChestMenu) player.containerMenu).setScrollAmount(scrollAmount);
            player.containerMenu.sendAllDataToRemote();
        }
    }


    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event){
        if(event.getItemStack().getItem() == CSItemRegistry.BALLOON.get()){
            event.setUseBlock(TriState.FALSE);
        }
    }

    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult){
            Entity entity = ((EntityHitResult) event.getRayTraceResult()).getEntity();
            boolean flag = false;
            if(entity instanceof BalloonBuddyEntity buddy) {
                if (buddy.getPersonality() == BalloonFace.SMIRK) {
                    if (event.getEntity() instanceof AbstractArrow) {
                        //fixes soft crash with vanilla
                        ((AbstractArrow) event.getEntity()).setPierceLevel((byte) 0);
                    }
                    event.setCanceled(true);
                    boolean left = buddy.getRandom().nextBoolean();
                    Vec3 vector3d2 = new Vec3(0, 0, 1).yRot((float) ((left ? event.getProjectile().getYRot() - 90F : event.getProjectile().getYRot() + 90F) * Math.PI / 180F)).normalize();
                    buddy.hasImpulse = true;
                    buddy.setRotZ(left ? -45 : 45);
                    buddy.move(MoverType.SELF, new Vec3(vector3d2.x(), vector3d2.y(), vector3d2.z()));
                    flag = true;
                }
            }
            if(!flag && (entity instanceof LivingBalloon || entity instanceof BalloonEntity)){
                if(event.getProjectile().getOwner() instanceof AbstractSkeleton && entity instanceof BadloonEntity badloon){
                    badloon.dropMusicDisk = true;
                }
            }else if(!flag && event.getProjectile() instanceof AbstractArrow arrow){
                AABB aabb = arrow.getBoundingBox().inflate(15);
                BalloonEntity tied = null;
                List<BalloonEntity> balloons = arrow.level().getEntitiesOfClass(BalloonEntity.class, aabb);
                for(BalloonEntity balloon : balloons){
                    if(balloon.isArrow() && balloon.getChildId() != null && balloon.getChildId().equals(arrow.getUUID())){
                        tied = balloon;
                        break;
                    }
                }
                if(tied != null){
                    tied.setChildId(entity.getUUID());
                    arrow.setNoGravity(false);
                    tied.setStringLength(BalloonEntity.DEFAULT_STRING_LENGTH);
                    tied.setArrowTime(random.nextInt(80) + 80);
                }
            }
        }
    }

    public void onMobAttemptSpawn(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event){
        if(event.getEntity() instanceof Monster && random.nextFloat() < 0.5F){
            double dist = 64;
            AABB aabb = event.getEntity().getBoundingBox().inflate(dist);
            List<BalloonBuddyEntity> balloonBuddies = event.getLevel().getEntitiesOfClass(BalloonBuddyEntity.class, aabb);
            if(!balloonBuddies.isEmpty()){
                for(BalloonBuddyEntity balloonBuddy : balloonBuddies){
                    if(balloonBuddy.getPersonality() == BalloonFace.HAPPY){
                        event.setSpawnCancelled(true);
                        balloonBuddy.level().broadcastEntityEvent(balloonBuddy, (byte)68);
                        break;
                    }
                }
            }
        }
    }


    public void registerCommands(RegisterCommandsEvent event){
        RetrieveBalloonsCommand.register(event.getDispatcher());
    }

    public void onMobVisiblity(LivingEvent.LivingVisibilityEvent event){
        if(event.getEntity() instanceof Player){
            double dist = 16;
            AABB aabb = event.getEntity().getBoundingBox().inflate(dist);
            List<BalloonBuddyEntity> balloonBuddies = event.getEntity().level().getEntitiesOfClass(BalloonBuddyEntity.class, aabb);
            if(!balloonBuddies.isEmpty()){
                for(BalloonBuddyEntity balloonBuddy : balloonBuddies){
                    if(balloonBuddy.getPersonality() == BalloonFace.EYEPATCH){
                        event.modifyVisibility(0.25F);
                        break;
                    }
                }
            }
        }
    }

    public void onVillagerTrades(VillagerTradesEvent event) {
        if (CloudStorage.CONFIG.balloonSalesmanVillager.get() && event.getType() == CSVillagerRegistry.BALLOON_SALESMAN.get()) {
            List<VillagerTrades.ItemListing> level1 = new ArrayList<>();
            List<VillagerTrades.ItemListing> level2 = new ArrayList<>();
            List<VillagerTrades.ItemListing> level3 = new ArrayList<>();
            List<VillagerTrades.ItemListing> level4 = new ArrayList<>();
            List<VillagerTrades.ItemListing> level5 = new ArrayList<>();
            CSVillagerRegistry.initTrades(level1, level2, level3, level4, level5);
            event.getTrades().put(1, level1);
            event.getTrades().put(2, level2);
            event.getTrades().put(3, level3);
            event.getTrades().put(4, level4);
            event.getTrades().put(5, level5);
        }
    }

    public void processCloudInfoRequest(Player player, int balloonColor) {
        if(!player.level().isClientSide){
            CSWorldData data = CSWorldData.get(player.level());
            int usedSlots = 0;
            int allSlots = 0;
            int staticUsedSlots = 0;
            int staticAllSlots = 0;
            if(data != null){
                CloudIndex staticCloud = data.getPublicCloud(balloonColor);
                CloudIndex cloud = data.getPrivateCloud(player.getUUID(), balloonColor);
                if(cloud != null){
                    usedSlots = cloud.calcUsedSlots();
                    allSlots = cloud.getContainerSize();
                }
                if(staticCloud != null){
                    staticUsedSlots = staticCloud.calcUsedSlots();
                    staticAllSlots = staticCloud.getContainerSize();
                }
            }
            CloudStorage.sendNonLocal(new MessageUpdateCloudInfo(balloonColor, usedSlots, allSlots, staticUsedSlots, staticAllSlots), (ServerPlayer) player);
        }
    }

    public void setClientCloudInfo(Player player, int balloonColor, CloudInfo cloudInfo) {
    }

    public void openBookScreen(ItemStack itemStackIn) {
    }

    public void onServerTick(LevelTickEvent.Post tick) {
        if (!tick.getLevel().isClientSide && tick.getLevel() instanceof ServerLevel && CloudStorage.CONFIG.skyMobSpawning.get() && tick.getLevel().getDifficulty() != Difficulty.PEACEFUL) {
            ServerLevel serverWorld = (ServerLevel) tick.getLevel();
            if (SKY_MOB_SPAWNER_MAP.get(serverWorld) == null) {
                SKY_MOB_SPAWNER_MAP.put(serverWorld, new SkyMobSpawner(serverWorld));
            }
            SkyMobSpawner spawner = SKY_MOB_SPAWNER_MAP.get(serverWorld);
            spawner.tick();
        }
    }

    private static void checkForLuftballoons(Player player){
        int redBalloons = 0;
        Inventory inv = player.getInventory();
        for(int i = 0; i < inv.getContainerSize(); i++){
            if(inv.getItem(i).is(CSItemRegistry.BALLOON.get()) && BalloonItem.getBalloonColor(inv.getItem(i)) == BalloonItem.DEFAULT_COLOR){
                redBalloons += inv.getItem(i).getCount();
            }
            if(redBalloons >= 99){
                break;
            }
        }
        if(redBalloons >= 99 && player instanceof ServerPlayer){
            LUFTBALLONS_TRIGGER.trigger((ServerPlayer)player);
        }
    }

    public void onPlayerCraft(PlayerEvent.ItemCraftedEvent event) {
        checkForLuftballoons(event.getEntity());
    }

    public void onPlayerPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post event) {
        checkForLuftballoons(event.getPlayer());
    }

    public void onPlayerOpenContainer(PlayerContainerEvent event) {
        checkForLuftballoons(event.getEntity());
    }

    public void onEntitySetTarget(LivingChangeTargetEvent event) {
        if(event.getEntity() instanceof Mob && event.getNewAboutToBeSetTarget() instanceof BalloonBuddyEntity && ((BalloonBuddyEntity)event.getNewAboutToBeSetTarget()).getPersonality() == BalloonFace.EYEPATCH){
            ((Mob) event.getEntity()).setTarget(null);
        }
    }

    public void onHoldingBalloon(LivingEntity holder, ItemStack balloon, boolean leftHanded) {
    }
}
