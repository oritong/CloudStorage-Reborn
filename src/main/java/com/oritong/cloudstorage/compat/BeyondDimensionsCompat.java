package com.oritong.cloudstorage.compat;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.inventory.CloudFluidStorage;
import com.oritong.cloudstorage.world.CSWorldData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/** Uses Beyond Dimensions' public API without making it a build or runtime requirement. */
public final class BeyondDimensionsCompat {
    private static final String API_PACKAGE = "com.wintercogs.beyonddimensions.";
    private static final Map<Player, OpenMenu> OPEN_MENUS = new WeakHashMap<>();
    private static Api api;
    private static boolean failed;

    private BeyondDimensionsCompat() {
    }

    @Nullable
    private static Api getApi() {
        if (!CloudStorage.CONFIG.beyondDimensionsStorage.get()
                || !ModList.get().isLoaded("beyonddimensions") || failed) return null;
        if (api == null) {
            try {
                Class<?> net = Class.forName(API_PACKAGE + "api.dimensionnet.DimensionsNet");
                Class<?> storage = Class.forName(API_PACKAGE + "api.dimensionnet.UnifiedStorage");
                Class<?> handler = Class.forName(API_PACKAGE + "api.capability.helper.unordered.ItemUnifiedStorageHandler");
                Class<?> fluidHandler = Class.forName(API_PACKAGE + "api.capability.helper.unordered.FluidUnifiedStorageHandler");
                Class<?> unordered = Class.forName(API_PACKAGE + "api.storage.handler.impl.AbstractUnorderedStackHandler");
                Class<?> menu = Class.forName(API_PACKAGE + "common.menu.DimensionsNetMenu");
                api = new Api(net.getMethod("getPrimaryNetFromPlayer", Player.class), net.getMethod("getOwner"),
                        net.getMethod("getUnifiedStorage"), handler.getConstructor(storage), fluidHandler.getConstructor(storage),
                        menu.getConstructor(MenuType.class, int.class, Inventory.class, unordered));
            } catch (ReflectiveOperationException | LinkageError e) {
                disable(e);
            }
        }
        return api;
    }

    private static void disable(Throwable error) {
        if (!failed) CloudStorage.LOGGER.warn("Beyond Dimensions integration failed; keeping remaining contents in cloud storage", error);
        failed = true;
    }

    @Nullable
    private static Target getTarget(Player player) {
        Api bridge = getApi();
        if (bridge == null || player.level().isClientSide()) return null;
        try {
            Object net = bridge.primary.invoke(null, player);
            // Membership of someone else's network is not consent to move a personal inventory there.
            if (net == null || !player.getUUID().equals(bridge.owner.invoke(net))) return null;
            Object storage = bridge.storage.invoke(net);
            return new Target(net, storage, (IItemHandler) bridge.handler.newInstance(storage),
                    (IFluidHandler) bridge.fluidHandler.newInstance(storage));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            disable(e);
            return null;
        }
    }

    /** Only removes what the destination accepted. Mutate the original stack for open search menus. */
    public static long transfer(Container source, IItemHandler destination) {
        long moved = 0;
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty()) continue;
            int offered = stack.getCount();
            ItemStack remaining = destination.insertItem(0, stack.copy(), false);
            if (!remaining.isEmpty() && (!ItemStack.isSameItemSameComponents(stack, remaining)
                    || remaining.getCount() > offered)) {
                throw new IllegalStateException("Beyond Dimensions returned an invalid insertion remainder");
            }
            int accepted = offered - remaining.getCount();
            if (accepted <= 0) continue;
            stack.shrink(accepted);
            if (stack.isEmpty()) source.setItem(slot, ItemStack.EMPTY);
            source.setChanged();
            moved += accepted;
        }
        return moved;
    }

    private static void transferClouds(Player player, CSWorldData data, Target target) {
        for (CloudIndex cloud : data.getAllPrivateCloudsFor(player.getUUID())) {
            // Mark dirty before insertion so a later error cannot leave earlier transfers unsaved.
            if (!cloud.getContainer().isEmpty()) {
                data.setDirty();
                transfer(cloud.getContainer(), target.items);
            }
            if (cloud.getFluids().storedAmount() > 0) {
                data.setDirty();
                transferFluids(cloud.getFluids(), target.fluids);
            }
        }
    }

    public static long transferFluids(CloudFluidStorage source, IFluidHandler destination) {
        if (!source.isEnabled()) return 0;
        long moved = 0;
        for (var entry : source.snapshot()) {
            // Bound work per scan even for clouds holding more than Integer.MAX_VALUE mB.
            int offered = (int) Math.min(Integer.MAX_VALUE, entry.amount());
            int accepted = destination.fill(entry.fluid().copyWithAmount(offered), IFluidHandler.FluidAction.EXECUTE);
            if (accepted < 0 || accepted > offered) throw new IllegalStateException("Beyond Dimensions accepted an invalid fluid amount");
            if (accepted > 0) {
                source.drain(entry.fluid().copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
                moved += accepted;
            }
        }
        return moved;
    }

    /** Returns null for ordinary cloud storage fallback, including a full network's remaining items. */
    @Nullable
    public static AbstractContainerMenu createMenu(int id, Inventory inventory, CloudIndex cloud) {
        Player player = inventory.player;
        Target target = getTarget(player);
        if (target == null) return null;
        try {
            CSWorldData data = CSWorldData.get(player.level());
            if (data == null) return null;
            transferClouds(player, data, target);
            if (!cloud.getContainer().isEmpty() || cloud.getFluids().isEnabled() && cloud.getFluids().storedAmount() > 0) return null;
            MenuType<?> type = BuiltInRegistries.MENU.get(ResourceLocation.fromNamespaceAndPath("beyonddimensions", "dimensions_net_menu"));
            if (type == null) throw new IllegalStateException("Beyond Dimensions storage menu is not registered");
            AbstractContainerMenu menu = (AbstractContainerMenu) api.menu.newInstance(type, id, inventory, target.storage);
            OPEN_MENUS.put(player, new OpenMenu(menu, target.net));
            return menu;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            disable(e);
            return null;
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        // The external menu's own validity check is unrestricted; enforce our personal-network rule.
        var iterator = OPEN_MENUS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Player player = entry.getKey();
            if (player.containerMenu != entry.getValue().menu) {
                iterator.remove();
                continue;
            }
            Target target = getTarget(player);
            if (target == null || target.net != entry.getValue().net) {
                player.closeContainer();
                iterator.remove();
            }
        }
        int transferIntervalTicks = CloudStorage.CONFIG.beyondDimensionsTransferIntervalSeconds.get() * 20;
        if (event.getServer().getTickCount() % transferIntervalTicks != 0 || getApi() == null) return;
        CSWorldData data = CSWorldData.get(event.getServer().overworld());
        if (data == null) return;
        for (Player player : event.getServer().getPlayerList().getPlayers()) {
            Target target = getTarget(player);
            if (target == null) continue;
            try {
                transferClouds(player, data, target);
            } catch (LinkageError | RuntimeException e) {
                disable(e);
                return;
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        OPEN_MENUS.clear();
        api = null;
        failed = false;
    }

    private record Api(Method primary, Method owner, Method storage, Constructor<?> handler, Constructor<?> fluidHandler, Constructor<?> menu) {
    }

    private record Target(Object net, Object storage, IItemHandler items, IFluidHandler fluids) {
    }

    private record OpenMenu(AbstractContainerMenu menu, Object net) {
    }
}
