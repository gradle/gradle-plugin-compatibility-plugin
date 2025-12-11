package org.gradle.plugin.devel.compatibility.internal;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.plugin.devel.compatibility.CompatibilityExtension;
import org.gradle.plugin.devel.compatibility.CompatibleFeatures;
import org.gradle.plugin.devel.PluginDeclaration;

/**
 * Strategy for Gradle >= 8.14 where PluginDeclaration implements ExtensionAware.
 * Uses the ExtensionAware API to directly attach and configure compatibility extensions.
 */
class ExtensionAwareStrategy implements CompatibilityStrategy {

    @Override
    public void createExtension(PluginDeclaration declaration, Project project) {
        if (!(declaration instanceof ExtensionAware)) {
            throw new IllegalStateException("ExtensionAwareStrategy requires PluginDeclaration to implement ExtensionAware");
        }
        ExtensionAware extensionAware = (ExtensionAware) declaration;
        extensionAware.getExtensions().create("compatibility", CompatibilityExtension.class, project.getObjects());
    }

    @Override
    public CompatibleFeatures extractFeatures(PluginDeclaration declaration, Project project) {
        if (!(declaration instanceof ExtensionAware)) {
            throw new IllegalStateException("ExtensionAwareStrategy requires PluginDeclaration to implement ExtensionAware");
        }
        ExtensionAware extensionAware = (ExtensionAware) declaration;
        return extensionAware.getExtensions().getByType(CompatibilityExtension.class).getFeatures();
    }

    @Override
    public void configure(PluginDeclaration declaration, Project project, Action<CompatibilityExtension> configuration) {
        if (!(declaration instanceof ExtensionAware)) {
            throw new IllegalStateException("ExtensionAwareStrategy requires PluginDeclaration to implement ExtensionAware");
        }
        ExtensionAware extensionAware = (ExtensionAware) declaration;
        CompatibilityExtension extension = extensionAware.getExtensions().getByType(CompatibilityExtension.class);
        configuration.execute(extension);
    }
}
