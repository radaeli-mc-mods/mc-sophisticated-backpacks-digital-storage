package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import com.refinedmods.refinedstorage.api.network.impl.node.security.SecurityDecisionProviderProxyNetworkNode;
import com.refinedmods.refinedstorage.api.network.impl.security.SecurityDecisionProviderImpl;
import com.refinedmods.refinedstorage.api.network.security.SecurityPolicy;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.security.SecurityManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

final class RsSecurityFixture {
    static void allowAll(GameTestHelper helper, BlockPos endpoint) {
        if (helper.getLevel().getBlockEntity(endpoint.above()) instanceof SecurityManagerBlockEntity manager) {
            for (var container : manager.getContainerProvider().getContainers()) {
                ((SecurityDecisionProviderProxyNetworkNode) container.getNode()).setDelegate(
                        new SecurityDecisionProviderImpl().setDefaultPolicy(SecurityPolicy.of(BuiltinPermission.values())));
            }
        }
    }
    static SecurityFixture create(GameTestHelper helper, BlockPos endpoint) {
        var manager = (SecurityManagerBlockEntity) helper.getLevel().getBlockEntity(endpoint.above());
        var node = (SecurityDecisionProviderProxyNetworkNode) manager.getContainerProvider().getContainers().iterator().next().getNode();
        var provider = new SecurityDecisionProviderImpl();
        node.setDelegate(provider);
        return new SecurityFixture(insertion -> provider.setDefaultPolicy(SecurityPolicy.of(BuiltinPermission.OPEN,
                insertion ? BuiltinPermission.INSERT : BuiltinPermission.EXTRACT)), () -> {});
    }
}
