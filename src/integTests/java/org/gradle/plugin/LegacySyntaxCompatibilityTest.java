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
 * Tests for legacy compatibility(it) syntax on ALL Gradle versions.
 * This verifies backward compatibility - the old syntax should work on both
 * pre-8.14 and post-8.14 Gradle versions.
 */
@ParameterizedClass
@MethodSource("allGradleVersions")
@Execution(ExecutionMode.CONCURRENT)
class LegacySyntaxCompatibilityTest extends CompatibilityTestBase {

    LegacySyntaxCompatibilityTest(String version) {
        super(version);
    }

    @Test
    @DisplayName("Legacy compatibility(it) syntax with both features set")
    void legacySyntaxBothFeaturesSet() throws IOException {
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
    @DisplayName("Legacy syntax with only configuration-cache set")
    void legacySyntaxOnlyConfigurationCache() throws IOException {
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
                }
            }
            """);
        createTestPluginSource();

        var result = runGradle("jar");

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasImplementationClass("org.gradle.plugin.TestPlugin")
                .hasConfigurationCache(SUPPORTED)
                .hasIsolatedProjects(UNKNOWN);
    }

    @Test
    @DisplayName("Legacy syntax with named() override")
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
        // named() block should override create() block
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasImplementationClass("org.gradle.plugin.TestPlugin")
                .hasConfigurationCache(NOT_SUPPORTED)
                .hasIsolatedProjects(SUPPORTED);
    }
}
