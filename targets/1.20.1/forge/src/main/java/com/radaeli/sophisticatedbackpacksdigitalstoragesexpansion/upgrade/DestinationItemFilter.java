package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTarget;

/** Destination-specific filtering; local matching retains Sophisticated memory semantics. */
public final class DestinationItemFilter {
    private DestinationItemFilter() {}

    /** Pass only the real remainder to the next independently filtered destination. */
    public static ItemStack route(SourcePriority priority, ItemStack offered,
            java.util.function.UnaryOperator<ItemStack> remote, java.util.function.UnaryOperator<ItemStack> local) {
        ItemStack remainder = offered.copy();
        if (priority == SourcePriority.NETWORK_FIRST) {
            remainder = remote.apply(remainder);
            return remainder.isEmpty() ? remainder : local.apply(remainder);
        }
        remainder = local.apply(remainder);
        return remainder.isEmpty() ? remainder : remote.apply(remainder);
    }
    public static boolean local(ContentsFilterLogic filters, ItemStack offered) {
        return !offered.isEmpty() && filters.matchesFilter(offered);
    }
    public static boolean remote(ContentsFilterLogic filters, StorageTarget target,
            ServerPlayer player, ItemStack offered) {
        if (target == null || offered.isEmpty()) return false;
        if (filters.getFilterType() != ContentsFilterType.STORAGE) return filters.matchesFilter(offered);
        return target.hasStoredMatching(player, stored -> filters.stackMatchesFilter(offered, stored),
                filters.getPrimaryMatch() == PrimaryMatch.ITEM ? Optional.of(offered.getItem()) : Optional.empty());
    }
}
