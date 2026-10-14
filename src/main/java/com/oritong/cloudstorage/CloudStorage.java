package com.oritong.cloudstorage;

import com.oritong.cloudstorage.block.CSBlockEntityRegistry;
import com.oritong.cloudstorage.block.CSBlockRegistry;
import com.oritong.cloudstorage.block.CSPOIRegistry;
import com.oritong.cloudstorage.client.ClientProxy;
import com.oritong.cloudstorage.client.particle.CSParticleRegistry;
import com.oritong.cloudstorage.entity.CSEntityRegistry;
import com.oritong.cloudstorage.entity.villager.CSVillagerRegistry;
import com.oritong.cloudstorage.inventory.CSMenuRegistry;
import com.oritong.cloudstorage.item.CSItemRegistry;
import com.oritong.cloudstorage.misc.CSCreativeTabRegistry;
import com.oritong.cloudstorage.misc.CSLootRegistry;
import com.oritong.cloudstorage.misc.CSRecipeRegistry;
import com.oritong.cloudstorage.misc.CSSoundRegistry;
import com.oritong.cloudstorage.network.*;
import com.oritong.cloudstorage.world.CSStructureRegistry;
import com.oritong.cloudstorage.world.CSVillageStructureRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("cloudstorage_reborn")
public class CloudStorage
{
    public static final String MODID = "cloudstorage_reborn";
    public static final Logger LOGGER = LogManager.getLogger();
    public static CommonProxy PROXY = createProxy(() -> ClientProxy::new, () -> CommonProxy::new);

    private static <T> T createProxy(java.util.function.Supplier<java.util.function.Supplier<T>> client, java.util.function.Supplier<java.util.function.Supplier<T>> server) {
        return (FMLEnvironment.dist == Dist.CLIENT ? client : server).get().get();
    }

    public static IEventBus MOD_BUS;
    public static final CSConfig CONFIG;
    private static final ModConfigSpec CONFIG_SPEC;

    static {
        final Pair<CSConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(CSConfig::new);
        CONFIG = specPair.getLeft();
        CONFIG_SPEC = specPair.getRight();
    }


    public CloudStorage(IEventBus bus, ModContainer container) {
        MOD_BUS = bus;
        bus.addListener(this::registerPayloads);
        bus.addListener(this::registerCapabilities);
        bus.addListener(this::setupClient);
        bus.addListener(this::setup);
        container.registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC, "cloudstorage_reborn-common.toml");
        CSBlockRegistry.DEF_REG.register(bus);
        CSItemRegistry.DEF_REG.register(bus);
        CSEntityRegistry.DEF_REG.register(bus);
        CSCreativeTabRegistry.DEF_REG.register(bus);
        CSBlockEntityRegistry.DEF_REG.register(bus);
        CSPOIRegistry.DEF_REG.register(bus);
        CSStructureRegistry.STRUCTURE_PIECE_DEF_REG.register(bus);
        CSStructureRegistry.STRUCTURE_TYPE_DEF_REG.register(bus);
        CSSoundRegistry.DEF_REG.register(bus);
        CSMenuRegistry.DEF_REG.register(bus);
        CSVillagerRegistry.DEF_REG.register(bus);
        CSRecipeRegistry.DEF_REG.register(bus);
        CSParticleRegistry.DEF_REG.register(bus);
        CSLootRegistry.DEF_REG.register(bus);
        CSVillageStructureRegistry.DEF_REG.register(bus);
        PROXY.init();
    }

    private void setupClient(FMLClientSetupEvent event) {
        event.enqueueWork(PROXY::clientInit);
    }


    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() ->{
            CSItemRegistry.registerDispenserBehavior();
            CSVillageStructureRegistry.registerHouses();
        });
    }
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("6");
        registrar.playToServer(MessageCloudQuickMove.TYPE, MessageCloudQuickMove.CODEC, MessageCloudQuickMove::handle);
        registrar.playToServer(MessageCloudTerminalAction.TYPE, MessageCloudTerminalAction.CODEC, MessageCloudTerminalAction::handle);
        registrar.playToClient(MessageCloudTerminalContents.TYPE, MessageCloudTerminalContents.CODEC, MessageCloudTerminalContents::handle);
        registrar.playToServer(MessageSortCloudChest.TYPE, MessageSortCloudChest.CODEC, MessageSortCloudChest.Handler::handle);
        registrar.playToServer(MessageSearchCloudChest.TYPE, MessageSearchCloudChest.CODEC, MessageSearchCloudChest.Handler::handle);
        registrar.playToServer(MessageRequestCloudSearch.TYPE, MessageRequestCloudSearch.CODEC, MessageRequestCloudSearch::handle);
        registrar.playToServer(MessageCloudSearchMatches.TYPE, MessageCloudSearchMatches.CODEC, MessageCloudSearchMatches::handle);
        registrar.playToClient(MessageCloudSearchContents.TYPE, MessageCloudSearchContents.CODEC, MessageCloudSearchContents::handle);
        registrar.playToServer(MessageRequestCloudInfo.TYPE, MessageRequestCloudInfo.CODEC, MessageRequestCloudInfo.Handler::handle);
        registrar.playToServer(MessageScrollCloudChest.TYPE, MessageScrollCloudChest.CODEC, MessageScrollCloudChest.Handler::handle);
        registrar.playToServer(MessageLeftClickCloudBlower.TYPE, MessageLeftClickCloudBlower.CODEC, MessageLeftClickCloudBlower.Handler::handle);
        registrar.playToClient(MessageUpdateCloudInfo.TYPE, MessageUpdateCloudInfo.CODEC, MessageUpdateCloudInfo.Handler::handle);
        registrar.playToClient(MessageOpenCloudChest.TYPE, MessageOpenCloudChest.CODEC, MessageOpenCloudChest.Handler::handle);
    }

    private void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, CSBlockEntityRegistry.STATIC_CLOUD_CHEST.get(), (chest, side) -> chest.getItemHandler(side));
    }

    public static void sendMSGToAll(CustomPacketPayload message) { PacketDistributor.sendToAllPlayers(message); }
    public static void sendNonLocal(CustomPacketPayload message, ServerPlayer player) { PacketDistributor.sendToPlayer(player, message); }
    public static void sendMSGToServer(CustomPacketPayload message) { PacketDistributor.sendToServer(message); }
}
