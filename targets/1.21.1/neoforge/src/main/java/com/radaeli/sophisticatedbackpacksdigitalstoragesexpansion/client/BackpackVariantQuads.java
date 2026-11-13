package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant;

/**
 * Re-skins the quads Sophisticated bakes for its backpack model.
 *
 * <p>Cloth ({@code tintindex 0}), border ({@code tintindex 1}) and the tier clips keep the exact geometry and UV
 * layout of the original model; only the sprite changes and the tint is dropped, because the variant textures
 * already carry their colors. Every other quad (tanks, battery, modules, display anchor) is returned untouched.</p>
 */
public final class BackpackVariantQuads {
    private static final String SOPHISTICATED = "sophisticatedbackpacks";
    private static final int VERTEX_STRIDE = 8;
    private static final int U_OFFSET = 4;
    private static final int V_OFFSET = 5;

    /** Set on placed backpacks' model data from their color components. */
    public static final ModelProperty<BackpackVariant> VARIANT = new ModelProperty<>();

    /**
     * Variant of the stack most recently resolved by Sophisticated's item override. Item models are resolved right
     * before they are drawn, always on the render thread; block models use {@link #VARIANT} instead.
     */
    private static BackpackVariant itemVariant;

    private static final Map<BackpackVariant, Map<BakedQuad, BakedQuad>> CACHE = new EnumMap<>(BackpackVariant.class);

    private BackpackVariantQuads() {
    }

    public static void setItemVariant(BackpackVariant variant) {
        itemVariant = variant;
    }

    public static BackpackVariant itemVariant() {
        return itemVariant;
    }

    public static List<BakedQuad> apply(List<BakedQuad> quads, BackpackVariant variant) {
        if (variant == null || quads.isEmpty() || !variant.isAvailable()) return quads;
        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            BakedQuad skinned = skin(quad, variant);
            if (skinned != quad) {
                if (result == null) result = new ArrayList<>(quads);
                result.set(i, skinned);
            }
        }
        return result == null ? quads : result;
    }

    private static String layer(BakedQuad quad) {
        var name = quad.getSprite().contents().name();
        if (!SOPHISTICATED.equals(name.getNamespace())) return null;
        String path = name.getPath();
        if (path.equals("block/backpack_cloth")) return "cloth";
        if (path.equals("block/backpack_border")) return "border";
        if (path.startsWith("block/") && path.endsWith("_clips")) return "clips";
        return null;
    }

    private static BakedQuad skin(BakedQuad quad, BackpackVariant variant) {
        String layer = layer(quad);
        if (layer == null) return quad;
        Map<BakedQuad, BakedQuad> cache;
        synchronized (CACHE) {
            cache = CACHE.computeIfAbsent(variant, key -> Collections.synchronizedMap(new WeakHashMap<>()));
        }
        BakedQuad cached = cache.get(quad);
        if (cached != null) return cached;
        TextureAtlasSprite target = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(variant.texture(layer));
        if (target.contents().name().equals(MissingTextureAtlasSprite.getLocation())) return quad;
        BakedQuad skinned = resprite(quad, target);
        cache.put(quad, skinned);
        return skinned;
    }

    private static BakedQuad resprite(BakedQuad quad, TextureAtlasSprite target) {
        TextureAtlasSprite source = quad.getSprite();
        int[] vertices = quad.getVertices().clone();
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * VERTEX_STRIDE;
            float u = Float.intBitsToFloat(vertices[base + U_OFFSET]);
            float v = Float.intBitsToFloat(vertices[base + V_OFFSET]);
            float relativeU = (u - source.getU0()) / (source.getU1() - source.getU0());
            float relativeV = (v - source.getV0()) / (source.getV1() - source.getV0());
            vertices[base + U_OFFSET] = Float.floatToRawIntBits(target.getU0() + relativeU * (target.getU1() - target.getU0()));
            vertices[base + V_OFFSET] = Float.floatToRawIntBits(target.getV0() + relativeV * (target.getV1() - target.getV0()));
        }
        return new BakedQuad(vertices, -1, quad.getDirection(), target, quad.isShade(), quad.hasAmbientOcclusion());
    }
}
