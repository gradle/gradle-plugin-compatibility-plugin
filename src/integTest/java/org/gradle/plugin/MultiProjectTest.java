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

package org.gradle.plugin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for multi-project builds with compatibility plugin.
 * Uses Kotlin buildscripts for consistent syntax across Gradle versions.
 */
@Execution(ExecutionMode.CONCURRENT)
@ParameterizedClass
@MethodSource("allGradleVersions")
class MultiProjectTest extends CompatibilityTestBase {

    MultiProjectTest(String version) {
        super(version);
    }

    @Test
    @DisplayName("Root project with plugin, subproject without")
    void rootProjectWithPlugin_subprojectWithout() throws IOException {
        var settingsFile = testProjectDir.resolve("settings.gradle");
        Files.writeString(settingsFile, """
            rootProject.name = "multi-project"
            include("subproject")
            """);

        withKotlinBuildScript("""
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("rootPlugin") {
                        id = "com.example.root-plugin"
                        implementationClass = "com.example.RootPlugin"
                        compatibility {
                            features {
                                configurationCache.set(true)
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource("com.example", "RootPlugin");

        var subBuildFile = testProjectDir.resolve("subproject/build.gradle.kts");
        Files.createDirectories(subBuildFile.getParent());
        Files.writeString(subBuildFile, """
            // Subproject without plugin
            """);

        var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        assertPluginDescriptor("com.example.root-plugin")
            .hasImplementationClass("com.example.RootPlugin")
            .hasConfigurationCache(SUPPORTED);
    }

    @Test
    @DisplayName("Multiple subprojects with different compatibility settings")
    void multipleSubprojectsWithDifferentCompatibility() throws IOException {
        var settingsFile = testProjectDir.resolve("settings.gradle");
        Files.writeString(settingsFile, """
            rootProject.name = "multi-project"
            include("plugin-a", "plugin-b")
            """);

        withKotlinBuildScript("plugin-a/build.gradle.kts", """
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("pluginA") {
                        id = "com.example.plugin-a"
                        implementationClass = "com.example.PluginA"
                        compatibility {
                            features {
                                configurationCache.set(true)
                            }
                        }
                    }
                }
            }
            """);

        createTestPluginSource("plugin-a", "com.example", "PluginA");

        withKotlinBuildScript("plugin-b/build.gradle.kts", """            
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("pluginB") {
                        id = "com.example.plugin-b"
                        implementationClass = "com.example.PluginB"
                        compatibility {
                            features {
                                configurationCache.set(false)
                            }
                        }
                    }
                }
            }
            """);

        createTestPluginSource("plugin-b", "com.example", "PluginB");

        var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        assertPluginDescriptor("plugin-a", "com.example.plugin-a")
            .hasConfigurationCache(SUPPORTED);

        assertPluginDescriptor("plugin-b", "com.example.plugin-b")
            .hasConfigurationCache(UNSUPPORTED);
    }

    @Test
    @DisplayName("Subproject inherits nothing from root")
    void subprojectDoesNotInheritFromRoot() throws IOException {
        var settingsFile = testProjectDir.resolve("settings.gradle");
        Files.writeString(settingsFile, """
            rootProject.name = "multi-project"
            include("subproject")
            """);

        withKotlinBuildScript("""
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("rootPlugin") {
                        id = "com.example.root-plugin"
                        implementationClass = "com.example.RootPlugin"
                        compatibility {
                            features {
                                configurationCache.set(true)
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource("com.example", "RootPlugin");

        withKotlinBuildScript("subproject/build.gradle.kts", """
            gradlePlugin {
                plugins {
                    create("subPlugin") {
                        id = "com.example.sub-plugin"
                        implementationClass = "com.example.SubPlugin"
                    }
                }
            }
            """);
        createTestPluginSource("subproject", "com.example", "SubPlugin");

        var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        // Root plugin has compatibility set
        assertPluginDescriptor("com.example.root-plugin")
            .hasConfigurationCache(SUPPORTED);

        // Subproject plugin has no compatibility set - should be UNDECLARED
        assertPluginDescriptor("subproject", "com.example.sub-plugin")
            .hasConfigurationCache(UNDECLARED);
    }
}
