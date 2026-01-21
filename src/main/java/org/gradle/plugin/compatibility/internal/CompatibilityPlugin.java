/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.plugin.compatibility.internal;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.plugin.compatibility.internal.groovy.CompatibilityProjectExtension;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;
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

                CompatibilityStrategy strategy = CompatibilityStrategy.getInstance();

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
