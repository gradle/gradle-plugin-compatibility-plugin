package org.gradle.plugin.compatibility.internal;


import org.gradle.api.Action;
import org.gradle.plugin.compatibility.CompatibilityExtension;
import org.gradle.plugin.devel.PluginDeclaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompatibilityRegistry {
    private static final ConcurrentWeakIdentityHashMap<
            PluginDeclaration,
            List<Action<CompatibilityExtension>>
            > featureConfigurators = new ConcurrentWeakIdentityHashMap<>();

    public static void store(PluginDeclaration declaration, Action<? super CompatibilityExtension> action) {
        featureConfigurators.computeIfAbsent(declaration, d -> new ArrayList<>()).add(action::execute);
    }

    public static List<Action<CompatibilityExtension>> getForDeclaration(PluginDeclaration declaration) {
        return featureConfigurators.getOrDefault(declaration, Collections.emptyList());
    }
}
