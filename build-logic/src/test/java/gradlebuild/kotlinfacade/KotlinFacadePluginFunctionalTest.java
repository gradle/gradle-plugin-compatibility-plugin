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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

class KotlinFacadePluginFunctionalTest {

    @TempDir
    Path projectDir;

    @Test
    void generatesFacadeAndWiresItIntoTheBuild() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
            rootProject.name = "compatibility-plugin"
            """);
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
            plugins {
                java
                id("gradlebuild.kotlin-facade")
            }
            """);

        BuildResult result = runner("verifyKotlinFacadeSources").build();
        assertThat(result.task(":verifyKotlinFacadeSources").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

        // A dry run, because compiling the facade needs the Gradle API and the compatibility plugin classes
        BuildResult checkPlan = runner("check", "--dry-run").build();
        assertThat(checkPlan.getOutput()).contains(":verifyKotlinFacadeSources SKIPPED");

        Path facadeSource = projectDir.resolve(
            "build/generated/sources/kotlinFacade/java/org/gradle/plugin/compatibility/SupportedFeaturesExtensionsKt.java"
        );
        assertThat(facadeSource).content()
            .contains("@kotlin.Metadata(")
            .contains("mv = {1, 8, 0}")
            .contains("public static void compatibility(PluginDeclaration declaration, Action<? super CompatibilityExtension> action)");

        Path kotlinSource = projectDir.resolve(
            "build/generated/sources/kotlinFacade/kotlin/org/gradle/plugin/compatibility/SupportedFeaturesExtensions.kt"
        );
        assertThat(kotlinSource).content()
            .contains("package org.gradle.plugin.compatibility")
            .contains("fun PluginDeclaration.compatibility(action: Action<in CompatibilityExtension>)");

        Path moduleIndex = projectDir.resolve(
            "build/generated/resources/kotlinFacade/META-INF/compatibility-plugin.kotlin_module"
        );
        assertThat(moduleIndex).isNotEmptyFile();

        try (var sourcesJar = new ZipFile(projectDir.resolve("build/libs/compatibility-plugin-sources.jar").toFile())) {
            assertThat(sourcesJar.getEntry("org/gradle/plugin/compatibility/SupportedFeaturesExtensions.kt")).isNotNull();
            assertThat(sourcesJar.getEntry("org/gradle/plugin/compatibility/SupportedFeaturesExtensionsKt.java")).isNull();
        }
    }

    private GradleRunner runner(String... tasks) {
        List<String> arguments = new ArrayList<>(List.of(tasks));
        arguments.addAll(List.of("--configuration-cache", "--stacktrace"));
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(arguments)
            .forwardOutput();
    }
}
