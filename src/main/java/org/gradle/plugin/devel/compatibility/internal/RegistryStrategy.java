package org.gradle.plugin.devel.compatibility.internal;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.plugin.devel.compatibility.CompatibilityExtension;
import org.gradle.plugin.devel.compatibility.CompatibleFeatures;
import org.gradle.plugin.devel.PluginDeclaration;

/**
 * Strategy for Gradle < 8.14 where PluginDeclaration does not implement ExtensionAware.
 * Uses a registry-based approach to store and retrieve compatibility configuration.
 */
public class RegistryStrategy implements CompatibilityStrategy {

    @Override
    public void createExtension(PluginDeclaration declaration, Project project) {
        // For the registry strategy, we don't create extensions upfront
        // They're registered on-demand when configure() is called
    }

    @Override
    public CompatibleFeatures extractFeatures(PluginDeclaration declaration, Project project) {
        CompatibilityExtension extension = project.getObjects().newInstance(CompatibilityExtension.class);

        CompatibilityRegistry
                .getForDeclaration(declaration)
                .forEach(action -> action.execute(extension));

        return extension.getFeatures();
    }

    @Override
    public void configure(PluginDeclaration declaration, Project project, Action<CompatibilityExtension> configuration) {
        CompatibilityRegistry.store(declaration, configuration);
    }
}
