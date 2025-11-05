package org.gradle.plugin.devel;


import org.gradle.api.Action;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CompatibilityRegistry {
    private static final Map<PluginDeclaration, List<Action<? super CompatibilityExtension>>> featureConfigurators = Collections.synchronizedMap(new WeakHashMap<>());

    public static void store(@NotNull PluginDeclaration declaration, @NotNull Action<@NotNull CompatibilityExtension> action) {
        featureConfigurators.computeIfAbsent(declaration, d -> new ArrayList<>()).add(action);
    }

    public static List<Action<? super CompatibilityExtension>> getForDeclaration(@NotNull PluginDeclaration declaration) {
        return featureConfigurators.getOrDefault(declaration, List.of());
    }
}
