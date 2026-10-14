package com.oritong.cloudstorage.inventory;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.network.MessageScrollCloudChest;
import com.oritong.cloudstorage.network.MessageCloudTerminalAction;
import com.oritong.cloudstorage.network.MessageCloudTerminalContents;
import com.oritong.cloudstorage.network.MessageCloudQuickMove;
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
import net.neoforged.neoforge.fluids.FluidUtil;
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
    private long terminalRevision;
    private long sentTerminalRevision = -1;
    private long observedFluidRevision = -1;
    private long observedFluidCapacity = -1;
    private int observedFluidTypes = -1;
    private boolean observedFluidEnabled;
    private List<CloudTerminalEntry> terminalEntries = List.of();
    private final List<CloudTerminalEntry> pendingTerminal = new ArrayList<>();
    private long pendingTerminalRevision = -1;
    private int pendingTerminalTotal;
    private long fluidCapacity;
    private int fluidTypeCapacity;

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

    public long getTerminalRevision() { return terminalRevision; }
    public int getItemCapacity() { return container.getContainerSize(); }
    public long getFluidCapacity() { return fluidStorage == null ? fluidCapacity : fluidStorage.isEnabled() ? fluidStorage.capacity() : 0; }
    public int getFluidTypeCapacity() { return fluidStorage == null ? fluidTypeCapacity : fluidStorage.isEnabled() ? fluidStorage.typeCapacity() : 0; }
    public boolean isFluidStorageEnabled() { return getFluidTypeCapacity() > 0 || terminalEntries.stream().anyMatch(CloudTerminalEntry::isFluid); }
    public List<CloudTerminalEntry> getTerminalEntries() { return terminalEntries; }

    public void receiveTerminal(MessageCloudTerminalContents message) {
        if (message.containerId() != containerId || message.revision() < terminalRevision) return;
        if (message.offset() == 0) {
            pendingTerminal.clear();
            pendingTerminalRevision = message.revision();
            pendingTerminalTotal = message.total();
        }
        if (pendingTerminalRevision != message.revision() || pendingTerminalTotal != message.total()
                || pendingTerminal.size() != message.offset()) return;
        pendingTerminal.addAll(message.entries());
        if (pendingTerminal.size() == message.total()) {
            terminalEntries = List.copyOf(pendingTerminal);
            terminalRevision = message.revision();
            fluidCapacity = message.fluidCapacity();
            fluidTypeCapacity = message.fluidTypeCapacity();
        }
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!owner.level().isClientSide()) refreshTerminal(false);
    }

    private void refreshTerminal(boolean force) {
        boolean changed = fluidStorage != null
                && (observedFluidRevision != fluidStorage.revision() || observedFluidCapacity != getFluidCapacity()
                || observedFluidTypes != getFluidTypeCapacity() || observedFluidEnabled != fluidStorage.isEnabled());
        if (!changed) {
            int entry = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                if (entry >= terminalEntries.size()) { changed = true; break; }
                var previous = terminalEntries.get(entry++);
                if (previous.isFluid() || previous.source() != i || !ItemStack.matches(stack, previous.item())) {
                    changed = true;
                    break;
                }
            }
            if (entry < terminalEntries.size() && !terminalEntries.get(entry).isFluid()) changed = true;
        }
        if (changed || force) {
            List<CloudTerminalEntry> current = new ArrayList<>();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) current.add(CloudTerminalEntry.item(i, stack));
            }
            if (fluidStorage != null && fluidStorage.isEnabled()) {
                var fluids = fluidStorage.snapshot();
                for (int i = 0; i < fluids.size(); i++) current.add(CloudTerminalEntry.fluid(i, fluids.get(i)));
            }
            terminalEntries = List.copyOf(current);
            terminalRevision++;
            if (fluidStorage != null) {
                observedFluidRevision = fluidStorage.revision();
                observedFluidCapacity = getFluidCapacity();
                observedFluidTypes = getFluidTypeCapacity();
                observedFluidEnabled = fluidStorage.isEnabled();
            }
        }
        if (owner instanceof ServerPlayer player && player.containerMenu == this && sentTerminalRevision != terminalRevision) {
            MessageCloudTerminalContents.send(player, this, terminalEntries);
            sentTerminalRevision = terminalRevision;
        }
    }

    public void handleTerminalAction(Player player, MessageCloudTerminalAction message) {
        if (player != owner || player.containerMenu != this || !stillValid(player)
                || player.level().isClientSide() || message.containerId() != containerId
                || message.button() < 0 || message.button() > 1 || message.entry() < -1) return;
        refreshTerminal(false);
        if (message.revision() != terminalRevision) {
            refreshTerminal(true);
            return;
        }
        if (message.entry() >= terminalEntries.size()) return;
        var entry = message.entry() < 0 ? null : terminalEntries.get(message.entry());
        if (fluidStorage != null && fluidStorage.isEnabled() && message.button() == 1 && FluidUtil.getFluidContained(getCarried()).isPresent()) {
            if (CloudFluidContainerInteraction.interact(player, this, fluidStorage, FluidStack.EMPTY, true)) fluidChanged.run();
        } else if (entry != null && entry.isFluid()) {
            if (message.button() == 0 && fluidStorage != null && CloudFluidContainerInteraction.interact(player, this,
                    fluidStorage, entry.fluid().copyWithAmount(Integer.MAX_VALUE), false)) fluidChanged.run();
        } else if (message.shift() && entry != null && getCarried().isEmpty()) {
            ItemStack source = container.getItem(entry.source());
            moveItemStackTo(source, cloudSlotCount(), slots.size(), true);
            if (source.isEmpty()) container.setItem(entry.source(), ItemStack.EMPTY);
            container.setChanged();
        } else {
            interactItem(entry, message.button());
        }
        broadcastChanges();
        fluidChanged.run();
    }

    private int cloudSlotCount() { return Math.min(container.getContainerSize(), 54); }

    public void handleQuickMove(Player player, MessageCloudQuickMove message) {
        if (player != owner || player.containerMenu != this || !stillValid(player)
                || player.level().isClientSide() || message.containerId() != containerId || !getCarried().isEmpty()
                || message.source() < 0 || message.source() >= container.getContainerSize() || message.expected().isEmpty()) return;
        ItemStack source = container.getItem(message.source());
        if (!ItemStack.matches(source, message.expected())) return;
        if (moveItemStackTo(source, cloudSlotCount(), slots.size(), true)) {
            if (source.isEmpty()) container.setItem(message.source(), ItemStack.EMPTY);
            container.setChanged();
            fluidChanged.run();
        }
        broadcastChanges();
    }

    private void interactItem(CloudTerminalEntry entry, int button) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            if (entry != null) {
                ItemStack source = container.getItem(entry.source());
                int count = button == 0 ? source.getCount() : (source.getCount() + 1) / 2;
                setCarried(source.split(count));
                if (source.isEmpty()) container.setItem(entry.source(), ItemStack.EMPTY);
            }
        } else if (entry == null) {
            insertCarried(button == 1 ? 1 : carried.getCount());
        } else {
            ItemStack source = container.getItem(entry.source());
            if (ItemStack.isSameItemSameComponents(source, carried)) {
                int count = Math.min(button == 1 ? 1 : carried.getCount(), Math.max(0, Math.min(container.getMaxStackSize(), source.getMaxStackSize()) - source.getCount()));
                source.grow(count);
                carried.shrink(count);
            } else if (carried.getCount() <= Math.min(container.getMaxStackSize(), carried.getMaxStackSize())) {
                container.setItem(entry.source(), carried);
                setCarried(source);
            }
        }
        if (getCarried().isEmpty()) setCarried(ItemStack.EMPTY);
        container.setChanged();
    }

    private void insertCarried(int amount) {
        ItemStack carried = getCarried();
        ItemStack offered = carried.copyWithCount(amount);
        moveItemStackToScrollable(offered, 0, container.getContainerSize(), false);
        carried.shrink(amount - offered.getCount());
    }

    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof SlotCloudChest) return;
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
        if (slotIndex < cloudSlotCount() || slotIndex >= slots.size()) return ItemStack.EMPTY;
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
            fluidChanged.run();
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
                    int maxSize = Math.min(container.getMaxStackSize(), Math.min(itemstack.getMaxStackSize(), beingMoved.getMaxStackSize()));
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

            while (!beingMoved.isEmpty()) {
                if (moveBackwards) {
                    if (i < startAt) {
                        break;
                    }
                } else if (i >= endAt) {
                    break;
                }

                ItemStack itemstack1 = this.container.getItem(i);
                if (itemstack1.isEmpty()) {
                    int maxSize = Math.min(container.getMaxStackSize(), beingMoved.getMaxStackSize());
                    container.setItem(i, beingMoved.split(Math.min(beingMoved.getCount(), maxSize)));
                    container.setChanged();
                    flag = true;
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
