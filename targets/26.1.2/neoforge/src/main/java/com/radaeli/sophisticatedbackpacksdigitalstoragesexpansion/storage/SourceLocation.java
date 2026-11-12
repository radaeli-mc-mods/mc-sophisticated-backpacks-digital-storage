package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public record SourceLocation(ResourceKey<Level> dimension, Vec3 position) {
    public static SourceLocation carriedBy(Player player) {
        return new SourceLocation(player.level().dimension(), player.position());
    }

    public static SourceLocation fromMenu(StorageContainerMenuBase<?> menu, Player player) {
        return menu.getBlockPosition()
                .map(pos -> new SourceLocation(player.level().dimension(), Vec3.atCenterOf(pos)))
                .orElseGet(() -> menu.getEntity()
                        .map(entity -> new SourceLocation(entity.level().dimension(), entity.position()))
                        .orElseGet(() -> carriedBy(player)));
    }
}
