package org.gradle.plugin.devel.compatibility.internal;


import org.gradle.api.Action;
import org.gradle.plugin.devel.compatibility.CompatibilityExtension;
import org.gradle.plugin.devel.PluginDeclaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompatibilityRegistry {
    private static final ConcurrentWeakIdentityHashMap<
            PluginDeclaration,
            List<Action<CompatibilityExtension>>
            > featureConfigurators = new ConcurrentWeakIdentityHashMap<>();

    public static void store( PluginDeclaration declaration, Action<CompatibilityExtension> action) {
        featureConfigurators.computeIfAbsent(declaration, d -> new ArrayList<>()).add(action);
    }

    public static List<Action<CompatibilityExtension>> getForDeclaration(PluginDeclaration declaration) {
        return featureConfigurators.getOrDefault(declaration, Collections.emptyList());
    }
}
