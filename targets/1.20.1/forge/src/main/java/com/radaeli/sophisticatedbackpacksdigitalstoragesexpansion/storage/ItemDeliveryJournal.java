package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.DeliveryJournal;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** P08/P09: the last known remainder and in-flight destination travel with the upgrade. */
public final class ItemDeliveryJournal implements DeliveryJournal<ItemStack> {
    private static final String KEY = "remote_item_delivery";
    private final ItemStack upgrade;
    private final HolderLookup.Provider registries;
    private final Runnable save;

    public ItemDeliveryJournal(ItemStack upgrade, HolderLookup.Provider registries, Runnable save) {
        this.upgrade = upgrade;
        this.registries = registries;
        this.save = save;
    }
    private CompoundTag root() { return upgrade.hasTag() ? upgrade.getTag().getCompound(KEY) : new CompoundTag(); }
    private void update(java.util.function.Consumer<CompoundTag> action) {
        var tag = root(); action.accept(tag); if (tag.isEmpty()) upgrade.getOrCreateTag().remove(KEY); else upgrade.getOrCreateTag().put(KEY, tag);
        save.run();
    }
    public static boolean hasPending(ItemStack upgrade) {
        return upgrade.hasTag() && upgrade.getTag().contains(KEY);
    }
    public boolean hasPending() { return hasPending(upgrade); }
    public boolean uncertain() { return root().getBoolean("uncertain"); }
    public ItemStack pending() {
        var tag = root();
        if (!tag.contains("stack")) return ItemStack.EMPTY;
        ItemStack identity = ItemStack.of(tag.getCompound("stack"));
        int amount = tag.contains("amount") ? tag.getInt("amount") : identity.getCount();
        if (amount <= 0) throw new IllegalStateException("Invalid persisted item custody quantity");
        return identity.copyWithCount(amount);
    }
    @Override public void checkpoint(ItemStack remainder) {
        var encoded = remainder.isEmpty() ? null : remainder.copyWithCount(1).save(new CompoundTag());
        update(tag -> {
            tag.remove("uncertain"); tag.remove("stage");
            if (encoded == null) { tag.remove("stack"); tag.remove("amount"); }
            else { tag.put("stack", encoded); tag.putInt("amount", remainder.getCount()); }
        });
    }
    @Override public void beginDelivery(String stage) {
        update(tag -> { tag.putBoolean("uncertain", true); tag.putString("stage", stage); });
    }
    public void clear() { update(tag -> { tag.remove("stack"); tag.remove("amount"); tag.remove("uncertain"); tag.remove("stage"); }); }
}
