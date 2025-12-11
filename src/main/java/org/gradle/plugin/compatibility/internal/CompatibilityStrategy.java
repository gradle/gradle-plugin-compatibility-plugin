package org.gradle.plugin.compatibility.internal;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.plugin.compatibility.CompatibilityExtension;
import org.gradle.plugin.compatibility.CompatibleFeatures;
import org.gradle.plugin.devel.PluginDeclaration;

/**
 * Strategy for configuring and retrieving compatibility information from plugin declarations.
 * Different strategies are used depending on the Gradle version.
 */
public interface CompatibilityStrategy {

    /**
     * Creates or registers a compatibility extension for the given plugin declaration.
     */
    void createExtension(PluginDeclaration declaration, Project project);

    /**
     * Extracts compatibility features from the given plugin declaration.
     */
    CompatibleFeatures extractFeatures(PluginDeclaration declaration, Project project);

    /**
     * Configures the compatibility extension for the given plugin declaration.
     */
    void configure(PluginDeclaration declaration, Project project, Action<CompatibilityExtension> configuration);
}
