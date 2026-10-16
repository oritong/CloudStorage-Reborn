package com.oritong.cloudstorage.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** One persistent furnace per cloud, with shared slots and up to 64 independent burners. */
public final class CloudFurnace extends CloudChestContainer {
    public static final TagKey<Item> AUTO_FUEL = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("cloudstorage", "fuel"));
    public static final int FUEL = 9, OUTPUT = 18;
    private int count, flags, cursor, active, ticks;
    private int clientProgress;
    private final int[] burn = new int[64], duration = new int[64], progress = new int[64], total = new int[64], input = new int[64];
    private final ItemStack[] ingredient = new ItemStack[64];
    private final ResourceLocation[] recipeIds = new ResourceLocation[64];
    private final Map<ResourceLocation, Integer> recipesUsed = new HashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck = RecipeManager.createCheck(RecipeType.SMELTING);

    public CloudFurnace() {
        super(27);
        Arrays.fill(input, -1);
        Arrays.fill(ingredient, ItemStack.EMPTY);
    }
    public int count() { return count; }
    public void setCount(int value) { count = Math.clamp(value, 0, 64); }
    public boolean upload() { if (count == 64) return false; count++; setChanged(); return true; }
    public int slotsPerGroup() { return count >= 64 ? 9 : count >= 32 ? 4 : count >= 16 ? 2 : count > 0 ? 1 : 0; }
    public int rows() { return slotsPerGroup() == 9 ? 3 : slotsPerGroup() >= 2 ? 2 : 1; }
    public int columns() { return slotsPerGroup() == 9 ? 3 : slotsPerGroup() == 4 ? 2 : 1; }
    public int flags() { return flags; }
    public void setFlags(int value) {
        flags = value & 15;
        if ((flags & 8) != 0) flags &= ~2;
    }
    public boolean autoOutput() { return (flags & 1) != 0; }
    public boolean autoFuel() { return fuelMode() != 0; }
    public int fuelMode() { return (flags & 8) != 0 ? 2 : ((flags & 2) != 0 ? 1 : 0); }
    public boolean smeltIncoming() { return (flags & 4) != 0; }
    public void toggle(int option) {
        if (option == 1) {
            toggleAutoFuel();
            return;
        }
        flags ^= 1 << option;
        setChanged();
    }
    public void toggleAutoFuel() {
        if (fuelMode() == 0) flags |= 2;
        else if (fuelMode() == 1) flags &= ~2;
        else flags = (flags & ~8) | 2;
        setChanged();
    }
    public void setPriorityFuel(boolean enabled) {
        if (enabled) flags = (flags & ~2) | 8;
        else flags &= ~8;
        setChanged();
    }
    public int active() { return active; }
    public void setActive(int value) { active = value; }
    public int progressScale() {
        int sum = 0, running = 0;
        for (int i = 0; i < count; i++) if (total[i] > 0 && input[i] >= 0) { sum += (int) ((long) progress[i] * 1000 / total[i]); running++; }
        return running == 0 ? clientProgress : sum / running;
    }
    public void setClientProgress(int value) { clientProgress = value; }
    public boolean isSmeltable(Level level, ItemStack stack) {
        return !stack.isEmpty() && quickCheck.getRecipeFor(new SingleRecipeInput(stack), level).isPresent();
    }
    public static boolean isFuel(ItemStack stack) { return !stack.isEmpty() && stack.getBurnTime(RecipeType.SMELTING) > 0; }

    /** Simulations only work on copies, including the empty-slot case. */
    public ItemStack insertInput(ItemStack stack, boolean simulate) { return insert(this, 0, slotsPerGroup(), stack, simulate); }
    private static ItemStack insert(Container target, int start, int size, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int pass = 0; pass < 2; pass++) for (int i = start; i < start + size && !remaining.isEmpty(); i++) {
            ItemStack existing = target.getItem(i);
            if (pass == 0 ? existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining) : !existing.isEmpty()) continue;
            int accepted = Math.min(remaining.getCount(), Math.max(0, Math.min(target.getMaxStackSize(remaining), remaining.getMaxStackSize()) - existing.getCount()));
            if (accepted <= 0) continue;
            if (!simulate) target.setItem(i, existing.isEmpty() ? remaining.copyWithCount(accepted) : existing.copyWithCount(existing.getCount() + accepted));
            remaining.shrink(accepted);
        }
        return remaining;
    }
    private boolean roomForOutput(ItemStack stack) { return !stack.isEmpty() && insert(this, OUTPUT, slotsPerGroup(), stack, true).isEmpty(); }

    private void pullFuel(Container cloud) {
        boolean room = false;
        for (int i = FUEL; i < FUEL + slotsPerGroup(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty() || isFuel(stack) && stack.getCount() < Math.min(getMaxStackSize(stack), stack.getMaxStackSize())) { room = true; break; }
        }
        if (!room) return;
        int source = -1;
        long bestAmount = -1;
        for (int i = 0; i < cloud.getContainerSize(); i++) {
            ItemStack stack = cloud.getItem(i);
            if (!isFuel(stack) || fuelMode() == 1 && !stack.is(AUTO_FUEL)) continue;
            long total = 0;
            for (int j = i; j < cloud.getContainerSize(); j++) {
                ItemStack other = cloud.getItem(j);
                if (ItemStack.isSameItemSameComponents(stack, other)) total += other.getCount();
            }
            if (total > bestAmount) {
                bestAmount = total;
                source = i;
            }
        }
        if (source >= 0) {
            ItemStack stack = cloud.getItem(source);
            ItemStack remaining = insert(this, FUEL, slotsPerGroup(), stack, false);
            if (remaining.getCount() != stack.getCount()) cloud.setItem(source, remaining);
        }
    }
    private int lightBurner(Container cloud) {
        for (int slot = FUEL; slot < FUEL + slotsPerGroup(); slot++) {
            ItemStack fuel = getItem(slot);
            int time = fuel.getBurnTime(RecipeType.SMELTING);
            if (time <= 0) continue;
            ItemStack remainder = fuel.getCraftingRemainingItem();
            if (fuel.getCount() > 1 && !remainder.isEmpty()) {
                if (!insert(cloud, 0, cloud.getContainerSize(), remainder, true).isEmpty()) continue;
                insert(cloud, 0, cloud.getContainerSize(), remainder, false);
            }
            setItem(slot, fuel.getCount() == 1 ? remainder : fuel.copyWithCount(fuel.getCount() - 1));
            return time;
        }
        return 0;
    }

    public void tick(ServerLevel level, Container cloud) {
        if (count == 0) return;
        ticks++;
        if (autoOutput() && ticks % 8 == 0) for (int i = OUTPUT; i < OUTPUT + slotsPerGroup(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) {
                ItemStack remaining = insert(cloud, 0, cloud.getContainerSize(), stack, false);
                if (remaining.getCount() != stack.getCount()) setItem(i, remaining);
            }
        }
        if (autoFuel() && ticks % 8 == 0) pullFuel(cloud);
        boolean hasInput = false;
        for (int i = 0; i < slotsPerGroup(); i++) if (!getItem(i).isEmpty()) { hasInput = true; break; }
        if (!hasInput) {
            active = 0;
            for (int lane = 0; lane < count; lane++) {
                burn[lane] = Math.max(0, burn[lane] - 1);
                progress[lane] = total[lane] = 0;
                input[lane] = -1;
            }
            return;
        }
        var recipes = new ArrayList<RecipeHolder<SmeltingRecipe>>();
        var outputs = new ArrayList<ItemStack>();
        int[] available = new int[slotsPerGroup()];
        for (int i = 0; i < slotsPerGroup(); i++) {
            var recipe = getItem(i).isEmpty() ? null : quickCheck.getRecipeFor(new SingleRecipeInput(getItem(i)), level).orElse(null);
            recipes.add(recipe);
            outputs.add(recipe == null ? ItemStack.EMPTY : recipe.value().assemble(new SingleRecipeInput(getItem(i)), level.registryAccess()));
            available[i] = recipe == null ? 0 : getItem(i).getCount();
        }
        active = 0;
        for (int lane = 0; lane < count; lane++) {
            if (burn[lane] > 0) burn[lane]--;
            int slot = input[lane];
            if (slot < 0 || slot >= available.length || available[slot] == 0 || !ItemStack.isSameItemSameComponents(ingredient[lane], getItem(slot))) {
                slot = -1;
                for (int n = 0; n < available.length; n++) {
                    int next = (cursor + n) % available.length;
                    if (available[next] > 0) { slot = next; cursor = (next + 1) % available.length; break; }
                }
                progress[lane] = 0;
                ingredient[lane] = slot < 0 ? ItemStack.EMPTY : getItem(slot).copyWithCount(1);
            }
            input[lane] = slot;
            if (slot < 0) { total[lane] = 0; continue; }
            available[slot]--;
            var recipe = recipes.get(slot);
            int cookTime = Math.max(1, recipe.value().getCookingTime());
            if (!recipe.id().equals(recipeIds[lane]) || total[lane] != cookTime) progress[lane] = 0;
            recipeIds[lane] = recipe.id();
            total[lane] = cookTime;
            ItemStack result = outputs.get(slot);
            if (!roomForOutput(result)) continue;
            if (burn[lane] == 0) {
                burn[lane] = duration[lane] = lightBurner(cloud);
            }
            if (burn[lane] == 0) { progress[lane] = Math.max(0, progress[lane] - 2); continue; }
            active++;
            if (++progress[lane] < cookTime) continue;
            // The input stays visible until completion. Each lane reserved one of the available items above.
            insert(this, OUTPUT, slotsPerGroup(), result, false);
            if (getItem(slot).is(Items.WET_SPONGE)) for (int f = FUEL; f < FUEL + slotsPerGroup(); f++) if (getItem(f).is(Items.BUCKET) && getItem(f).getCount() == 1) {
                setItem(f, new ItemStack(Items.WATER_BUCKET)); break;
            }
            removeItem(slot, 1);
            recipesUsed.merge(recipe.id(), 1, (a, b) -> a == Integer.MAX_VALUE ? a : a + b);
            progress[lane] = 0;
            input[lane] = -1;
        }
    }

    public void awardExperience(ServerPlayer player) {
        var recipes = new ArrayList<RecipeHolder<?>>();
        recipesUsed.forEach((id, amount) -> player.level().getRecipeManager().byKey(id).ifPresent(recipe -> {
            if (!(recipe.value() instanceof SmeltingRecipe smelting)) return;
            recipes.add(recipe);
            double exact = (double) amount * smelting.getExperience();
            int xp = (int) Math.min(Integer.MAX_VALUE, Math.floor(exact) + (player.getRandom().nextDouble() < exact % 1 ? 1 : 0));
            if (xp > 0) ExperienceOrb.award(player.serverLevel(), player.position(), xp);
        }));
        player.awardRecipes(recipes);
        recipesUsed.clear();
    }
    public CompoundTag save(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putInt("Count", count); tag.putInt("Flags", flags);
        tag.put("Items", createTag(registries));
        tag.putIntArray("Burn", burn); tag.putIntArray("Duration", duration);
        tag.putIntArray("Progress", progress); tag.putIntArray("Total", total); tag.putIntArray("Input", input);
        var keys = new ListTag();
        for (int i = 0; i < count; i++) {
            var key = new CompoundTag();
            if (!ingredient[i].isEmpty()) key.put("Item", ingredient[i].save(registries));
            if (recipeIds[i] != null) key.putString("Recipe", recipeIds[i].toString());
            keys.add(key);
        }
        tag.put("Keys", keys);
        var used = new CompoundTag();
        recipesUsed.forEach((id, amount) -> used.putInt(id.toString(), amount)); tag.put("Recipes", used);
        return tag;
    }
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        setCount(tag.getInt("Count")); setFlags(tag.getInt("Flags"));
        fromTag(tag.getList("Items", 10), registries);
        loadArray(tag, "Burn", burn); loadArray(tag, "Duration", duration); loadArray(tag, "Progress", progress);
        loadArray(tag, "Total", total); loadArray(tag, "Input", input);
        var keys = tag.getList("Keys", 10);
        for (int i = 0; i < Math.min(count, keys.size()); i++) {
            var key = keys.getCompound(i);
            ingredient[i] = ItemStack.parseOptional(registries, key.getCompound("Item"));
            recipeIds[i] = ResourceLocation.tryParse(key.getString("Recipe"));
        }
        var used = tag.getCompound("Recipes");
        for (String id : used.getAllKeys()) {
            var key = ResourceLocation.tryParse(id);
            if (key != null && used.getInt(id) > 0) recipesUsed.put(key, used.getInt(id));
        }
    }
    private static void loadArray(CompoundTag tag, String key, int[] target) {
        int[] values = tag.getIntArray(key);
        for (int i = 0; i < Math.min(target.length, values.length); i++) target[i] = Math.max(key.equals("Input") ? -1 : 0, values[i]);
    }
}
