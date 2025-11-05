package org.gradle.plugin.devel;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.plugin.devel.tasks.GeneratePluginDescriptors;

public class CompatibilityPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin(
                "java-gradle-plugin",
                plugin -> {
                    configurePluginDescriptorsTask(project);
                }
        );
    }

    private static void configurePluginDescriptorsTask(Project project) {
        project.getTasks()
                .withType(GeneratePluginDescriptors.class)
                .configureEach(task -> task.doLast(
                        "addSupportedFeatureFlags",
                        project.getObjects().newInstance(SerializeCompatibilityDataAction.class, task)
                ));
    }
}
