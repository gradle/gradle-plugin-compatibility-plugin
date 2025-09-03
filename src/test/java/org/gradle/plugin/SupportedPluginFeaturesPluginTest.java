package org.gradle.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class SupportedPluginFeaturesPluginTest {

    @TempDir
    Path testProjectDir;

    @Test
    public void testWithGroovyScript() throws IOException {
        Path settingsFile = testProjectDir.resolve("settings.gradle");
        Path buildFile = testProjectDir.resolve("build.gradle");

        Files.writeString(
            settingsFile,
            "rootProject.name = \"plugin-support-flags-plugin-test\""
        );
        Files.writeString(
            buildFile,
                """
                import org.gradle.plugin.SupportedFeatureState
                
                plugins {
                    id("java-gradle-plugin")
                    id("org.gradle.supported-plugin-features")
                }
                
                gradlePlugin {
                    plugins {
                        create("testPlugin") {
                            id = "org.gradle.test.plugin"
                            implementationClass = "org.gradle.plugin.TestPlugin"
                            supportedFeatures {
                                configurationCache = SupportedFeatureState.SUPPORTED
                            }
                        }
                    }
                }
                """
        );
        var sourceDir = testProjectDir.resolve("src/main/java/org/gradle/plugin");
        Files.createDirectories(sourceDir);
        Files.writeString(
                sourceDir.resolve("TestPlugin.java"),
                """
                package org.gradle.plugin;
                import org.gradle.api.Plugin;
                import org.gradle.api.Project;
                
                public class TestPlugin implements Plugin<Project> {
                    @Override
                    public void apply(Project project) {
                        // Plugin logic here
                    }
                }
                """
        );

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments("jar")
                .withPluginClasspath()
                .withDebug(true)
                .build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        Path propertiesFile = testProjectDir.resolve(
                "build/resources/main/META-INF/gradle-plugins/org.gradle.test.plugin.properties"
        );
        assertThat(propertiesFile).exists();
        String content = Files.readString(propertiesFile);
        assertThat(content).contains("feature-support.configuration-cache=supported");
        assertThat(content).contains("feature-support.isolated-projects=unknown");
    }

    @Test
    public void testWithKotlinScript() throws IOException {
        Path settingsFile = testProjectDir.resolve("settings.gradle.kts");
        Path buildFile = testProjectDir.resolve("build.gradle.kts");

        Files.writeString(
                settingsFile,
                "rootProject.name = \"plugin-support-flags-plugin-test\""
        );
        Files.writeString(
                buildFile,
                """
                import org.gradle.plugin.supportedFeatures
                import org.gradle.plugin.SupportedFeatureState
                
                plugins {
                    `java-gradle-plugin`
                    id("org.gradle.supported-plugin-features")
                }
                
                gradlePlugin {
                    plugins {
                        create("testPlugin") {
                            id = "org.gradle.test.plugin"
                            implementationClass = "org.gradle.plugin.TestPlugin"
                            supportedFeatures {
                                configurationCache = SupportedFeatureState.SUPPORTED
                                isolatedProjects = SupportedFeatureState.UNKNOWN
                            }
                        }
                    }
                }
                """
        );
        var sourceDir = testProjectDir.resolve("src/main/java/org/gradle/plugin");
        Files.createDirectories(sourceDir);
        Files.writeString(
                sourceDir.resolve("TestPlugin.java"),
                """
                package org.gradle.plugin;
                
                import org.gradle.api.Plugin;
                import org.gradle.api.Project;
                
                public class TestPlugin implements Plugin<Project> {
                    @Override
                    public void apply(Project project) {
                        // Plugin logic here
                    }
                }
                """
        );

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments("jar")
                .withPluginClasspath()
                .build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        Path propertiesFile = testProjectDir.resolve(
                "build/resources/main/META-INF/gradle-plugins/org.gradle.test.plugin.properties"
        );
        assertThat(propertiesFile).exists();
        String content = Files.readString(propertiesFile);
        assertThat(content).contains("feature-support.configuration-cache=supported");
        assertThat(content).contains("feature-support.isolated-projects=unknown");
    }

}
