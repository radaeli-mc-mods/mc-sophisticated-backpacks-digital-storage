package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client.BackpackVariantQuads;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant;

/** Records which skin the backpack stack about to be drawn uses (inventory, hand, ground and worn rendering). */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackDynamicModel$BackpackItemOverrideList", remap = false)
public abstract class BackpackItemOverrideListMixin {
    // Sophisticated 1.20.1 is compiled against SRG names, so the inherited vanilla method is `m_173464_` in production
    // and `resolve` once the dev environment remaps the mod. Exactly one of them exists at runtime.
    @Inject(method = {"resolve", "m_173464_"}, at = @At("HEAD"), require = 1)
    private void sophisticatedBackpackIsAddon$rememberVariant(BakedModel model, ItemStack stack, ClientLevel level,
            LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
        BackpackVariantQuads.setItemVariant(BackpackVariant.fromStack(stack).orElse(null));
    }
}
