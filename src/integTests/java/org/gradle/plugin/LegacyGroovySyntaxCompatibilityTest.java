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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for legacy compatibility(it) syntax on ALL Gradle versions.
 * This verifies backward compatibility - the old syntax should work on both
 * pre-8.14 and post-8.14 Gradle versions.
 */
@ParameterizedClass
@MethodSource("allGradleVersions")
@Execution(ExecutionMode.CONCURRENT)
class LegacyGroovySyntaxCompatibilityTest extends CompatibilityTestBase {

    LegacyGroovySyntaxCompatibilityTest(String version) {
        super(version);
    }

    @ParameterizedTest
    @DisplayName("can apply features with legacy syntax")
    @CsvSource({
        "configurationCache = true, " + SUPPORTED,
        "configurationCache = false, " + UNSUPPORTED,
        "''," + UNDECLARED, // configuration omitted
        "configurationCache = project.provider { null as Boolean }, " + UNDECLARED, // unset provider
        "configurationCache = project.provider { true }, " + SUPPORTED,
        "configurationCache = project.provider { false }, " + UNSUPPORTED,
    })
    void legacySyntaxOnlyConfigurationCache(String configurationLine, String expectedStatus) throws IOException {
        withSettingsFile();
        withGroovyBuildScript("""
            gradlePlugin {
                plugins {
                    create('testPlugin') {
                        id = 'org.gradle.test.plugin'
                        implementationClass = 'org.gradle.plugin.TestPlugin'
                        compatibility(it) {
                            features {
                                %s
                            }
                        }
                    }
                }
            }
            """.formatted(configurationLine));
        createTestPluginSource();

        var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasImplementationClass("org.gradle.plugin.TestPlugin")
                .hasConfigurationCache(expectedStatus);
    }

    @Test
    @DisplayName("can have compatibility blocks in multiple element configuration blocks")
    void legacySyntaxNamedOverride() throws IOException {
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
        // named() block should override create() block
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasImplementationClass("org.gradle.plugin.TestPlugin")
                .hasConfigurationCache(UNSUPPORTED);
    }
}
