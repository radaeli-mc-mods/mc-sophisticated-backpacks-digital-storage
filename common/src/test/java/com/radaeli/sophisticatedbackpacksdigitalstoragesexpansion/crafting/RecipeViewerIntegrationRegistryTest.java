package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeViewerIntegrationRegistryTest {
    @AfterEach
    void clearRegistry() {
        RecipeViewerIntegrationRegistry.clearForTests();
    }

    @Test
    void availabilityRequiresAnAdapterThatSelectsItsHandlerForTheCurrentMenu() {
        Object supportedMenu = new Object();
        RecipeViewerIntegrationRegistry.register(new RecipeViewerAdapter() {
            @Override
            public String id() {
                return "test";
            }

            @Override
            public boolean isReadyFor(Object menu) {
                return menu == supportedMenu;
            }
        });

        assertTrue(RecipeViewerIntegrationRegistry.isReadyFor(supportedMenu));
        assertFalse(RecipeViewerIntegrationRegistry.isReadyFor(new Object()));
        assertFalse(RecipeViewerIntegrationRegistry.isReadyFor(null));
    }

    @Test
    void removingOrInvalidatingTheRuntimeMakesTheAdapterUnavailable() {
        boolean[] runtimeAvailable = {true};
        RecipeViewerAdapter adapter = new RecipeViewerAdapter() {
            @Override
            public String id() {
                return "test";
            }

            @Override
            public boolean isReadyFor(Object menu) {
                return runtimeAvailable[0];
            }
        };
        RecipeViewerIntegrationRegistry.register(adapter);
        assertTrue(RecipeViewerIntegrationRegistry.isReadyFor(new Object()));

        runtimeAvailable[0] = false;
        assertFalse(RecipeViewerIntegrationRegistry.isReadyFor(new Object()));
        RecipeViewerIntegrationRegistry.unregister("test");
        runtimeAvailable[0] = true;
        assertFalse(RecipeViewerIntegrationRegistry.isReadyFor(new Object()));
    }
}
