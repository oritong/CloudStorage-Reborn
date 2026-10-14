package com.oritong.cloudstorage.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Keeps display positions stable even when returned items occupy different cloud slots. */
public final class CloudTerminalOrder {
    private CloudTerminalOrder() { }

    public static List<CloudTerminalEntry> refreshLocked(List<CloudTerminalEntry> previous,
                                                        List<CloudTerminalEntry> current,
                                                        Predicate<CloudTerminalEntry> matchesSearch) {
        Map<SourceKey, Integer> bySource = new HashMap<>();
        Map<ResourceKey, ArrayDeque<Integer>> byResource = new HashMap<>();
        for (int i = 0; i < current.size(); i++) {
            var entry = current.get(i);
            var resource = new ResourceKey(entry);
            bySource.put(new SourceKey(resource, entry.isFluid() ? -1 : entry.source()), i);
            byResource.computeIfAbsent(resource, unused -> new ArrayDeque<>()).addLast(i);
        }

        int[] assignments = new int[previous.size()];
        Arrays.fill(assignments, -1);
        BitSet claimed = new BitSet(current.size());
        // Reserve all unchanged sources first, so a depleted position cannot steal another live stack.
        for (int i = 0; i < previous.size(); i++) {
            var entry = previous.get(i);
            var index = bySource.get(new SourceKey(new ResourceKey(entry), entry.isFluid() ? -1 : entry.source()));
            if (index != null && !claimed.get(index)) {
                assignments[i] = index;
                claimed.set(index);
            }
        }
        // Reuse matching positions when reinsertion, merging or sorting changes the underlying source.
        for (int i = 0; i < previous.size(); i++) {
            if (assignments[i] >= 0) continue;
            var candidates = byResource.get(new ResourceKey(previous.get(i)));
            if (candidates == null) continue;
            while (!candidates.isEmpty() && claimed.get(candidates.peekFirst())) candidates.removeFirst();
            if (!candidates.isEmpty()) {
                int index = candidates.removeFirst();
                assignments[i] = index;
                claimed.set(index);
            }
        }

        List<CloudTerminalEntry> refreshed = new ArrayList<>(previous.size());
        for (int i = 0; i < previous.size(); i++) {
            var key = previous.get(i);
            refreshed.add(assignments[i] >= 0 ? current.get(assignments[i])
                    : new CloudTerminalEntry(key.source(), key.item(), key.fluid(), 0));
        }
        for (int i = 0; i < current.size(); i++) {
            if (!claimed.get(i) && matchesSearch.test(current.get(i))) refreshed.add(current.get(i));
        }
        return List.copyOf(refreshed);
    }

    private record SourceKey(ResourceKey resource, int source) { }

    /** Counts and physical slots do not identify a resource; data components do. */
    private record ResourceKey(CloudTerminalEntry entry) {
        @Override public boolean equals(Object other) {
            return other instanceof ResourceKey key && entry.isFluid() == key.entry.isFluid()
                    && (entry.isFluid() ? FluidStack.isSameFluidSameComponents(entry.fluid(), key.entry.fluid())
                    : ItemStack.isSameItemSameComponents(entry.item(), key.entry.item()));
        }
        @Override public int hashCode() {
            return entry.isFluid() ? 31 * FluidStack.hashFluidAndComponents(entry.fluid()) + 1
                    : 31 * ItemStack.hashItemAndComponents(entry.item());
        }
    }
}
