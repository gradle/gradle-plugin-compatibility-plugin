package org.gradle.plugin.compatibility;

import org.gradle.api.Incubating;

/**
 * Various definitions for the plugin descriptor fields. These can be used by other plugins to integrate with defined
 * features.
 */
@Incubating
public interface CompatibilityDeclarationProtocol {
    /**
     * The feature is declared as supported. The actual support status may differ due to bugs.
     */
    String DECLARED_SUPPORTED = "DECLARED_SUPPORTED";

    /**
     * The feature is declared as unsupported.
     */
    String DECLARED_UNSUPPORTED = "DECLARED_UNSUPPORTED";

    /**
     * The status of the feature isn't declared at all.
     */
    String UNDECLARED = "UNDECLARED";

    /**
     * The prefix for the properties defining the compatibility with Gradle features. Each feature has its own property.
     */
    String SUPPORT_FLAGS_PREFIX = "compatibility.feature.";

    /**
     * The Configuration Cache feature name.
     */
    String FEATURE_CONFIGURATION_CACHE = "configuration-cache";

    /**
     * The Isolated Projects feature name.
     */
    String FEATURE_ISOLATED_PROJECTS = "isolated-projects";
}
