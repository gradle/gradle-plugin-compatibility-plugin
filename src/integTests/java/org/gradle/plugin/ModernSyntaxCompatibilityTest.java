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
 * Tests for modern compatibility syntax on Gradle 8.14+.
 * This uses the clean syntax without passing 'it' parameter.
 */
@ParameterizedClass
@MethodSource("modernSyntaxGradleVersions")
@Execution(ExecutionMode.CONCURRENT)
class ModernSyntaxCompatibilityTest extends CompatibilityTestBase {

    ModernSyntaxCompatibilityTest(String version) {
        super(version);
    }

    @Test
    @DisplayName("Modern compatibility syntax (Groovy DSL)")
    void modernSyntaxGroovyDsl() throws IOException {
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
                                isolatedProjects = false
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
                .hasConfigurationCache(SUPPORTED)
                .hasIsolatedProjects(NOT_SUPPORTED);
    }

    @Test
    @DisplayName("Modern syntax (Kotlin DSL)")
    void modernSyntaxKotlinDsl() throws IOException {
        withSettingsFile();
        withKotlinBuildScript("""
            import org.gradle.plugin.devel.compatibility.compatibility

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                configurationCache = true
                                isolatedProjects = false
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
                .hasConfigurationCache(SUPPORTED)
                .hasIsolatedProjects(NOT_SUPPORTED);
    }

    @Test
    @DisplayName("Modern syntax with only isolated-projects set")
    void modernSyntaxOnlyIsolatedProjects() throws IOException {
        withSettingsFile();
        withGroovyBuildScript("""
            gradlePlugin {
                plugins {
                    create('testPlugin') {
                        id = 'org.gradle.test.plugin'
                        implementationClass = 'org.gradle.plugin.TestPlugin'
                        compatibility {
                            features {
                                isolatedProjects = true
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
                .hasConfigurationCache(UNKNOWN)
                .hasIsolatedProjects(SUPPORTED);
    }
}
