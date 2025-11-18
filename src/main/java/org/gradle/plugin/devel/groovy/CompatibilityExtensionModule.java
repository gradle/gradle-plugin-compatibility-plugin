package org.gradle.plugin.devel.groovy;

import groovy.lang.Closure;
import org.gradle.plugin.devel.CompatibilityRegistry;
import org.gradle.plugin.devel.PluginDeclaration;

public class CompatibilityExtensionModule {
    public static void compatibility(PluginDeclaration declaration, Closure<?> closure) {
        CompatibilityRegistry.store(
                declaration,
                extension -> {
                    Closure<?> code = closure.rehydrate(extension, closure.getOwner(), closure.getThisObject());
                    code.setResolveStrategy(groovy.lang.Closure.DELEGATE_FIRST);
                    code.call(extension);
                }
        );
    }
}
