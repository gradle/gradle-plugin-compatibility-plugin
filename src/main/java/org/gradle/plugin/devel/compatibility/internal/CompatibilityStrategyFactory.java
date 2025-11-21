package org.gradle.plugin.devel.compatibility.internal;

import org.gradle.util.GradleVersion;

/**
 * Factory for selecting the appropriate compatibility strategy based on the Gradle version.
 */
public class CompatibilityStrategyFactory {
    public static final GradleVersion EXTENSION_AWARE_MIN_VERSION = GradleVersion.version("8.14");

    private static final CompatibilityStrategy EXTENSION_AWARE_STRATEGY = new ExtensionAwareStrategy();
    private static final CompatibilityStrategy REGISTRY_STRATEGY = new RegistryStrategy();

    /**
     * Returns the appropriate strategy for the current Gradle version.
     */
    public static CompatibilityStrategy getStrategy() {
        if (GradleVersion.current().compareTo(EXTENSION_AWARE_MIN_VERSION) >= 0) {
            return EXTENSION_AWARE_STRATEGY;
        } else {
            return REGISTRY_STRATEGY;
        }
    }
}
