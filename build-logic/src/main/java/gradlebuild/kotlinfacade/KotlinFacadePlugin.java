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

package gradlebuild.kotlinfacade;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import java.util.List;

/**
 * Generates the Kotlin DSL facade for the {@code compatibility} extension function and adds it to the main source
 * set, the resources and the sources jar, which {@code check} verifies.
 */
public abstract class KotlinFacadePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin("java", ignored -> {
            TaskProvider<GenerateKotlinFacade> generate = registerGenerateTask(project);

            JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet main = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            main.getJava().srcDir(generate.flatMap(GenerateKotlinFacade::getJavaOutputDirectory));
            main.getResources().srcDir(generate.flatMap(GenerateKotlinFacade::getResourcesOutputDirectory));

            String facadeJavaSource = "**/" + GenerateKotlinFacade.FACADE_CLASS_NAME + ".java";
            java.withSourcesJar();
            TaskProvider<Jar> sourcesJar = project.getTasks().named(main.getSourcesJarTaskName(), Jar.class, jar -> {
                jar.from(generate.flatMap(GenerateKotlinFacade::getKotlinOutputDirectory));
                jar.exclude(facadeJavaSource);
            });

            project.getTasks().withType(Javadoc.class)
                .configureEach(javadoc -> javadoc.exclude(facadeJavaSource));

            TaskProvider<VerifyKotlinFacadeSources> verify = registerVerifyTask(project, generate, sourcesJar);
            project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME, check -> check.dependsOn(verify));
        });
    }

    private static TaskProvider<GenerateKotlinFacade> registerGenerateTask(Project project) {
        DirectoryProperty buildDirectory = project.getLayout().getBuildDirectory();
        return project.getTasks().register("generateKotlinFacade", GenerateKotlinFacade.class, task -> {
            task.setGroup("build");
            task.setDescription("Generates the Kotlin DSL facade for the compatibility extension function");
            task.getModuleName().convention(project.getName());
            // Kotlin 1.8 metadata is what the Kotlin compiler used to emit for this plugin; it is readable by the
            // Kotlin embedded in every supported Gradle version, down to 7.4.2
            task.getMetadataVersion().convention(List.of(1, 8, 0));
            task.getJavaOutputDirectory().convention(buildDirectory.dir("generated/sources/kotlinFacade/java"));
            task.getKotlinOutputDirectory().convention(buildDirectory.dir("generated/sources/kotlinFacade/kotlin"));
            task.getResourcesOutputDirectory().convention(buildDirectory.dir("generated/resources/kotlinFacade"));
        });
    }

    private static TaskProvider<VerifyKotlinFacadeSources> registerVerifyTask(
        Project project,
        TaskProvider<GenerateKotlinFacade> generate,
        TaskProvider<Jar> sourcesJar
    ) {
        return project.getTasks().register("verifyKotlinFacadeSources", VerifyKotlinFacadeSources.class, task -> {
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            task.setDescription("Checks that the sources jar ships the generated Kotlin source of the compatibility extension function");
            task.getSourcesJar().convention(sourcesJar.flatMap(Jar::getArchiveFile));
            task.getKotlinSources().convention(generate.flatMap(GenerateKotlinFacade::getKotlinOutputDirectory));
            task.getReportFile().convention(project.getLayout().getBuildDirectory().file("reports/kotlinFacade/verified-sources.txt"));
        });
    }
}
