package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.integrateddynamics.api.network.INetworkIngredientsChannel;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.core.network.IngredientChannelPositioned;
import org.cyclops.integrateddynamics.core.network.PositionedAddonsNetworkIngredients;

/** P03/P08/P11: native wildcard item channel, revalidated against the live Forge network. */
public final class IntegratedDynamicsStorageAdapter implements StorageAdapter {
    @Override
    public String backendId() {
        return "integrateddynamics";
    }

    @Override
    public Optional<StorageEndpoint> identify(Level level, BlockPos pos) {
        return IntegratedDynamicsLinkDiscovery.identify(level, pos);
    }

    @Override
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (!backendId().equals(endpoint.backendId()) || !level.dimension().equals(endpoint.dimension())
                || !level.isLoaded(endpoint.pos())) {
            return Optional.empty();
        }
        return Optional.of(new Target(endpoint));
    }

    private record Target(StorageEndpoint endpoint) implements StorageTarget {
        private Optional<IPositionedAddonsNetworkIngredients<ItemStack, Integer>> network(ServerPlayer player) {
            return IntegratedDynamicsAccessPolicy.ingredients(player, endpoint, IngredientComponent.ITEMSTACK);
        }

        private Optional<INetworkIngredientsChannel<ItemStack, Integer>> channel(ServerPlayer player) {
            return network(player).map(value -> IntegratedDynamicsAccessPolicy.liveChannel(value));
        }

        @Override
        public boolean isReachable(ServerPlayer player, SourceLocation source) {
            return IntegratedDynamicsAccessPolicy.resolve(player, endpoint, source).isPresent();
        }

        @Override
        public boolean canBind(ServerPlayer player, SourceLocation source) {
            return IntegratedDynamicsAccessPolicy.resolve(player, endpoint, source).isPresent();
        }

        @Override
        public boolean canAccess(ServerPlayer player, SourceLocation source, StorageOperation operation) {
            return IntegratedDynamicsAccessPolicy.ingredients(player, endpoint, source, IngredientComponent.ITEMSTACK).isPresent();
        }

        @Override
        public IItemHandler getAvailableItems(ServerPlayer player) {
            return new SnapshotItemHandler(getAvailableItemAmounts(player).stream()
                    .map(StoredItemAmount::exemplar).toList());
        }

        @Override
        public List<StoredItemAmount> getAvailableItemAmounts(ServerPlayer player) {
            return queryItems(player, stack -> true, Optional.empty(), Integer.MAX_VALUE);
        }

        private List<StoredItemAmount> queryItems(ServerPlayer player, Predicate<ItemStack> matches,
                Optional<Item> item, int needed) {
            if (needed <= 0) {
                return List.of();
            }
            Optional<IPositionedAddonsNetworkIngredients<ItemStack, Integer>> resolved = network(player);
            if (resolved.isEmpty()) {
                return List.of();
            }
            var nativeChannel = IntegratedDynamicsAccessPolicy.liveChannel(resolved.get());
            var identities = item.map(type -> nativeChannel.iterator(new ItemStack(type), ItemMatch.ITEM))
                    .orElseGet(nativeChannel::iterator);
            var amounts = new IngredientCollectionPrototypeMap<>(IngredientComponent.ITEMSTACK);
            identities.forEachRemaining(amounts::add);

            List<StoredItemAmount> result = new ArrayList<>();
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

        @Override
        public boolean hasStoredMatching(ServerPlayer player, Predicate<ItemStack> matches, Optional<Item> item) {
            Optional<IPositionedAddonsNetworkIngredients<ItemStack, Integer>> resolved = network(player);
            if (resolved.isEmpty()) {
                return false;
            }
            var nativeChannel = IntegratedDynamicsAccessPolicy.liveChannel(resolved.get());
            var identities = item.map(type -> nativeChannel.iterator(new ItemStack(type), ItemMatch.ITEM))
                    .orElseGet(nativeChannel::iterator);
            while (identities.hasNext()) {
                ItemStack identity = identities.next();
                if (!identity.isEmpty() && matches.test(identity.copyWithCount(1))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int simulateInsert(ItemStack stack, ServerPlayer player) {
            if (stack.isEmpty()) {
                return 0;
            }
            ItemStack remainder = insert(stack, player, true);
            return ItemInsertion.acceptedCount(stack, (long) stack.getCount() - remainder.getCount());
        }

        @Override
        public ItemStack insert(ItemStack stack, ServerPlayer player) {
            return insert(stack, player, false);
        }

        private ItemStack insert(ItemStack stack, ServerPlayer player, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return channel(player).map(value -> value.insert(stack.copy(), simulate)).orElseGet(stack::copy);
        }

        @Override
        public int simulateExtract(ItemStack identity, int amount, ServerPlayer player) {
            return extract(identity, amount, player, true).getCount();
        }

        @Override
        public ItemStack extract(ItemStack identity, int amount, ServerPlayer player) {
            return extract(identity, amount, player, false);
        }

        private ItemStack extract(ItemStack identity, int amount, ServerPlayer player, boolean simulate) {
            if (identity.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack request = identity.copyWithCount(Math.min(amount, identity.getMaxStackSize()));
            return network(player).map(value -> IntegratedDynamicsAccessPolicy.liveChannel(value).extract(request,
                    IngredientComponent.ITEMSTACK.getMatcher().getExactMatchNoQuantityCondition(), simulate))
                    .orElse(ItemStack.EMPTY);
        }
    }
}
