package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;

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

    private static final Map<BackpackVariant, Map<BakedQuad, BakedQuad>> CACHE = new EnumMap<>(BackpackVariant.class);

    private BackpackVariantQuads() {
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

    public static BlockStateModelPart apply(BlockStateModelPart part, BackpackVariant variant) {
        return variant == null || !variant.isAvailable() ? part : new SkinnedPart(part, variant);
    }

    private static String layer(BakedQuad quad) {
        var name = quad.materialInfo().sprite().contents().name();
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
        TextureAtlasSprite target = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(variant.texture(layer));
        if (target.contents().name().equals(MissingTextureAtlasSprite.getLocation())) return quad;
        BakedQuad skinned = resprite(quad, target);
        cache.put(quad, skinned);
        return skinned;
    }

    private static long remap(long packedUv, TextureAtlasSprite source, TextureAtlasSprite target) {
        float u = UVPair.unpackU(packedUv);
        float v = UVPair.unpackV(packedUv);
        float relativeU = (u - source.getU0()) / (source.getU1() - source.getU0());
        float relativeV = (v - source.getV0()) / (source.getV1() - source.getV0());
        return UVPair.pack(target.getU0() + relativeU * (target.getU1() - target.getU0()),
                target.getV0() + relativeV * (target.getV1() - target.getV0()));
    }

    private static BakedQuad resprite(BakedQuad quad, TextureAtlasSprite target) {
        BakedQuad.MaterialInfo info = quad.materialInfo();
        TextureAtlasSprite source = info.sprite();
        BakedQuad.MaterialInfo skinned = new BakedQuad.MaterialInfo(target, info.layer(), info.itemRenderType(), -1,
                info.shade(), info.lightEmission(), info.ambientOcclusion());
        return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                remap(quad.packedUV0(), source, target), remap(quad.packedUV1(), source, target),
                remap(quad.packedUV2(), source, target), remap(quad.packedUV3(), source, target),
                quad.direction(), skinned, quad.bakedNormals(), quad.bakedColors());
    }

    /** A model part whose backpack quads are re-skinned on demand; everything else is delegated. */
    private record SkinnedPart(BlockStateModelPart delegate, BackpackVariant variant) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            return apply(delegate.getQuads(direction), variant);
        }

        @Override
        @Deprecated
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return delegate.materialFlags();
        }
    }
}
