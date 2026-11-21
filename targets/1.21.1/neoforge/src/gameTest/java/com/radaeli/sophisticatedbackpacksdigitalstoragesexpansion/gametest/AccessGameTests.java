package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.*;

@Mod("digitalstorage_tests")
@GameTestHolder("digitalstorage_tests")
@PrefixGameTestTemplate(false)
public final class AccessGameTests {
    public AccessGameTests(IEventBus bus) {
        if (ModList.get().isLoaded("integrateddynamics") && ModList.get().isLoaded("integratedtunnels")) IntegratedResourceFixture.register(bus);
        try {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new java.io.File(System.getProperty("digitalstorage.testReport"))));
        } catch (Exception failure) { throw new IllegalStateException("Cannot initialize test reporter", failure); }
        bus.addListener(this::register);
    }
    private void register(RegisterGameTestsEvent event) {
        event.register(AccessGameTests.class);
        if (ModList.get().isLoaded("integrateddynamics") && ModList.get().isLoaded("integratedtunnels")) {
            // Enable native energy before constructing networks so their observer can initialize
            // normally. Changing it only during an assertion leaves the energy index cold.
            org.cyclops.integrateddynamics.GeneralConfig.energyConsumptionMultiplier = 1;
            event.register(IntegratedAccessGameTests.class);
            event.register(IntegratedResourceGameTests.class);
        }
    }
    @GameTest(template = "empty")
    public static void profile(GameTestHelper helper) {
        String profile = System.getProperty("digitalstorage.backendProfile");
        var expected = new HashSet<String>();
        switch (profile) {
            case "all" -> expected.addAll(Set.of("ae2", "refinedstorage", "toms_storage", "scc", "integrateddynamics"));
            case "ae2" -> expected.add("ae2");
            case "rs" -> expected.add("refinedstorage");
            case "toms" -> expected.add("toms_storage");
            case "scc" -> expected.add("scc");
            case "integrated", "terminals", "full" -> expected.add("integrateddynamics");
            case "none", "dynamics" -> {}
            default -> throw new IllegalArgumentException(profile);
        }
        helper.assertTrue(StorageAdapterRegistry.installedBackendIds().equals(expected), "Unexpected installed adapters");
        helper.assertTrue(ModList.get().isLoaded("integrateddynamics") == (expected.contains("integrateddynamics") || profile.equals("dynamics")), "Dynamics profile mismatch");
        helper.assertTrue(ModList.get().isLoaded("integratedtunnels") == expected.contains("integrateddynamics"), "Tunnels profile mismatch");
        helper.assertTrue(ModList.get().isLoaded("integratedterminals") == Set.of("terminals", "full", "all").contains(profile), "Terminals profile mismatch");
        helper.assertTrue(ModList.get().isLoaded("integratedcrafting") == Set.of("full", "all").contains(profile), "Crafting profile mismatch");
        helper.succeed();
    }
}
