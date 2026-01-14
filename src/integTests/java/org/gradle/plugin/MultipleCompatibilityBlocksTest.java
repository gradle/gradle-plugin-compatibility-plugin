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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for multiple compatibility blocks on the same plugin.
 * Tests both legacy and modern syntax, in same and different clauses.
 */
@Execution(ExecutionMode.CONCURRENT)
class MultipleCompatibilityBlocksTest{

    @Nested
    @ParameterizedClass
    @MethodSource("allGradleVersions")
    @DisplayName("Legacy syntax tests")
    class LegacySyntaxTests extends CompatibilityTestBase {

        LegacySyntaxTests(String version) {
            super(version);
        }

        @Test
        @DisplayName("Legacy: multiple blocks in same create() - last wins")
        void legacyMultipleBlocksSameCreate() throws IOException {

            withSettingsFile();
        withGroovyBuildScript("""
            gradlePlugin {
                plugins {
                    create('testPlugin') {
                        id = 'org.gradle.test.plugin'
                        implementationClass = 'org.gradle.plugin.TestPlugin'
                        compatibility(it) {
                            features {
                                configurationCache = true
                            }
                        }
                        compatibility(it) {
                            features {
                                configurationCache = false
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

            var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        // Last block wins
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(UNSUPPORTED);
    }

        @Test
        @DisplayName("Legacy: multiple blocks in create() and named() - named wins")
        void legacyMultipleBlocksCreateAndNamed() throws IOException {

            withSettingsFile();
        withGroovyBuildScript("""
            gradlePlugin {
                plugins {
                    create('testPlugin') {
                        id = 'org.gradle.test.plugin'
                        implementationClass = 'org.gradle.plugin.TestPlugin'
                        compatibility(it) {
                            features {
                                configurationCache = true
                            }
                        }
                    }
                    named('testPlugin') {
                        compatibility(it) {
                            features {
                                configurationCache = false
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

            var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        // named() block wins for configurationCache
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(UNSUPPORTED);
        }
    }

    @Nested
    @ParameterizedClass
    @MethodSource("modernSyntaxGradleVersions")
    @DisplayName("Modern syntax tests")
    class ModernSyntaxTests extends CompatibilityTestBase {

        ModernSyntaxTests(String version) {
            super(version);
        }

        @Test
        @DisplayName("Modern: multiple blocks in same create() - last wins")
        void modernMultipleBlocksSameCreate() throws IOException {

            withSettingsFile();
        withGroovyBuildScript("""
            gradlePlugin {
                plugins {
                    create('testPlugin') {
                        id = 'org.gradle.test.plugin'
                        implementationClass = 'org.gradle.plugin.TestPlugin'
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                        compatibility {
                            features {
                                configurationCache = false
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

            var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        // Last block wins
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(UNSUPPORTED);
    }

        @Test
        @DisplayName("Modern: multiple blocks in create() and named() - named wins")
        void modernMultipleBlocksCreateAndNamed() throws IOException {

            withSettingsFile();
        withGroovyBuildScript("""
            gradlePlugin {
                plugins {
                    create('testPlugin') {
                        id = 'org.gradle.test.plugin'
                        implementationClass = 'org.gradle.plugin.TestPlugin'
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                    }
                    named('testPlugin') {
                        compatibility {
                            features {
                                configurationCache = false
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

            var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        // named() block wins for configurationCache, isolatedProjects from create() is preserved
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(UNSUPPORTED);
        }

        @Test
        @DisplayName("Mixed legacy and modern syntax in different clauses")
        void mixedLegacyAndModernSyntax() throws IOException {

            withSettingsFile();
            withGroovyBuildScript("""
                    gradlePlugin {
                        plugins {
                            create('testPlugin') {
                                id = 'org.gradle.test.plugin'
                                implementationClass = 'org.gradle.plugin.TestPlugin'
                                compatibility(it) {
                                    features {
                                        configurationCache = true
                                    }
                                }
                            }
                            named('testPlugin') {
                                compatibility {
                                    features {
                                        configurationCache = false
                                    }
                                }
                            }
                        }
                    }
                    """);
        createTestPluginSource();

            var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        // named() block wins regardless of syntax
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(UNSUPPORTED);
        }
    }
}
