package org.gradle.plugin;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for verifying that compatibility descriptor tasks properly handle up-to-date checks
 * when system properties are used to control compatibility features.
 */
@Execution(ExecutionMode.CONCURRENT)
@ParameterizedClass
@MethodSource("allGradleVersions")
class TaskUpToDateTest extends CompatibilityTestBase {

    TaskUpToDateTest(String gradleVersion) {
        super(gradleVersion);
    }

    @BeforeEach
    @Override
    void setUp() throws IOException {
        super.setUp();

        withKotlinBuildScript("""
            import org.gradle.plugin.devel.compatibility.compatibility

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
                                isolatedProjects.set(
                                    providers.systemProperty("enable-ip").map { it.toBoolean() }
                                )
                            }
                        }
                    }
                }
            }
            """);

        createTestPluginSource();
    }

    @Test
    @DisplayName("Task stays up-to-date when system property value does not change")
    void taskStaysUpToDateWithSamePropertyValue() {
        // First run with property
        var firstRun = runGradle("jar", "-Denable-cc=true");

        assertThat(firstRun.getOutput()).contains("BUILD SUCCESSFUL");
        assertThat(firstRun.task(":pluginDescriptors"))
                .isNotNull()
                .satisfies(task -> assertThat(task.getOutcome())
                        .isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE));

        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(SUPPORTED)
                .hasIsolatedProjects(UNDECLARED);

        // Second run with same property value - task should be up to date
        var secondRun = runGradle("jar", "-Denable-cc=true");

        assertThat(secondRun.getOutput()).contains("BUILD SUCCESSFUL");
        assertThat(secondRun.task(":pluginDescriptors"))
                .isNotNull()
                .satisfies(task -> assertThat(task.getOutcome())
                        .as("Task should be UP-TO-DATE when property value hasn't changed")
                        .isEqualTo(TaskOutcome.UP_TO_DATE));

        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(SUPPORTED)
                .hasIsolatedProjects(UNDECLARED);
    }

    @Test
    @DisplayName("Task is invalidated when system property value changes")
    void taskInvalidatedWhenPropertyValueChanges() {
        // First run - property not set
        var firstRun = runGradle("jar");

        assertThat(firstRun.getOutput()).contains("BUILD SUCCESSFUL");
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(UNDECLARED);

        // Second run - the property changed to true
        var secondRun = runGradle("jar", "-Denable-cc=true");

        assertThat(secondRun.getOutput()).contains("BUILD SUCCESSFUL");
        assertThat(secondRun.task(":pluginDescriptors"))
                .isNotNull()
                .satisfies(task -> assertThat(task.getOutcome())
                        .as("Task should be re-executed when property value changes")
                        .isEqualTo(TaskOutcome.SUCCESS));

        // Descriptor should have the updated value
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(SUPPORTED);

        // Third run - the property changed to false
        var thirdRun = runGradle("jar", "-Denable-cc=false");

        assertThat(thirdRun.getOutput()).contains("BUILD SUCCESSFUL");
        assertThat(thirdRun.task(":pluginDescriptors"))
                .isNotNull()
                .satisfies(task -> assertThat(task.getOutcome())
                        .as("Task should be re-executed when property value changes back")
                        .isEqualTo(TaskOutcome.SUCCESS));

        // Descriptor should be updated
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(UNSUPPORTED);

        // Fourth run - the property changed back to undefined
        var fourthRun = runGradle("jar");

        assertThat(thirdRun.getOutput()).contains("BUILD SUCCESSFUL");
        assertThat(thirdRun.task(":pluginDescriptors"))
                .isNotNull()
                .satisfies(task -> assertThat(task.getOutcome())
                        .as("Task should be re-executed when property value changes back")
                        .isEqualTo(TaskOutcome.SUCCESS));

        // Descriptor should be updated
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(UNDECLARED);
    }

    @Test
    @DisplayName("Task stays up-to-date when multiple property values do not change")
    void taskStaysUpToDateWithMultipleProperties() {
        // First run with both properties set
        var firstRun = runGradle("jar", "-Denable-cc=true", "-Denable-ip=true");

        assertThat(firstRun.getOutput()).contains("BUILD SUCCESSFUL");
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(SUPPORTED)
                .hasIsolatedProjects(SUPPORTED);

        // Second run with same property values - task should be up to date
        var secondRun = runGradle("jar", "-Denable-cc=true", "-Denable-ip=true");

        assertThat(secondRun.getOutput()).contains("BUILD SUCCESSFUL");
        assertThat(secondRun.task(":pluginDescriptors"))
                .isNotNull()
                .satisfies(task -> assertThat(task.getOutcome())
                        .as("Task should be UP-TO-DATE when property values haven't changed")
                        .isEqualTo(TaskOutcome.UP_TO_DATE));

        // Descriptors should still have correct values
        assertPluginDescriptor("org.gradle.test.plugin")
                .hasConfigurationCache(SUPPORTED)
                .hasIsolatedProjects(SUPPORTED);
    }
}
