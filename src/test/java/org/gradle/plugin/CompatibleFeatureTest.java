package org.gradle.plugin;

import org.gradle.plugin.testutils.GradleVersionRegistry;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class CompatibleFeatureTest {

    @TempDir
    Path testProjectDir;

    static Stream<String> gradleVersions() {
        return GradleVersionRegistry.getVersions(GradleVersion.version("8.10")).stream().map(GradleVersion::getVersion);
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    @DisplayName("should generate correct properties file with Groovy DSL")
    void shouldGenerateCorrectPropertiesFileWithGroovyDsl(String gradleVersion) throws IOException {
        var settingsFile = testProjectDir.resolve("settings.gradle");
        var buildFile = testProjectDir.resolve("build.gradle");

        Files.writeString(settingsFile, "rootProject.name = \"plugin-support-flags-plugin-test\"");
        Files.writeString(buildFile, """
            plugins {
                id("java-gradle-plugin")
                id("org.gradle.plugin.devel.compatibility")
            }

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                    }
                    named("testPlugin") {
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

        var result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments("jar", "-s")
                .withPluginClasspath()
                .withGradleVersion(gradleVersion)
                .build();

        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));

        var propertiesFile = testProjectDir.resolve(
                "build/resources/main/META-INF/gradle-plugins/org.gradle.test.plugin.properties"
        );
        assertTrue(propertiesFile.toFile().exists());

        var content = Files.readString(propertiesFile);
        assertTrue(content.contains("implementation-class=org.gradle.plugin.TestPlugin\n"));

        assertTrue(content.contains("compatibility.feature.configuration-cache=SUPPORTED\n"));
        assertTrue(content.contains("compatibility.feature.isolated-projects=UNKNOWN\n"));
    }

    @ParameterizedTest
    @MethodSource("gradleVersions")
    @DisplayName("should generate correct properties file with Kotlin DSL")
    void shouldGenerateCorrectPropertiesFileWithKotlinDsl(String gradleVersion) throws IOException {
        // given: a gradle project with a test plugin using Kotlin DSL
        var settingsFile = testProjectDir.resolve("settings.gradle.kts");
        var buildFile = testProjectDir.resolve("build.gradle.kts");

        Files.writeString(settingsFile, "rootProject.name = \"plugin-support-flags-plugin-test\"");
        Files.writeString(buildFile, """
            import org.gradle.plugin.devel.compatibility

            plugins {
                `java-gradle-plugin`
                id("org.gradle.plugin.devel.compatibility")
            }

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

        var result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments("jar", "-s")
                .withPluginClasspath()
                .withGradleVersion(gradleVersion)
                .build();

        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));

        var propertiesFile = testProjectDir.resolve(
                "build/resources/main/META-INF/gradle-plugins/org.gradle.test.plugin.properties"
        );
        assertTrue(propertiesFile.toFile().exists());

        var content = Files.readString(propertiesFile);
        // Implementation class should not be affected by the plugin
        assertTrue(content.contains("implementation-class=org.gradle.plugin.TestPlugin\n"));

        // Support flag values
        assertTrue(content.contains("compatibility.feature.configuration-cache=SUPPORTED\n"));
        assertTrue(content.contains("compatibility.feature.isolated-projects=UNKNOWN\n"));
    }

    @Test
    @DisplayName("plugin is configuration cache compatible")
    void pluginIsConfigurationCacheCompatible() throws IOException {
        var gradleProperties = testProjectDir.resolve("gradle.properties");
        Files.writeString(gradleProperties, """
            org.gradle.configuration-cache=true
            """);

        var settingsFile = testProjectDir.resolve("settings.gradle");
        Files.writeString(settingsFile, """
            rootProject.name = "plugin-support-flags-plugin-test"
            """);

        var buildFile = testProjectDir.resolve("build.gradle");
        Files.writeString(buildFile, """
            plugins {
                id("java-gradle-plugin")
                id("org.gradle.plugin.devel.compatibility")
            }

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

        System.out.println("==== FIRST RUN ====");
        var firstRun = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments("jar")
                .withPluginClasspath()
                .build();

        System.out.println("==== CLEAN ====");
        var clean = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments("clean")
                .withPluginClasspath()
                .build();

        System.out.println("==== SECOND RUN ====");
        var secondRun = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments("jar", "-s")
                .withPluginClasspath()
                .build();

        assertTrue(firstRun.getOutput().contains("BUILD SUCCESSFUL"));
        assertTrue(secondRun.getOutput().contains("BUILD SUCCESSFUL"));
        assertTrue(secondRun.getOutput().contains("Reusing configuration cache."));

        var propertiesFile = testProjectDir.resolve(
                "build/resources/main/META-INF/gradle-plugins/org.gradle.test.plugin.properties"
        );
        assertTrue(propertiesFile.toFile().exists());

        var content = Files.readString(propertiesFile);
        // Implementation class should not be affected by the plugin
        assertTrue(content.contains("implementation-class=org.gradle.plugin.TestPlugin\n"));

        // Support flag values
        assertTrue(content.contains("compatibility.feature.configuration-cache=SUPPORTED\n"));
        assertTrue(content.contains("compatibility.feature.isolated-projects=UNKNOWN\n"));
    }

    private void createTestPluginSource() throws IOException {
        var sourceDir = testProjectDir.resolve("src/main/java/org/gradle/plugin");
        Files.createDirectories(sourceDir);
        var testPluginFile = sourceDir.resolve("TestPlugin.java");
        Files.writeString(testPluginFile, """
            package org.gradle.plugin;
            import org.gradle.api.Plugin;
            import org.gradle.api.Project;

            public class TestPlugin implements Plugin<Project> {
                @Override
                public void apply(Project project) {
                    // No logic needed for this test
                }
            }
            """);
    }
}
