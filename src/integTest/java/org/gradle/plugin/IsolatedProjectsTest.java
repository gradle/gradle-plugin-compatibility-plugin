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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for isolated projects compatibility declaration.
 */
@Execution(ExecutionMode.CONCURRENT)
@ParameterizedClass
@MethodSource("allGradleVersions")
class IsolatedProjectsTest extends CompatibilityTestBase {

    IsolatedProjectsTest(String gradleVersion) {
        super(gradleVersion);
    }

    @Test
    @DisplayName("Isolated projects set to true")
    void isolatedProjectsSetToTrue() throws IOException {
        withKotlinBuildScript("""
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                isolatedProjects.set(true)
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

        var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        assertPluginDescriptor("org.gradle.test.plugin")
            .hasImplementationClass("org.gradle.plugin.TestPlugin")
            .hasIsolatedProjects(SUPPORTED);
    }

    @Test
    @DisplayName("Isolated projects set to false")
    void isolatedProjectsSetToFalse() throws IOException {
        withKotlinBuildScript("""
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                isolatedProjects.set(false)
                            }
                        }
                    }
                }
            }
            """);
        createTestPluginSource();

        var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        assertPluginDescriptor("org.gradle.test.plugin")
            .hasImplementationClass("org.gradle.plugin.TestPlugin")
            .hasIsolatedProjects(UNSUPPORTED);
    }
}
