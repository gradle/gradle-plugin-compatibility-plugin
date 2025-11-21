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
            "true,      true,      SUPPORTED,      SUPPORTED",
            "true,      false,     SUPPORTED,      NOT_SUPPORTED",
            "false,     true,      NOT_SUPPORTED,  SUPPORTED",
            "false,     false,     NOT_SUPPORTED,  NOT_SUPPORTED",
            "true,      undefined, SUPPORTED,      UNKNOWN",
            "false,     undefined, NOT_SUPPORTED,  UNKNOWN",
            "undefined, true,      UNKNOWN,        SUPPORTED",
            "undefined, false,     UNKNOWN,        NOT_SUPPORTED",
            "undefined, undefined, UNKNOWN,        UNKNOWN"
    })
    @DisplayName("All feature combinations including undefined")
    void testAllFeatureCombinations(
            String ccValue,
            String ipValue,
            String expectedCc,
            String expectedIp) throws IOException {

        withSettingsFile();

        String featuresBlock = buildFeaturesBlock(ccValue, ipValue);

        withKotlinBuildScript("""
            import org.gradle.plugin.devel.compatibility.compatibility

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
                .hasConfigurationCache(expectedCc)
                .hasIsolatedProjects(expectedIp);
    }

    private String buildFeaturesBlock(String cc, String ip) {
        StringBuilder sb = new StringBuilder("features {\n");
        if (!"undefined".equals(cc)) {
            sb.append("                configurationCache.set(").append(cc).append(")\n");
        }
        if (!"undefined".equals(ip)) {
            sb.append("                isolatedProjects.set(").append(ip).append(")\n");
        }
        sb.append("            }");
        return sb.toString();
    }
}
