package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

/** Linked upgrade base for functions whose source order is configurable. */
public abstract class PriorityLinkedStorageUpgradeWrapper<W extends PriorityLinkedStorageUpgradeWrapper<W, T>,
        T extends LinkedStorageUpgradeItem<W>> extends LinkedStorageUpgradeWrapper<W, T> {
    private static final String SOURCE_PRIORITY = "source_priority";

    protected PriorityLinkedStorageUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
            Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
    }

    public final SourcePriority getSourcePriority() {
        return readSourcePriority(upgrade);
    }

    static SourcePriority readSourcePriority(ItemStack upgrade) {
        CompoundTag data = upgrade.getTag();
        if (data == null) {
            return SourcePriority.NETWORK_FIRST;
        }
        return data.contains(SOURCE_PRIORITY)
                && SourcePriority.BACKPACK_FIRST.getSerializedName().equals(data.getString(SOURCE_PRIORITY))
                        ? SourcePriority.BACKPACK_FIRST
                        : SourcePriority.NETWORK_FIRST;
    }

    public final void setSourcePriority(SourcePriority priority) {
        writeSourcePriority(upgrade, priority);
        save();
    }

    static void writeSourcePriority(ItemStack upgrade, SourcePriority priority) {
        upgrade.getOrCreateTag().putString(SOURCE_PRIORITY, priority.getSerializedName());
    }

    protected final CompoundTag copyUpgradeData() {
        CompoundTag data = upgrade.getTag();
        return data == null ? new CompoundTag() : data.copy();
    }
}
