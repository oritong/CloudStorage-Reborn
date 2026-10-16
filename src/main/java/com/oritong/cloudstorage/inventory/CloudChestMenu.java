package com.oritong.cloudstorage.inventory;

import com.oritong.cloudstorage.CloudStorage;
import com.oritong.cloudstorage.network.MessageScrollCloudChest;
import com.oritong.cloudstorage.network.MessageCloudTerminalAction;
import com.oritong.cloudstorage.network.MessageCloudTerminalContents;
import com.oritong.cloudstorage.network.MessageCloudQuickMove;
import com.oritong.cloudstorage.misc.CloudIndex;
import com.oritong.cloudstorage.misc.CloudFunction;
import com.oritong.cloudstorage.world.CSWorldData;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
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
    private final CloudIndex cloud;
    private final CloudCraftingContainer crafting;
    private final ResultContainer craftingResult = new ResultContainer();
    private final ContainerListener craftingListener;
    private boolean craftingOpen;
    private int clientCraftingFlags;
    private long observedCraftingRevision = -1;
    private long lastPullTerminalRevision = -1;
    private long lastPullCraftingRevision = -1;

    public CloudChestMenu(int id, Inventory playerInv) {
        this(id, playerInv, new SimpleContainer(CloudStorage.PROXY.getVisibleCloudSlots()));
    }

    public CloudChestMenu(int id, Inventory playerInv, Container containerIn) {
        this(id, playerInv, containerIn, null, () -> { });
    }

    private CloudChestMenu(int id, Inventory playerInv, Container containerIn, CloudIndex cloud, Runnable changed) {
        super(CSMenuRegistry.CLOUD_CHEST_MENU.get(), id);
        this.owner = playerInv.player;
        this.container = containerIn;
        this.cloud = cloud;
        this.fluidChanged = changed;
        this.fluidStorage = cloud == null ? null : cloud.getFluids();
        this.crafting = cloud == null ? new CloudCraftingContainer() : cloud.getCrafting();
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
        for (int i = 0; i < 9; i++) addSlot(craftingSlot(i, 30 + i % 3 * 18, 144 + i / 3 * 18));
        addSlot(new CloudResultSlot(124, 162));
        addDataSlot(new DataSlot() {
            @Override public int get() { return craftingFlags(); }
            @Override public void set(int value) { clientCraftingFlags = value; }
        });
        craftingListener = ignored -> { observedCraftingRevision = -1; fluidChanged.run(); };
        if (!owner.level().isClientSide()) crafting.addListener(craftingListener);
        CloudStorage.PROXY.setVisibleCloudSlots(containerIn.getContainerSize());
        scrollTo(0.0F, false);
        updateCraftingResult();
    }

    public CloudChestMenu(int id, Inventory playerInv, CloudIndex cloud, CSWorldData data) {
        this(id, playerInv, cloud.getContainer(), cloud, data::setDirty);
    }

    private int craftingFlags() {
        if (cloud == null) return clientCraftingFlags;
        return cloud.functionMask() | (craftingOpen ? 256 : 0) | (cloud.autoClear() ? 512 : 0)
                | (cloud.autoPull() ? 1024 : 0) | (cloud.craftToCloud() ? 2048 : 0);
    }
    public int getFunctionMask() { return craftingFlags() & 255; }
    public boolean hasFunction(CloudFunction function) { return (getFunctionMask() & function.mask()) != 0; }
    public boolean isCraftingOpen() { return hasFunction(CloudFunction.CRAFTING_TABLE) && (craftingFlags() & 256) != 0; }
    public boolean isAutoClear() { return (craftingFlags() & 512) != 0; }
    public boolean isAutoPull() { return (craftingFlags() & 1024) != 0; }
    public boolean isCraftToCloud() { return (craftingFlags() & 2048) != 0; }
    public int craftingStart() { return playerSlotEnd(); }
    public int craftingResultSlot() { return craftingStart() + 9; }
    public CloudCraftingContainer getCrafting() { return crafting; }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (player != owner || player.level().isClientSide() || cloud == null || !stillValid(player)) return false;
        if (button == 100 && hasFunction(CloudFunction.CRAFTING_TABLE)) craftingOpen = !craftingOpen;
        else if (hasFunction(CloudFunction.CRAFTING_TABLE)) {
            switch (button) {
                case 200 -> cloud.setAutoClear(!cloud.autoClear());
                case 201 -> cloud.setAutoPull(!cloud.autoPull());
                case 202 -> cloud.setCraftToCloud(!cloud.craftToCloud());
                default -> { return false; }
            }
        } else return false;
        lastPullTerminalRevision = -1;
        lastPullCraftingRevision = -1;
        fluidChanged.run();
        broadcastChanges();
        return true;
    }

    private Slot craftingSlot(int index, int x, int y) {
        return new Slot(crafting, index, x, y) {
            @Override public boolean isActive() { return isCraftingOpen(); }
            @Override public boolean mayPlace(ItemStack stack) { return isActive(); }
            @Override public boolean mayPickup(Player player) { return isActive(); }
        };
    }

    /** Slot coordinates are final in vanilla; replace only the views when the screen is resized. */
    public void layoutSlots(int rows) {
        int offset = (rows - 6) * 18 + (isCraftingOpen() ? 78 : 0);
        for (int i = 0; i < 36; i++) {
            int index = cloudSlotCount() + i;
            Slot old = slots.get(index);
            Slot replacement = new Slot(old.container, old.getContainerSlot(), 8 + i % 9 * 18,
                    (i < 27 ? 140 + i / 9 * 18 : 198) + offset);
            replacement.index = index;
            slots.set(index, replacement);
        }
        int gridY = 18 + rows * 18 + 18;
        for (int i = 0; i < 10; i++) {
            int index = craftingStart() + i;
            Slot replacement = i == 9 ? new CloudResultSlot(124, gridY + 18)
                    : craftingSlot(i, 30 + i % 3 * 18, gridY + i / 3 * 18);
            replacement.index = index;
            slots.set(index, replacement);
        }
    }

    private void updateCraftingResult() {
        if (owner.level().isClientSide() || observedCraftingRevision == crafting.revision()) return;
        observedCraftingRevision = crafting.revision();
        ItemStack output = ItemStack.EMPTY;
        if (hasFunction(CloudFunction.CRAFTING_TABLE) && owner instanceof ServerPlayer player) {
            var input = crafting.asCraftInput();
            var recipe = owner.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, owner.level());
            if (recipe.isPresent() && craftingResult.setRecipeUsed(owner.level(), player, recipe.get())) {
                ItemStack assembled = recipe.get().value().assemble(input, owner.level().registryAccess());
                if (assembled.isItemEnabled(owner.level().enabledFeatures())) output = assembled;
            } else craftingResult.setRecipeUsed(null);
        }
        craftingResult.setItem(0, output);
    }

    private void pullIngredients() {
        if (!isCraftingOpen() || !isAutoPull() || owner.level().isClientSide()) return;
        List<ItemStack> pattern = crafting.pattern();
        boolean missing = false;
        for (int i = 0; i < 9; i++) if (!pattern.get(i).isEmpty() && crafting.getItem(i).isEmpty()) { missing = true; break; }
        if (!missing) return;
        var input = CraftingInput.of(3, 3, pattern);
        var recipe = owner.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, owner.level());
        if (recipe.isEmpty()) return;
        ItemStack expected = recipe.get().value().assemble(input, owner.level().registryAccess());
        crafting.beginBatch();
        boolean changed = false;
        try {
            for (int i = 0; i < 9; i++) {
                ItemStack wanted = pattern.get(i);
                if (wanted.isEmpty() || !crafting.getItem(i).isEmpty()) continue;
                int source = -1;
                for (int j = 0; j < container.getContainerSize(); j++) {
                    if (ItemStack.isSameItemSameComponents(container.getItem(j), wanted)) { source = j; break; }
                }
                if (source < 0) {
                    // Accept tag alternatives only when they preserve this recipe and its exact output.
                    for (int j = 0; j < container.getContainerSize(); j++) {
                        ItemStack candidate = container.getItem(j);
                        if (candidate.isEmpty()) continue;
                        List<ItemStack> test = new ArrayList<>(pattern);
                        for (int k = 0; k < 9; k++) if (!crafting.getItem(k).isEmpty()) test.set(k, crafting.getItem(k));
                        test.set(i, candidate.copyWithCount(1));
                        var candidateInput = CraftingInput.of(3, 3, test);
                        if (!recipe.get().value().matches(candidateInput, owner.level())) continue;
                        var candidateRecipe = owner.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, candidateInput, owner.level());
                        if (candidateRecipe.isPresent() && candidateRecipe.get().id().equals(recipe.get().id()) && ItemStack.matches(expected,
                                candidateRecipe.get().value().assemble(candidateInput, owner.level().registryAccess()))) { source = j; break; }
                    }
                }
                if (source >= 0) {
                    crafting.setItem(i, container.removeItem(source, 1));
                    changed = true;
                }
            }
        } finally { crafting.endBatch(); }
        if (changed) { container.setChanged(); fluidChanged.run(); }
    }

    private final class CloudResultSlot extends ResultSlot {
        CloudResultSlot(int x, int y) { super(owner, crafting, craftingResult, 0, x, y); }
        @Override public boolean isActive() { return isCraftingOpen(); }
        @Override public boolean mayPickup(Player player) { return isActive() && player == owner; }
        @Override public void onTake(Player player, ItemStack stack) {
            if (owner.level().isClientSide()) return;
            crafting.rememberPattern();
            crafting.beginBatch();
            try { super.onTake(player, stack); }
            finally { crafting.endBatch(); }
            pullIngredients();
            updateCraftingResult();
            fluidChanged.run();
        }
        void quickCraft(ItemStack output) { onQuickCraft(output, output.getCount()); }
    }

    private int freeSpace(Container target, ItemStack stack, int limit) {
        int space = 0;
        for (int i = 0; i < target.getContainerSize() && space < limit; i++) {
            ItemStack existing = target.getItem(i);
            if (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack))
                space += Math.max(0, Math.min(target.getMaxStackSize(stack), stack.getMaxStackSize()) - existing.getCount());
        }
        return space;
    }

    private ItemStack quickCraft(Player player) {
        if (player != owner || !isCraftingOpen() || player.level().isClientSide()) return ItemStack.EMPTY;
        updateCraftingResult();
        ItemStack first = craftingResult.getItem(0).copy();
        for (int crafts = 0; crafts < 64; crafts++) {
            ItemStack output = craftingResult.getItem(0).copy();
            if (output.isEmpty() || !ItemStack.isSameItemSameComponents(output, first)) break;
            // Include only the 36 main inventory slots, never armor, offhand or crafting ingredients.
            int inventorySpace = 0;
            for (int i = cloudSlotCount(); i < playerSlotEnd(); i++) {
                Slot slot = slots.get(i);
                ItemStack existing = slot.getItem();
                if (slot.mayPlace(output) && (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, output)))
                    inventorySpace += Math.max(0, slot.getMaxStackSize(output) - existing.getCount());
            }
            if (inventorySpace + freeSpace(container, output, output.getCount()) < output.getCount()) break;
            ItemStack moved = output.copy();
            if (isCraftToCloud()) moveItemStackToScrollable(moved, 0, container.getContainerSize(), false);
            moveItemStackTo(moved, cloudSlotCount(), playerSlotEnd(), true);
            if (!isCraftToCloud()) moveItemStackToScrollable(moved, 0, container.getContainerSize(), false);
            craftingResult.setItem(0, ItemStack.EMPTY);
            var resultSlot = (CloudResultSlot) slots.get(craftingResultSlot());
            output.getItem().onCraftedBy(output, player.level(), player);
            resultSlot.quickCraft(output);
            resultSlot.onTake(player, output);
        }
        broadcastChanges();
        return first;
    }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != craftingResult && super.canTakeItemForPickAll(stack, slot);
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
        if (!owner.level().isClientSide()) {
            refreshTerminal(false);
            if (lastPullTerminalRevision != terminalRevision || lastPullCraftingRevision != crafting.revision()) {
                pullIngredients();
                lastPullTerminalRevision = terminalRevision;
                lastPullCraftingRevision = crafting.revision();
            }
            updateCraftingResult();
        }
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
            moveItemStackTo(source, cloudSlotCount(), playerSlotEnd(), true);
            if (source.isEmpty()) container.setItem(entry.source(), ItemStack.EMPTY);
            container.setChanged();
        } else {
            interactItem(entry, message.button());
        }
        broadcastChanges();
        fluidChanged.run();
    }

    private int cloudSlotCount() { return Math.min(container.getContainerSize(), 54); }
    private int playerSlotEnd() { return cloudSlotCount() + 36; }

    public void handleQuickMove(Player player, MessageCloudQuickMove message) {
        if (player != owner || player.containerMenu != this || !stillValid(player)
                || player.level().isClientSide() || message.containerId() != containerId || !getCarried().isEmpty()
                || message.source() < 0 || message.source() >= container.getContainerSize() || message.expected().isEmpty()) return;
        ItemStack source = container.getItem(message.source());
        if (!ItemStack.matches(source, message.expected())) return;
        if (moveItemStackTo(source, cloudSlotCount(), playerSlotEnd(), true)) {
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
        if (slotId >= craftingStart() && slotId < slots.size() && !isCraftingOpen()) return;
        if (slotId == craftingResultSlot()) {
            updateCraftingResult();
            if (type == ClickType.QUICK_MOVE) { quickCraft(player); return; }
        }
        super.clicked(slotId, button, type, player);
        updateCraftingResult();
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
        if (slotIndex == craftingResultSlot()) return quickCraft(player);
        if (slotIndex >= craftingStart() && !isCraftingOpen()) return ItemStack.EMPTY;
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
        if (!owner.level().isClientSide()) {
            crafting.removeListener(craftingListener);
            if (isAutoClear()) {
                crafting.beginBatch();
                try {
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = crafting.getItem(i);
                        moveItemStackToScrollable(stack, 0, container.getContainerSize(), false);
                        if (stack.isEmpty()) crafting.setItem(i, ItemStack.EMPTY);
                    }
                    crafting.rememberPattern();
                } finally { crafting.endBatch(); }
                fluidChanged.run();
            }
        }
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
