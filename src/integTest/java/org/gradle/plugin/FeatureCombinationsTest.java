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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for all combinations of feature flag values.
 * Uses Kotlin buildscripts for consistent syntax across Gradle versions.
 * Includes "undefined" (not set) values.
 */
@Execution(ExecutionMode.CONCURRENT)
@ParameterizedClass
@MethodSource("allGradleVersions")
class FeatureCombinationsTest extends CompatibilityTestBase {

    FeatureCombinationsTest(String gradleVersion) {
        super(gradleVersion);
    }

    @ParameterizedTest
    @CsvSource({
        "true,      DECLARED_SUPPORTED",
        "false,     DECLARED_UNSUPPORTED",
        "undefined, UNDECLARED",
    })
    @DisplayName("All feature combinations including undefined")
    void testAllFeatureCombinations(String ccValue, String expectedCc) throws IOException {
        withSettingsFile();

        String featuresBlock = buildFeaturesBlock(ccValue);

        withKotlinBuildScript("""
            import org.gradle.plugin.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            %s
                        }
                    }
                }
            }
            """.formatted(featuresBlock));
        createTestPluginSource();

        // Use the latest Gradle version from the test classpath
        var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");

        assertPluginDescriptor("org.gradle.test.plugin")
            .hasImplementationClass("org.gradle.plugin.TestPlugin")
            .hasConfigurationCache(expectedCc);
    }

    private String buildFeaturesBlock(String cc) {
        StringBuilder sb = new StringBuilder("features {\n");
        if (!"undefined".equals(cc)) {
            sb.append("                configurationCache.set(").append(cc).append(")\n");
        }
        sb.append("            }");
        return sb.toString();
    }
}
