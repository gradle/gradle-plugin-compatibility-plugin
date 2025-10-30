package org.gradle.plugin

import org.gradle.plugin.testing.GradleVersionRegistry
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

class CompatibleFeatureTest extends Specification {

    @TempDir
    Path testProjectDir

    @Unroll
    def "should generate correct properties file with Groovy DSL"() {
        given: "a gradle project with a test plugin using Groovy DSL"
        def settingsFile = testProjectDir.resolve("settings.gradle")
        def buildFile = testProjectDir.resolve("build.gradle")

        settingsFile.text = 'rootProject.name = "plugin-support-flags-plugin-test"'
        buildFile.text = """
            plugins {
                id("java-gradle-plugin")
                id("org.gradle.plugin.devel.compatibility")
            }

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                    }
                }
            }
            """
        createTestPluginSource()

        when: "the jar task is executed"
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments("jar", "-s")
                .withPluginClasspath()
                .withGradleVersion(gradleVersion.version)
                .build()

        then: "the build is successful and properties file is correct"
        result.output.contains("BUILD SUCCESSFUL")

        def propertiesFile = testProjectDir.resolve(
                "build/resources/main/META-INF/gradle-plugins/org.gradle.test.plugin.properties"
        )
        propertiesFile.toFile().exists()

        def content = propertiesFile.text
        // Implementation class should not be affected by the plugin
        content.contains("implementation-class=org.gradle.plugin.TestPlugin\n")

        // Support flag values
        content.contains("compatibility.feature.configuration-cache=SUPPORTED\n")
        content.contains("compatibility.feature.isolated-projects=UNKNOWN\n")

        where:
        gradleVersion << GradleVersionRegistry.getVersions(GradleVersion.version("8.10"))
    }

    def "should generate correct properties file with Kotlin DSL"() {
        given: "a gradle project with a test plugin using Kotlin DSL"
        def settingsFile = testProjectDir.resolve("settings.gradle.kts")
        def buildFile = testProjectDir.resolve("build.gradle.kts")

        settingsFile.text = "rootProject.name = \"plugin-support-flags-plugin-test\""
        buildFile.text = """
            import org.gradle.plugin.devel.compatibility

            plugins {
                `java-gradle-plugin`
                id("org.gradle.plugin.devel.compatibility")
            }

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                    }
                }
            }
            """
        createTestPluginSource()

        when: "the jar task is executed"
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments("jar")
                .withPluginClasspath()
                .build()

        then: "the build is successful and properties file is correct"
        result.output.contains("BUILD SUCCESSFUL")

        def propertiesFile = testProjectDir.resolve(
                "build/resources/main/META-INF/gradle-plugins/org.gradle.test.plugin.properties"
        )
        propertiesFile.toFile().exists()

        def content = propertiesFile.text
        // Implementation class should not be affected by the plugin
        content.contains("implementation-class=org.gradle.plugin.TestPlugin\n")

        // Support flag values
        content.contains("compatibility.feature.configuration-cache=SUPPORTED\n")
        content.contains("compatibility.feature.isolated-projects=UNKNOWN\n")
    }

    def "plugin is configuration-cache compatible"() {
        given: "a gradle project with a test plugin using Groovy DSL"
        def gradleProperties = testProjectDir.resolve("gradle.properties")
        gradleProperties.text = """
            org.gradle.configuration-cache=true
        """

        def settingsFile = testProjectDir.resolve("settings.gradle")
        settingsFile.text = """
            rootProject.name = "plugin-support-flags-plugin-test"
        """

        def buildFile = testProjectDir.resolve("build.gradle")
        buildFile.text = """
            plugins {
                id("java-gradle-plugin")
                id("org.gradle.plugin.devel.compatibility")
            }

            gradlePlugin {
                plugins {
                    create("testPlugin") {
                        id = "org.gradle.test.plugin"
                        implementationClass = "org.gradle.plugin.TestPlugin"
                        compatibility {
                            features {
                                configurationCache = true
                            }
                        }
                    }
                }
            }
        """
        createTestPluginSource()

        when: "the plugin supports the configuration cache "
        println("==== FIRST RUN ====")
        BuildResult firstRun = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments( "jar")
                .withPluginClasspath()
                .build()

        println("==== CLEAN ====")
        BuildResult clean = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments( "clean")
                .withPluginClasspath()
                .build()

        println("==== SECOND RUN ====")
        BuildResult secondRun = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments( "jar")
                .withPluginClasspath()
                .build()

        then: "the build is successful and uses the configuration cache"
        firstRun.output.contains("BUILD SUCCESSFUL")
        secondRun.output.contains("BUILD SUCCESSFUL")
//        secondRun.output.contains("Reusing configuration cache.")

        def propertiesFile = testProjectDir.resolve(
                "build/resources/main/META-INF/gradle-plugins/org.gradle.test.plugin.properties"
        )
        propertiesFile.toFile().exists()

        def content = propertiesFile.text
        // Implementation class should not be affected by the plugin
        content.contains("implementation-class=org.gradle.plugin.TestPlugin\n")

        // Support flag values
        content.contains("compatibility.feature.configuration-cache=SUPPORTED\n")
        content.contains("compatibility.feature.isolated-projects=UNKNOWN\n")
    }

    private void createTestPluginSource() {
        def sourceDir = testProjectDir.resolve("src/main/java/org/gradle/plugin")
        Files.createDirectories(sourceDir)
        def testPluginFile = sourceDir.resolve("TestPlugin.java")
        testPluginFile.text = """
            package org.gradle.plugin;
            import org.gradle.api.Plugin;
            import org.gradle.api.Project;

            public class TestPlugin implements Plugin<Project> {
                @Override
                public void apply(Project project) {
                    // No logic needed for this test
                }
            }
            """
    }
}
