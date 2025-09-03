package org.gradle.plugin;

public enum SupportedFeatureState {
    /**
     * This is the default state and indicates that the plugin has not declared support for this feature.
     */
    UNKNOWN,
    /**
     * The feature is declared by the plugin as supported.
     */
    SUPPORTED,
    /**
     * The feature is declared by the plugin as not supported.
     */
    NOT_SUPPORTED,
}
