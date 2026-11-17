package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest.mixin;

import java.lang.reflect.Method;
import java.util.Set;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforgespi.language.ModFileScanData.AnnotationData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only: 1.21.1 otherwise loads all holders before checking enabled namespaces,
 * including Integrated's RS fixtures when RS is absent. No production behavior changes. */
@Mixin(value = GameTestHooks.class, remap = false)
public class GameTestNamespaceMixin {
    @Inject(method = "addGameTestMethods", at = @At("HEAD"), cancellable = true)
    private static void restrictFixtureNamespace(AnnotationData annotation, Set<Method> methods, CallbackInfo ci) {
        if (!"digitalstorage_tests".equals(annotation.annotationData().get("value"))) ci.cancel();
    }
}
