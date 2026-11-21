package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.*;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.items.IItemHandler;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.network.INetworkIngredientsChannel;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;

/** P03/P08/P11: native wildcard item channel, with fresh access validation on every call. */
public final class IntegratedDynamicsStorageAdapter implements StorageAdapter {
    public String backendId() { return "integrateddynamics"; }
    public Optional<StorageEndpoint> identify(Level level, BlockPos pos) {
        return IntegratedDynamicsLinkDiscovery.identify(level, pos);
    }
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        return resolve(level, endpoint, null);
    }
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint, SourceLocation source) {
        if (!backendId().equals(endpoint.backendId()) || !level.dimension().equals(endpoint.dimension())
                || !level.isLoaded(endpoint.pos())) return Optional.empty();
        return Optional.of(new Target(endpoint, source));
    }
    private record Target(StorageEndpoint endpoint, SourceLocation source) implements StorageTarget {
        private Optional<IPositionedAddonsNetworkIngredients<ItemStack, Integer>> network(ServerPlayer player, SourceLocation origin) {
            return IntegratedDynamicsAccessPolicy.ingredients(player, endpoint, origin, IngredientComponent.ITEMSTACK);
        }
        private Optional<INetworkIngredientsChannel<ItemStack, Integer>> channel(ServerPlayer player) {
            return network(player, source).map(network -> network.getChannel(IPositionedAddonsNetwork.WILDCARD_CHANNEL));
        }
        public boolean canBind(ServerPlayer player, SourceLocation origin) {
            return IntegratedDynamicsAccessPolicy.resolve(player, endpoint, origin).isPresent();
        }
        public boolean isReachable(ServerPlayer player, SourceLocation origin) { return canBind(player, origin); }
        public boolean canAccess(ServerPlayer player, SourceLocation origin, StorageOperation operation) {
            return network(player, origin).isPresent();
        }
        public IItemHandler getAvailableItems(ServerPlayer player) {
            return new SnapshotItemHandler(getAvailableItemAmounts(player).stream().map(StoredItemAmount::exemplar).toList());
        }
        public List<StoredItemAmount> getAvailableItemAmounts(ServerPlayer player) {
            return queryItems(player, stack -> true, Optional.empty(), Integer.MAX_VALUE);
        }
        public List<StoredItemAmount> queryItems(ServerPlayer player, java.util.function.Predicate<ItemStack> matches,
                Optional<Item> item, int needed) {
            if (needed <= 0) return List.of();
            var resolved = network(player, source);
            if (resolved.isEmpty()) return List.of();
            var channel = IntegratedDynamicsAccessPolicy.liveChannel(resolved.get());
            var identities = item.map(type -> channel.iterator(new ItemStack(type), ItemMatch.ITEM)).orElseGet(channel::iterator);
            var amounts = new IngredientCollectionPrototypeMap<>(IngredientComponent.ITEMSTACK);
            identities.forEachRemaining(amounts::add);
            var result = new ArrayList<StoredItemAmount>();
            var prototypes = amounts.prototypeIterator();
            while (prototypes.hasNext()) {
                var entry = prototypes.next();
                ItemStack identity = entry.getKey();
                long amount = entry.getValue();
                if (!identity.isEmpty() && amount > 0 && matches.test(identity.copyWithCount(1))) {
                    result.add(new StoredItemAmount(identity, amount));
                }
            }
            return List.copyOf(result);
        }
        public boolean hasStoredMatching(ServerPlayer player, java.util.function.Predicate<ItemStack> matches, Optional<Item> item) {
            var resolved = network(player, source);
            if (resolved.isEmpty()) return false;
            var channel = IntegratedDynamicsAccessPolicy.liveChannel(resolved.get());
            var identities = item.map(type -> channel.iterator(new ItemStack(type), ItemMatch.ITEM)).orElseGet(channel::iterator);
            while (identities.hasNext()) {
                var identity = identities.next();
                if (!identity.isEmpty() && matches.test(identity.copyWithCount(1))) return true;
            }
            return false;
        }
        private ItemStack insert(ItemStack stack, ServerPlayer player, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            var resolved = channel(player);
            if (resolved.isEmpty()) return stack.copy();
            try (Transaction tx = Transaction.openRoot()) {
                var remainder = resolved.get().insert(stack.copy(), tx);
                if (!simulate) tx.commit();
                return remainder;
            }
        }
        public int simulateInsert(ItemStack stack, ServerPlayer player) {
            return ItemInsertion.acceptedCount(stack, (long) stack.getCount() - insert(stack, player, true).getCount());
        }
        public ItemStack insert(ItemStack stack, ServerPlayer player) { return insert(stack, player, false); }
        private ItemStack extract(ItemStack identity, int amount, ServerPlayer player, boolean simulate) {
            if (identity.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            var request = identity.copyWithCount(Math.min(amount, identity.getMaxStackSize()));
            var resolved = network(player, source);
            if (resolved.isEmpty()) return ItemStack.EMPTY;
            try (Transaction tx = Transaction.openRoot()) {
                var extracted = IntegratedDynamicsAccessPolicy.liveChannel(resolved.get()).extract(request,
                        IngredientComponent.ITEMSTACK.getMatcher().getExactMatchNoQuantityCondition(), tx);
                if (!simulate) tx.commit();
                return extracted;
            }
        }
        public int simulateExtract(ItemStack identity, int amount, ServerPlayer player) { return extract(identity, amount, player, true).getCount(); }
        public ItemStack extract(ItemStack identity, int amount, ServerPlayer player) { return extract(identity, amount, player, false); }
    }
}
