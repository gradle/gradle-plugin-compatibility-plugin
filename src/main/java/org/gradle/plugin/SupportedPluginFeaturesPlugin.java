package org.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;
import org.gradle.plugin.devel.tasks.GeneratePluginDescriptors;

public class SupportedPluginFeaturesPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin(
                "java-gradle-plugin",
                plugin -> {
                    configurePluginExtension(project);
                    configurePluginDescriptorsTask(project);
                }
        );
    }

    private static void configurePluginExtension(Project project) {
        GradlePluginDevelopmentExtension extension = project
                .getExtensions()
                .getByType(GradlePluginDevelopmentExtension.class);

        extension.getPlugins().configureEach(
                pluginDeclaration -> {
                    ExtensionAware extensionAware = (ExtensionAware) pluginDeclaration;
                    extensionAware.getExtensions().create("supportedFeatures", SupportedFeatures.class);
                }
        );
    }

    private static void configurePluginDescriptorsTask(Project project) {
        project.getTasks()
                .withType(GeneratePluginDescriptors.class)
                .configureEach(
                        task ->
                                task.doLast("addSupportedFeatureFlags", new AddSupportedFlagsAction())
                );
    }
}
