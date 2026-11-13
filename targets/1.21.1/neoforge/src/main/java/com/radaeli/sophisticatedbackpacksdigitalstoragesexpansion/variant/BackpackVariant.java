package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant;

import java.util.Optional;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;

/**
 * Network "skins" for Sophisticated backpacks.
 *
 * <p>A variant has no data of its own: crafting only rewrites the backpack's two color components to the variant's
 * reserved palette, exactly like a dye recipe. The client recognizes a variant by that exact color pair and swaps the
 * cloth/border/clips textures. Dyeing the backpack again changes the colors and therefore drops the skin.</p>
 */
public enum BackpackVariant implements StringRepresentable {
    AE2("ae2", "ae2", "ae2:fluix_crystal", 0x6B46A8, 0x2B1F45),
    RS("rs", "refinedstorage", "refinedstorage:raw_advanced_processor", 0x3D7FC4, 0x16304F),
    TOMS("toms", "toms_storage", "toms_storage:inventory_connector", 0xA6763F, 0x3B2813),
    SCC("scc", "scc", "scc:storage_connector", 0xE0A71E, 0x5A3A06);

    public static final Codec<BackpackVariant> CODEC = StringRepresentable.fromEnum(BackpackVariant::values);
    public static final StreamCodec<ByteBuf, BackpackVariant> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(32).map(BackpackVariant::byId, BackpackVariant::getSerializedName);

    private final String id;
    private final String modId;
    private final String marker;
    private final int mainColor;
    private final int accentColor;

    BackpackVariant(String id, String modId, String marker, int mainColor, int accentColor) {
        this.id = id;
        this.modId = modId;
        this.marker = marker;
        this.mainColor = mainColor;
        this.accentColor = accentColor;
    }

    /** Mod whose presence enables this variant: its recipe is conditional on it and the skin only renders with it. */
    public String modId() {
        return modId;
    }

    /** Registry ID of the item that crafts this variant (the recipe JSON carries the same value). */
    public String markerItemId() {
        return marker;
    }

    public boolean isAvailable() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(modId);
    }

    public int mainColor() {
        return mainColor;
    }

    public int accentColor() {
        return accentColor;
    }

    /** Texture of one backpack layer ({@code cloth}, {@code border} or {@code clips}) in the block atlas. */
    public ResourceLocation texture(String layer) {
        return ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID,
                "block/backpack_variant/" + id + "_" + layer);
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    private static BackpackVariant byId(String id) {
        for (BackpackVariant variant : values()) {
            if (variant.id.equals(id)) return variant;
        }
        throw new IllegalArgumentException("Unknown backpack variant " + id);
    }

    /** Alpha is ignored: Sophisticated stores the colors opaque. */
    public static Optional<BackpackVariant> fromColors(int main, int accent) {
        for (BackpackVariant variant : values()) {
            if ((variant.mainColor & 0xFFFFFF) == (main & 0xFFFFFF)
                    && (variant.accentColor & 0xFFFFFF) == (accent & 0xFFFFFF)) {
                return Optional.of(variant);
            }
        }
        return Optional.empty();
    }

    /** A backpack of the given tier already skinned with this variant, as crafting would produce it. */
    public ItemStack stack(net.minecraft.world.item.Item backpack) {
        ItemStack stack = new ItemStack(backpack);
        BackpackItem.setColors(stack, mainColor, accentColor);
        return stack;
    }

    public static Optional<BackpackVariant> fromStack(ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackItem)) return Optional.empty();
        return fromColors(BackpackItem.getMainColor(stack), BackpackItem.getAccentColor(stack));
    }
}
