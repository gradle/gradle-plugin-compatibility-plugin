package org.gradle.plugin.compatibility.internal;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;
import org.gradle.plugin.compatibility.internal.groovy.CompatibilityProjectExtension;
import org.gradle.plugin.devel.tasks.GeneratePluginDescriptors;

@SuppressWarnings("unused") // Instantiated by Gradle
public class CompatibilityPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin(
                "java-gradle-plugin",
                plugin -> {
                    configurePluginDescriptorsTask(project);

                    CompatibilityProjectExtension.install(project);

                    CompatibilityStrategy strategy = CompatibilityStrategyFactory.getStrategy();

                    project.getExtensions().configure(GradlePluginDevelopmentExtension.class, gradlePlugins -> {
                        gradlePlugins.getPlugins().configureEach(decl -> {
                            strategy.createExtension(decl, project);
                        });
                    });
                }
        );
    }

    private static void configurePluginDescriptorsTask(Project project) {
        project.getTasks()
                .withType(GeneratePluginDescriptors.class)
                .configureEach(task -> {
                    SerializeCompatibilityDataAction action =
                            project.getObjects().newInstance(SerializeCompatibilityDataAction.class, task);

                    task.getInputs().property(
                            "compatibilityFeatures",
                            action.getSerializableCompatibilityData()
                    );

                    task.doLast("addSupportedFeatureFlags", action);
                });
    }
}
