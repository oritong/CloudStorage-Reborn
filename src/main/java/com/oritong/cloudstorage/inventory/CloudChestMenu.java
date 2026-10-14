package com.oritong.cloudstorage.inventory;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.network.MessageScrollCloudChest;
import com.oritong.cloudstorage.network.MessageCloudFluidAction;
import com.oritong.cloudstorage.network.MessageCloudFluids;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.world.CSWorldData;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.*;
import java.util.function.Predicate;

public class CloudChestMenu extends AbstractContainerMenu {
    private int scrollAmount;
    private final Container container;
    private final int playerInvStart = 0;
    private final int playerInvEnd = 0;
    private final Map<ItemStack, Integer> restoreFromAfterSearch = new HashMap<>();
    public String currentSearch = "";
    private SearchSnapshot pendingClientSearch;
    private final Player owner;
    private CloudFluidStorage fluidStorage;
    private Runnable fluidChanged = () -> { };
    private boolean fluidView;
    private long sentFluidRevision = -1;
    private long sentFluidCapacity = -1;
    private long fluidRevision = -1;
    private long fluidCapacity;
    private final List<CloudFluidStorage.Entry> clientFluids = new ArrayList<>();
    private final List<CloudFluidStorage.Entry> pendingFluids = new ArrayList<>();
    private long pendingFluidRevision = -1;

    public CloudChestMenu(int id, Inventory playerInv) {
        this(id, playerInv, new SimpleContainer(CloudStorage.PROXY.getVisibleCloudSlots()));
    }

    public CloudChestMenu(int id, Inventory playerInv, Container containerIn) {
        super(CSMenuRegistry.CLOUD_CHEST_MENU.get(), id);
        this.owner = playerInv.player;
        this.container = containerIn;
        containerIn.startOpen(playerInv.player);
        int clampedSize = Math.min(containerIn.getContainerSize(), 54);

        int ySlots = (int)Math.ceil(clampedSize / 9F);
        for (int k = 0; k < ySlots; ++k) {
            for (int l = 0; l < 9 && l + k * 9 < clampedSize; ++l) {
                int slotId = l + k * 9;
                this.addSlot(new SlotCloudChest(this, containerIn, slotId, 8 + l * 18, 18 + k * 18));
            }
        }
        for (int i1 = 0; i1 < 3; ++i1) {
            for (int k1 = 0; k1 < 9; ++k1) {
                this.addSlot(new Slot(playerInv, k1 + i1 * 9 + 9, 8 + k1 * 18, 140 + i1 * 18));
            }
        }

        for (int j1 = 0; j1 < 9; ++j1) {
            this.addSlot(new Slot(playerInv, j1, 8 + j1 * 18, 198));
        }
        CloudStorage.PROXY.setVisibleCloudSlots(containerIn.getContainerSize());
        scrollTo(0.0F, false);
    }

    public CloudChestMenu(int id, Inventory playerInv, CloudIndex cloud, CSWorldData data) {
        this(id, playerInv, cloud.getContainer());
        fluidStorage = cloud.getFluids();
        fluidChanged = data::setDirty;
    }

    public boolean isFluidView() { return fluidView; }
    public void setFluidView(boolean view) { fluidView = view; }
    public long getFluidRevision() { return fluidRevision; }
    public long getFluidCapacity() { return fluidCapacity; }
    public List<CloudFluidStorage.Entry> getClientFluids() { return Collections.unmodifiableList(clientFluids); }

    public void receiveFluids(MessageCloudFluids message) {
        if (message.offset() == 0) {
            pendingFluids.clear();
            pendingFluidRevision = message.revision();
        }
        if (pendingFluidRevision != message.revision() || pendingFluids.size() != message.offset()) return;
        pendingFluids.addAll(message.entries());
        if (pendingFluids.size() == message.total()) {
            clientFluids.clear();
            clientFluids.addAll(pendingFluids);
            fluidRevision = message.revision();
            fluidCapacity = message.capacity();
        }
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (fluidStorage != null && owner instanceof ServerPlayer player && player.containerMenu == this
                && (sentFluidRevision != fluidStorage.revision() || sentFluidCapacity != fluidStorage.capacity())) {
            MessageCloudFluids.send(player, containerId, fluidStorage);
            sentFluidRevision = fluidStorage.revision();
            sentFluidCapacity = fluidStorage.capacity();
        }
    }

    public void handleFluidAction(Player player, MessageCloudFluidAction message) {
        if (player != owner || player.containerMenu != this || !stillValid(player)
                || player.level().isClientSide() || fluidStorage == null || message.containerId() != containerId) return;
        if (message.action() == MessageCloudFluidAction.ITEMS || message.action() == MessageCloudFluidAction.FLUIDS) {
            fluidView = message.action() == MessageCloudFluidAction.FLUIDS;
            if (fluidView) {
                search(player, "");
                sentFluidRevision = -1;
                broadcastChanges();
            }
            return;
        }
        if (!fluidView || (message.action() != MessageCloudFluidAction.DEPOSIT && message.action() != MessageCloudFluidAction.WITHDRAW)) return;
        if (message.revision() != fluidStorage.revision()) {
            sentFluidRevision = -1;
            broadcastChanges();
            return;
        }
        FluidStack selected = FluidStack.EMPTY;
        if (message.action() == MessageCloudFluidAction.WITHDRAW) {
            if (message.entry() < 0 || message.entry() >= fluidStorage.snapshot().size()) return;
            selected = fluidStorage.getFluidInTank(message.entry()).copyWithAmount(Integer.MAX_VALUE);
        }
        if (CloudFluidContainerInteraction.interact(player, this, fluidStorage, selected,
                message.action() == MessageCloudFluidAction.DEPOSIT)) fluidChanged.run();
        broadcastChanges();
    }

    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (fluidView && slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof SlotCloudChest) return;
        super.clicked(slotId, button, type, player);
    }

    public static boolean matchesSearch(Player player, String search, ItemStack stack) {
        boolean matches = false;
        try{
            List<Component> tooltipLines = Lists.newArrayList();
            try{
                tooltipLines = stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(player.level()), player, TooltipFlag.Default.ADVANCED);
            }catch (Exception e){
                tooltipLines = Lists.newArrayList(stack.getHoverName());
            }
            for (Component line : tooltipLines) {
                String text = ChatFormatting.stripFormatting(line.getString()).toLowerCase(Locale.ROOT);
                if (player.level().isClientSide() ? CloudStorage.PROXY.matchesSearchText(text, search) : text.contains(search)) {
                    matches = true;
                    break;
                }
            }
        }catch (Exception e){
            CloudStorage.LOGGER.warn("cloud storage stopped a crash from displaying the tooltip of " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        if (search.startsWith("@")) {
            String modid = search.substring(1);
            ResourceLocation res = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if(res != null){
                matches = res.getNamespace().contains(modid);
            }
        } else if (search.startsWith("#")) {
            String tagId = search.substring(1);
            List<TagKey<Item>> tags = stack.getTags().toList();
            for (TagKey<Item> tag : tags) {
                ResourceLocation registryName = tag.location();
                if (registryName.toString().contains(tagId)) {
                    matches = true;
                    break;
                }
            }
        }
        return matches;
    }

    public void scrollTo(float scrollProgress, boolean sendPacket) {
        int maxScrollDown = this.container.getContainerSize() / 9 - 6;
        int i = Math.max((int) Math.floor(scrollProgress * maxScrollDown), 0);
        if (sendPacket) {
            CloudStorage.sendMSGToServer(new MessageScrollCloudChest(i));
        }
        scrollAmount = i;
    }

    public int getScrollAmount() {
        return scrollAmount;
    }

    public void setScrollAmount(int i) {
        scrollAmount = i;
    }

    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (fluidView || slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        int clampedSize = Math.min(this.container.getContainerSize(), 54);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (slotIndex < clampedSize) {
                if (!this.moveItemStackTo(itemstack1, clampedSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackToScrollable(itemstack1, 0, this.container.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    protected boolean moveItemStackToScrollable(ItemStack beingMoved, int startAt, int endAt, boolean moveBackwards) {
        boolean flag = false;
        int i = startAt;
        if (moveBackwards) {
            i = endAt - 1;
        }

        if (beingMoved.isStackable()) {
            while (!beingMoved.isEmpty()) {
                if (moveBackwards) {
                    if (i < startAt) {
                        break;
                    }
                } else if (i >= endAt) {
                    break;
                }

                ItemStack itemstack = this.container.getItem(i);
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameComponents(beingMoved, itemstack)) {
                    int j = itemstack.getCount() + beingMoved.getCount();
                    int maxSize = Math.min(itemstack.getMaxStackSize(), beingMoved.getMaxStackSize());
                    if (j <= maxSize) {
                        beingMoved.setCount(0);
                        itemstack.setCount(j);
                        container.setChanged();
                        flag = true;
                    } else if (itemstack.getCount() < maxSize) {
                        beingMoved.shrink(maxSize - itemstack.getCount());
                        itemstack.setCount(maxSize);
                        container.setChanged();
                        flag = true;
                    }
                }

                if (moveBackwards) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!beingMoved.isEmpty()) {
            if (moveBackwards) {
                i = endAt - 1;
            } else {
                i = startAt;
            }

            while (true) {
                if (moveBackwards) {
                    if (i < startAt) {
                        break;
                    }
                } else if (i >= endAt) {
                    break;
                }

                ItemStack itemstack1 = this.container.getItem(i);
                if (itemstack1.isEmpty()) {
                    if (beingMoved.getCount() > itemstack1.getMaxStackSize()) {
                        container.setItem(i, beingMoved.split(itemstack1.getMaxStackSize()));
                    } else {
                        container.setItem(i, beingMoved.split(beingMoved.getCount()));
                    }
                    container.setChanged();
                    flag = true;
                    break;
                }

                if (moveBackwards) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return flag;
    }

    public void updateGrays(Player player, String currentSearch) {
        updateGrays(currentSearch, stack -> matchesSearch(player, currentSearch, stack));
    }

    private void updateGrays(String currentSearch, Predicate<ItemStack> matcher) {
        for (Slot slot : this.slots) {
            if (slot instanceof SlotCloudChest cloudChest) {
                cloudChest.setGray(!currentSearch.isEmpty() && !matcher.test(cloudChest.getItem()));
            }
        }
    }

    public void removed(Player player) {
        pendingClientSearch = null;
        super.removed(player);
        this.container.stopOpen(player);
    }

    public void sort(Comparator<ItemStack> comparator) {
        pendingClientSearch = null;
        NonNullList<ItemStack> itemList = NonNullList.create();

        for (int i = 0; i < this.container.getContainerSize(); i++) {
            if (!container.getItem(i).isEmpty()) {
                itemList.add(container.getItem(i));
            }
        }
        itemList.sort(comparator);
        itemList = ItemSorting.combineStacks(container, itemList);
        container.clearContent();
        for (int j = 0; j < container.getContainerSize() && j < itemList.size(); j++) {
            container.setItem(j, itemList.get(j));
        }
        container.setChanged();
        broadcastChanges();
    }

    public void search(Player player, String search) {
        search(player, search, stack -> matchesSearch(player, search, stack));
    }

    private void search(Player player, String search, Predicate<ItemStack> matcher) {
        pendingClientSearch = null;
        if (search.isEmpty()) {
            if (!restoreFromAfterSearch.isEmpty()) {
                List<ItemStack> extras = new ArrayList<>();
                for (int i = 0; i < this.container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (restoreFromAfterSearch.get(stack) == null) { //new item
                        extras.add(stack);
                    }
                }
                this.container.clearContent();
                for (Map.Entry<ItemStack, Integer> stackEntry : restoreFromAfterSearch.entrySet()) {
                    this.container.setItem(stackEntry.getValue(), stackEntry.getKey());
                }
                for (ItemStack newStack : extras) {
                    ItemSorting.addItem(container, newStack);
                }
                restoreFromAfterSearch.clear();
            }
        } else {
            NonNullList<ItemStack> matches = NonNullList.create();
            NonNullList<ItemStack> noMatches = NonNullList.create();
            boolean reset = restoreFromAfterSearch.isEmpty();
            for (int i = 0; i < this.container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    if (matcher.test(stack)) {
                        matches.add(stack);
                    } else {
                        noMatches.add(stack);
                    }
                }
                if (reset) {
                    restoreFromAfterSearch.put(this.container.getItem(i), i);
                }
            }
            matches.sort(ItemSorting::defaultCompare);
            noMatches.sort(ItemSorting::defaultCompare);
            container.clearContent();
            matches.addAll(noMatches);
            for (int j = 0; j < container.getContainerSize() && j < matches.size(); j++) {
                container.setItem(j, matches.get(j));
            }
        }
        updateGrays(search, matcher);
        container.setChanged();
        broadcastChanges();
    }

    public record SearchSnapshot(int requestId, String search, List<ItemStack> items) {
    }

    private List<ItemStack> occupiedItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) items.add(stack);
        }
        return items;
    }

    public SearchSnapshot beginClientSearch(int requestId, String search) {
        pendingClientSearch = new SearchSnapshot(requestId, search, occupiedItems().stream().map(ItemStack::copy).toList());
        return pendingClientSearch;
    }

    public SearchSnapshot getPendingClientSearch(int requestId) {
        return pendingClientSearch != null && pendingClientSearch.requestId() == requestId ? pendingClientSearch : null;
    }

    public boolean finishClientSearch(Player player, int requestId, BitSet matches) {
        var snapshot = getPendingClientSearch(requestId);
        if (snapshot == null) return false;
        if (matches.length() > snapshot.items().size()) {
            pendingClientSearch = null;
            return false;
        }
        List<ItemStack> items = occupiedItems();
        // Shared inventories can change while the client is matching a snapshot.
        if (items.size() != snapshot.items().size()) return false;
        for (int i = 0; i < items.size(); i++) {
            if (!ItemStack.matches(items.get(i), snapshot.items().get(i))) return false;
        }
        Set<ItemStack> matchedItems = Collections.newSetFromMap(new IdentityHashMap<>());
        matches.stream().forEach(i -> matchedItems.add(items.get(i)));
        search(player, snapshot.search(), matchedItems::contains);
        return true;
    }

    public boolean isSlotGray(int i) {
        if (getSlot(i) instanceof SlotCloudChest slot) {
            return slot.isGray();
        }
        return false;
    }

}
