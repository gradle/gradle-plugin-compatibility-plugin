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
 * Tests that modern compatibility syntax fails with appropriate error message on pre-8.14 Gradle.
 */
@ParameterizedClass
@MethodSource("legacySyntaxOnlyGradleVersions")
@Execution(ExecutionMode.CONCURRENT)
class ModernSyntaxErrorTest extends CompatibilityTestBase {

    ModernSyntaxErrorTest(String version) {
        super(version);
    }

    @Test
    @DisplayName("Modern syntax on pre-8.14 fails with clear error message")
    void modernSyntaxOnPre814FailsWithMessage() throws IOException {

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
                }
            }
            """);
        createTestPluginSource();

        var result = runGradleAndFail("jar");

        assertThat(result.getOutput())
            .contains("compatibility { ... } syntax is only supported since Gradle 8.14+")
            .contains("Pass PluginDeclaration explicitly");
    }
}
