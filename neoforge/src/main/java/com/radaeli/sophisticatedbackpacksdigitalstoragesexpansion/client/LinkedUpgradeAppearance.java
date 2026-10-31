package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.EndpointBinding;

/**
 * Remote upgrade visual line (docs/design/UPGRADE_VISUALS.md), driven by item model overrides so it
 * applies in slots, in hand and in the world. "network" picks the linked sprite. Right after a link,
 * "link_frame" switches to the upgrade's canvas sprite, and the frame chosen by the eased timeline is
 * uploaded into that canvas from the strip for the previous state and the new network.
 * Two stacks of the same upgrade animating at once share the canvas.
 */
public final class LinkedUpgradeAppearance {
    private static final Logger LOGGER = LogUtils.getLogger();
    // Matches the order of the model overrides; 0 keeps the unlinked sprite.
    private static final List<String> NETWORKS = List.of("ae2", "refinedstorage", "toms_storage", "scc");
    private static final ResourceLocation NETWORK_PROPERTY = id("network");
    private static final ResourceLocation LINK_FRAME_PROPERTY = id("link_frame");
    // Strip frames: 0 previous state, 1..27 rim fill, 28..37 diagonal screen wipe, 38..64 rim retract.
    // {first frame, last frame, start ms, duration ms, easing}; mirrors design/upgrade-refresh/timeline.py.
    private static final int EASE_IN_OUT = 0;
    private static final int EASE_OUT = 1;
    private static final int[][] PHASES = {
            {0, 27, 0, 380, EASE_IN_OUT},
            {27, 37, 380, 280, EASE_OUT},
            {37, 64, 720, 360, EASE_IN_OUT}
    };
    private static final float DURATION = PHASES[2][2] + PHASES[2][3];
    private static final Map<Item, Canvas> CANVASES = new HashMap<>();

    private LinkedUpgradeAppearance() {
    }

    /** Must run on the main thread during client setup. */
    public static void register(Item item, String itemName) {
        Canvas canvas = new Canvas(itemName);
        CANVASES.put(item, canvas);
        ItemProperties.register(item, NETWORK_PROPERTY, (stack, level, entity, seed) -> networkIndex(stack));
        ItemProperties.register(item, LINK_FRAME_PROPERTY, (stack, level, entity, seed) -> canvas.show(stack) ? 1 : 0);
    }

    private static int networkIndex(ItemStack stack) {
        return EndpointBinding.read(stack).map(endpoint -> NETWORKS.indexOf(endpoint.backendId()) + 1).orElse(0);
    }

    /** Frame shown {@code ms} after the link, or -1 outside the animation. */
    private static int frameAt(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        var linkedAt = EndpointBinding.linkedAt(stack);
        if (level == null || linkedAt.isEmpty()) return -1;
        float ms = (level.getGameTime() - linkedAt.getAsLong()
                + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) * 50F;
        if (ms < 0F || ms >= DURATION) return -1;
        float frame = 0F;
        for (int[] phase : PHASES) {
            if (ms < phase[2]) break;
            float t = Math.min(1F, (ms - phase[2]) / phase[3]);
            float eased = phase[4] == EASE_OUT ? 1F - cube(1F - t)
                    : t < 0.5F ? 4F * t * t * t : 1F - cube(-2F * t + 2F) / 2F;
            frame = phase[0] + (phase[1] - phase[0]) * eased;
        }
        return Math.round(frame);
    }

    private static float cube(float value) {
        return value * value * value;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID, path);
    }

    /** One atlas sprite per upgrade that receives the current animation frame. */
    private static final class Canvas {
        private final String itemName;
        private final ResourceLocation spriteId;
        private final Map<String, Optional<NativeImage>> strips = new HashMap<>();
        private TextureAtlasSprite sprite;
        private String uploaded = "";

        private Canvas(String itemName) {
            this.itemName = itemName;
            this.spriteId = id("item/" + itemName + "_link_canvas");
        }

        /** Uploads the current frame and returns true while the link animation plays. */
        boolean show(ItemStack stack) {
            int network = networkIndex(stack);
            int frame = frameAt(stack);
            if (network == 0 || frame < 0 || !RenderSystem.isOnRenderThread()) return false;
            String from = EndpointBinding.linkedFrom(stack).filter(NETWORKS::contains).orElse("unlinked");
            String strip = from + "_to_" + NETWORKS.get(network - 1);
            Optional<NativeImage> image = strip(strip);
            if (image.isEmpty()) return false;

            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
            TextureAtlasSprite current = atlas.getSprite(spriteId);
            if (current != sprite) {
                // Resource reload: new atlas layout and possibly new strips.
                sprite = current;
                uploaded = "";
                strips.values().forEach(value -> value.ifPresent(NativeImage::close));
                strips.clear();
                image = strip(strip);
                if (image.isEmpty()) return false;
            }
            String key = strip + "#" + frame;
            if (!key.equals(uploaded)) {
                atlas.bind();
                image.get().upload(0, sprite.getX(), sprite.getY(), 0, frame * 16, 16, 16,
                        sprite.contents().byMipLevel.length > 1, false);
                uploaded = key;
            }
            return true;
        }

        private Optional<NativeImage> strip(String name) {
            return strips.computeIfAbsent(name, key -> {
                ResourceLocation location = id("textures/link_animation/" + itemName + "/" + key + ".png");
                var resource = Minecraft.getInstance().getResourceManager().getResource(location);
                if (resource.isEmpty()) return Optional.empty();
                try (InputStream input = resource.get().open()) {
                    return Optional.of(NativeImage.read(input));
                } catch (IOException exception) {
                    LOGGER.warn("Could not read link animation {}", location, exception);
                    return Optional.empty();
                }
            });
        }
    }
}
