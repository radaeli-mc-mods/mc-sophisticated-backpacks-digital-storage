package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.*;
import java.util.function.Predicate;
import de.scc.calculator.api.SccNetwork;
import de.scc.calculator.calc.StackKey;
import de.scc.calculator.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

/** Optional SCC bridge; instantiated only with the audited mod installed. */
public final class SccStorageAdapter implements StorageAdapter {
    public String backendId() { return "scc"; }
    public Optional<StorageEndpoint> identify(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof NetworkAntennaBlockEntity
                ? Optional.of(new StorageEndpoint("scc", level.dimension(), pos.immutable())) : Optional.empty();
    }
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        return resolve(level, endpoint, null);
    }
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint, SourceLocation source) {
        return identify(level, endpoint.pos()).map(ignored -> new Target(endpoint, source));
    }
    private record Target(StorageEndpoint endpoint, SourceLocation source) implements StorageTarget {
        private Optional<NetworkView> view(ServerPlayer player, SccSecurity.Right right) {
            return SccAccessPolicy.resolve(player, endpoint, source, right);
        }
        public boolean isReachable(ServerPlayer player, SourceLocation origin) {
            return SccAccessPolicy.resolve(player, endpoint, origin, null).isPresent();
        }
        public boolean canBind(ServerPlayer player, SourceLocation origin) { return isReachable(player, origin); }
        public boolean canAccess(ServerPlayer player, SourceLocation origin, StorageOperation operation) {
            return SccAccessPolicy.resolve(player, endpoint, origin,
                    operation == StorageOperation.INSERT ? SccSecurity.Right.PUT : SccSecurity.Right.TAKE).isPresent();
        }
        public boolean hasStoredMatching(ServerPlayer player, Predicate<ItemStack> matches, Optional<Item> item) {
            return !list(player, stack -> item.map(stack::is).orElse(true) && matches.test(stack),
                    Optional.empty(), 1, null).isEmpty();
        }
        public IItemHandler getAvailableItems(ServerPlayer player) {
            return new SnapshotItemHandler(list(player, stack -> true, Optional.empty(), Integer.MAX_VALUE, null)
                    .stream().map(StoredItemAmount::exemplar).toList());
        }
        public List<StoredItemAmount> getAvailableItemAmounts(ServerPlayer player) {
            return queryItems(player, stack -> true, Optional.empty(), Integer.MAX_VALUE);
        }
        public List<StoredItemAmount> queryItems(ServerPlayer player, Predicate<ItemStack> matches, Optional<Item> item, int needed) {
            return list(player, matches, item, needed, SccSecurity.Right.TAKE);
        }
        private List<StoredItemAmount> list(ServerPlayer player, Predicate<ItemStack> matches,
                Optional<Item> item, int needed, SccSecurity.Right right) {
            if (needed <= 0) return List.of();
            var resolved = view(player, right);
            if (resolved.isEmpty()) return List.of();
            var network = resolved.get();
            Map<StackKey, Long> stock = item.isPresent() ? new LinkedHashMap<>() : new LinkedHashMap<>(network.contents());
            if (item.isEmpty()) AutoCompress.expand(stock, AutoCompress.parse(AutoCompress.syncText(network)));
            Set<Item> types = new LinkedHashSet<>();
            if (item.isPresent()) types.add(item.get());
            else stock.keySet().forEach(key -> types.add(key.item()));
            List<StoredItemAmount> result = new ArrayList<>();
            for (Item type : types) {
                List<ItemStack> candidates = new ArrayList<>(network.find(type, matches, Integer.MAX_VALUE));
                ItemStack plain = new ItemStack(type);
                if (matches.test(plain) && (right == null ? stock.getOrDefault(StackKey.of(plain), 0L) > 0 : network.free(StackKey.of(plain)) > 0)) candidates.add(plain);
                for (ItemStack candidate : candidates) {
                    if (candidate.isEmpty() || !matches.test(candidate)
                            || result.stream().anyMatch(value -> ItemStack.isSameItemSameComponents(value.exemplar(), candidate))) continue;
                    if (right == null) {
                        result.add(new StoredItemAmount(candidate.copyWithCount(1), 1));
                        if (needed == 1) return List.copyOf(result);
                        continue;
                    }
                    long free = network.free(StackKey.of(candidate));
                    if (free <= 0) continue;
                    int count = simulate(candidate, (int)Math.min(free, needed), network).getCount();
                    if (count > 0) result.add(new StoredItemAmount(candidate.copyWithCount(1), count));
                }
            }
            return List.copyOf(result);
        }
        public int simulateInsert(ItemStack stack, ServerPlayer player) {
            return stack.getCount() - view(player, SccSecurity.Right.PUT)
                    .map(network -> SccNetwork.insert(network, stack.copy(), true)).orElse(stack).getCount();
        }
        public ItemStack insert(ItemStack stack, ServerPlayer player) {
            return view(player, SccSecurity.Right.PUT).map(network -> SccNetwork.insert(network, stack.copy(), false)).orElseGet(stack::copy);
        }
        private static ItemStack simulate(ItemStack stack, int amount, NetworkView network) {
            int limit = (int)Math.min(Math.min(Integer.MAX_VALUE - 9, Math.max(0, amount)), network.free(StackKey.of(stack)));
            if (limit <= 0) return ItemStack.EMPTY;
            if (stack.getComponentsPatch().isEmpty()) {
                ItemStack estimate = SccNetwork.extract(network, stack.copy(), limit, true);
                if (ItemStack.isSameItemSameComponents(stack, estimate)) return estimate;
            }
            return network.extractExact(stack.copy(), limit, true);
        }
        public int simulateExtract(ItemStack stack, int amount, ServerPlayer player) {
            return view(player, SccSecurity.Right.TAKE).map(network -> simulate(stack, amount, network).getCount()).orElse(0);
        }
        public ItemStack extract(ItemStack stack, int amount, ServerPlayer player) {
            var resolved = view(player, SccSecurity.Right.TAKE);
            if (resolved.isEmpty() || stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            NetworkView network = resolved.get();
            int limit = (int)Math.min(Math.min(amount, Integer.MAX_VALUE - 9), network.free(StackKey.of(stack)));
            if (limit <= 0) return ItemStack.EMPTY;
            if (stack.getComponentsPatch().isEmpty()) {
                ItemStack estimate = SccNetwork.extract(network, stack.copy(), limit, true);
                if (ItemStack.isSameItemSameComponents(stack, estimate))
                    return SccNetwork.extract(network, stack.copy(), Math.min(limit, estimate.getCount()), false);
            }
            return network.extractExact(stack.copy(), limit, false);
        }
    }
}
