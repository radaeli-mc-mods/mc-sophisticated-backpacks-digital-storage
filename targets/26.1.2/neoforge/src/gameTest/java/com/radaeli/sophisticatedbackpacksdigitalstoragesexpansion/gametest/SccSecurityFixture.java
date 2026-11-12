package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import de.scc.calculator.SccServerConfig;
import de.scc.calculator.network.CalculatorControllerBlockEntity;
import de.scc.calculator.network.SccSecurity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

final class SccSecurityFixture {
    static void checkCompression(GameTestHelper helper, BlockPos endpoint, ServerPlayer player,
            com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTarget target) {
        var controller = (CalculatorControllerBlockEntity) helper.getLevel().getBlockEntity(endpoint.east());
        controller.applyCompress(Integer.toString(de.scc.calculator.network.AutoCompress.MAX));
        var block = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BLOCK);
        var ingot = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
        helper.assertTrue(target.insert(block, player).isEmpty(), "Compressed seed refused");
        var view = de.scc.calculator.api.SccNetwork.at(helper.getLevel(), endpoint);
        helper.assertTrue(target.simulateExtract(ingot, 4, player) == 4, "Native decompression estimate failed");
        helper.assertTrue(view.rawCount(de.scc.calculator.calc.StackKey.of(block)) == 1 && view.rawCount(de.scc.calculator.calc.StackKey.of(ingot)) == 0,
                "Decompression simulation mutated the native stores");
        helper.assertTrue(target.extract(ingot, 4, player).getCount() == 4, "Native decompression extraction failed");
        helper.assertTrue(view.contents().getOrDefault(de.scc.calculator.calc.StackKey.of(ingot), 0L) == 5
                && view.contents().getOrDefault(de.scc.calculator.calc.StackKey.of(block), 0L) == 0, "Nine-unit decompression lost its surplus");
        var packed = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);
        var unit = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE);
        helper.assertTrue(target.insert(packed, player).isEmpty(), "Four-unit compressed seed refused");
        helper.assertTrue(target.simulateExtract(unit, 3, player) == 3 && target.extract(unit, 3, player).getCount() == 3,
                "Four-unit decompression failed");
        helper.assertTrue(view.contents().getOrDefault(de.scc.calculator.calc.StackKey.of(unit), 0L) == 1
                && view.contents().getOrDefault(de.scc.calculator.calc.StackKey.of(packed), 0L) == 0, "Four-unit decompression lost its surplus");
        controller.applyCompress(Integer.toString(de.scc.calculator.network.AutoCompress.OFF));
    }

    static SecurityFixture create(GameTestHelper helper, BlockPos endpoint, ServerPlayer player) {
        var config = SccServerConfig.get();
        boolean security = config.security;
        config.security = true;
        var controller = (CalculatorControllerBlockEntity) helper.getLevel().getBlockEntity(endpoint.east());
        controller.setOwner(java.util.UUID.randomUUID(), "network-owner");
        return new SecurityFixture(insertion -> controller.trust(player.getUUID(), "storage-test",
                insertion ? SccSecurity.Right.PUT.bit() : SccSecurity.Right.TAKE.bit()), () -> config.security = security);
    }
}
