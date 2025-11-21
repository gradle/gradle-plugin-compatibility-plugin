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
