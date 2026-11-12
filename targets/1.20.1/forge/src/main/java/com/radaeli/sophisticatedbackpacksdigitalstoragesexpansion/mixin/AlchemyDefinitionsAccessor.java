package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper.AlchemyItemDefinition;

/** Read-only access to Sophisticated's extensible alchemy definitions. */
@Mixin(value = AlchemyUpgradeWrapper.class, remap = false)
public interface AlchemyDefinitionsAccessor {
    @Accessor(value = "itemDefinitions", remap = false)
    static List<AlchemyItemDefinition> sophisticatedBackpackIsAddon$getItemDefinitions() {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
