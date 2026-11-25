package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import java.util.Set;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageAdapterRegistry;

@Mod("digitalstorage_tests")
@PrefixGameTestTemplate(false)
public final class AccessGameTests {
    public AccessGameTests() throws javax.xml.parsers.ParserConfigurationException {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        if (ModList.get().isLoaded("integratedtunnels")) IntegratedResourceFixture.register(bus);
        GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new java.io.File(System.getProperty("digitalstorage.testReport"))));
        bus.addListener(this::register);
    }
    private void register(RegisterGameTestsEvent event) {
        event.register(AccessGameTests.class);
        if (ModList.get().isLoaded("integratedtunnels")) {
            org.cyclops.integrateddynamics.GeneralConfig.energyConsumptionMultiplier = 1;
            event.register(IntegratedAccessGameTests.class);
            event.register(IntegratedResourceGameTests.class);
        }
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests")
    public static void profile(GameTestHelper helper) {
        var expected = Set.of(System.getProperty("digitalstorage.expectedBackends", "").split(","));
        var actual = StorageAdapterRegistry.installedBackendIds();
        helper.assertTrue(expected.containsAll(actual) && (expected.contains("") ? actual.isEmpty() : actual.equals(expected)), "Forge profile mismatch");
        helper.succeed();
    }
}
