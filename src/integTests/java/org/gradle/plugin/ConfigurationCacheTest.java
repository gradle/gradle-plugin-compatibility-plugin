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

import org.assertj.core.api.Assumptions;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for configuration cache compatibility of the plugin itself.
 * Uses Kotlin buildscripts for consistent syntax across Gradle versions.
 */
@Execution(ExecutionMode.CONCURRENT)
@ParameterizedClass
@MethodSource("allGradleVersions")
class ConfigurationCacheTest extends CompatibilityTestBase {

    ConfigurationCacheTest(String gradleVersion) {
        super(gradleVersion);
    }

    @Override
    protected List<String> buildArguments(List<String> argumentsBuffer, String... userArgs) {
        argumentsBuffer.add("--configuration-cache");
        return super.buildArguments(argumentsBuffer, userArgs);
    }

    @Test
    @DisplayName("Configuration cache reused across builds")
    void configurationCacheReusedAcrossBuilds() throws IOException {
        withKotlinBuildScript("""
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                configurationCache.set(true)
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

        // First run - stores cache
        var firstRun = runGradle("jar");

        assertThat(firstRun.getOutput()).contains("BUILD SUCCESSFUL");

        // Clean
        runGradle("clean");

        // Second run - reuses cache
        var secondRun = runGradle("jar");

        assertThat(secondRun.getOutput())
            .contains("BUILD SUCCESSFUL")
            .contains("Reusing configuration cache.");

        assertPluginDescriptor("org.gradle.test.plugin")
            .hasImplementationClass("org.gradle.plugin.TestPlugin")
            .hasConfigurationCache(SUPPORTED);
    }

    @Test
    @DisplayName("Changing compatibility does not invalidate cache")
    void changingCompatibilityDoesNotInvalidateCache() throws IOException {
        // Gradle versions before 8.7 evaluated the system property eagerly
        Assumptions.assumeThat(getGradleVersion()).isGreaterThanOrEqualTo(GradleVersion.version("8.7"));

        withKotlinBuildScript("""
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                configurationCache.set(
                                    providers.systemProperty("enable-cc").map { it.toBoolean() }
                                )
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

        // First run
        runGradle("jar");

        assertPluginDescriptor("org.gradle.test.plugin")
            .hasConfigurationCache(UNDECLARED);

        // Second run - the cache should not be invalidated
        var secondRun = runGradle("jar", "-Denable-cc=true");

        assertThat(secondRun.getOutput())
            .contains("BUILD SUCCESSFUL")
            .contains("Reusing configuration cache.");  // Cache was not invalidated

        assertPluginDescriptor("org.gradle.test.plugin")
            .hasConfigurationCache(SUPPORTED);

        // Second run - the cache should not be invalidated
        var thirdRun = runGradle("jar", "-Denable-cc=false");

        assertThat(thirdRun.getOutput())
            .contains("BUILD SUCCESSFUL")
            .contains("Reusing configuration cache.");  // Cache was not invalidated

        assertPluginDescriptor("org.gradle.test.plugin")
            .hasConfigurationCache(UNSUPPORTED);
    }

    @Test
    @DisplayName("Multiple plugins with configuration cache")
    void multiplePluginsWithConfigurationCache() throws IOException {
        withKotlinBuildScript("""
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("plugin1") {
                        id = "com.example.plugin1"
                        implementationClass = "com.example.Plugin1"
                        compatibility {
                            features {
                                configurationCache.set(true)
                            }
                        }
                    }
                    create("plugin2") {
                        id = "com.example.plugin2"
                        implementationClass = "com.example.Plugin2"
                        compatibility {
                            features {
                                configurationCache.set(false)
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource("com.example", "Plugin1");
        createTestPluginSource("com.example", "Plugin2");

        // First run
        var firstRun = runGradle("jar");

        assertThat(firstRun.getOutput()).contains("BUILD SUCCESSFUL");

        // Clean and second run
        runGradle("clean");

        var secondRun = runGradle("jar");

        assertThat(secondRun.getOutput())
            .contains("BUILD SUCCESSFUL")
            .contains("Reusing configuration cache.");

        assertPluginDescriptor("com.example.plugin1")
            .hasConfigurationCache(SUPPORTED);

        assertPluginDescriptor("com.example.plugin2")
            .hasConfigurationCache(UNSUPPORTED);
    }
}
