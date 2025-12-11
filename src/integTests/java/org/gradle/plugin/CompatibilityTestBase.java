package org.gradle.plugin;

import org.assertj.core.api.AbstractAssert;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.plugin.devel.compatibility.internal.CompatibilityStrategyFactory.EXTENSION_AWARE_MIN_VERSION;

/**
 * Base class for compatibility plugin integration tests.
 * Provides common utilities and test helpers.
 */
public abstract class CompatibilityTestBase {
    protected static final String SUPPORTED = "SUPPORTED";
    protected static final String NOT_SUPPORTED = "NOT_SUPPORTED";
    protected static final String UNKNOWN = "UNKNOWN";

    @TempDir
    protected Path testProjectDir;

    /**
     * Gradle version to use for test execution. Set by parameterized tests.
     */
    private final String gradleVersion;

    protected CompatibilityTestBase(String gradleVersion) {
        this.gradleVersion = gradleVersion;
    }

    public GradleVersion getGradleVersion() {
        return GradleVersion.version(gradleVersion);
    }

    private static Stream<GradleVersion> testedGradleVersions() {
        // TODO(https://github.com/gradle/gradle-plugin-compatibility-plugin/issues/3) Figure out the strategy for
        //  testing latest Gradle releases here (automatically?)
        Stream<GradleVersion> versions = Stream.of(
                "7.4.2", // Oldest supported
                "7.6.6", // Latest 7.x
                "8.0.2", // First 8.x release
                "8.14.3", // Last 8.x release
                "9.0.0", // First 9.x release
                "9.2.1" // Last 9.x release
        ).map(GradleVersion::version);

        // Skip Gradle 9+ when running with Java 8
        if (System.getProperty("java8Home") != null) {
            versions = versions.filter(v -> v.getMajorVersion() < 9);
        }

        return versions;
    }

    @SuppressWarnings("unused") // Used in @MethodSource
    protected static Stream<String> allGradleVersions() {
        return testedGradleVersions().map(GradleVersion::getVersion);
    }

    @SuppressWarnings("unused") // Used in @MethodSource
    protected static Stream<String> legacySyntaxOnlyGradleVersions() {
        return testedGradleVersions()
                .filter(v -> v.compareTo(EXTENSION_AWARE_MIN_VERSION) < 0)
                .map(GradleVersion::getVersion);
    }

    @SuppressWarnings("unused") // Used in @MethodSource
    protected static Stream<String> modernSyntaxGradleVersions() {
        return testedGradleVersions()
                .filter(v -> v.compareTo(EXTENSION_AWARE_MIN_VERSION) >= 0)
                .map(GradleVersion::getVersion);
    }

    @BeforeEach
    void setUp() throws IOException {
        withSettingsFile();
    }

    protected void createTestPluginSource() throws IOException {
        createTestPluginSource("org.gradle.plugin", "TestPlugin");
    }

    protected void createTestPluginSource(String packageName, String className) throws IOException {
        createTestPluginSource(testProjectDir, packageName, className);
    }

    protected void createTestPluginSource(String subproject, String packageName, String className) throws IOException {
        createTestPluginSource(testProjectDir.resolve(subproject), packageName, className);
    }

    private void createTestPluginSource(Path projectDir, String packageName, String className) throws IOException {
        var packagePath = packageName.replace('.', '/');
        var testPluginFile = file(projectDir.resolve("src/main/java/%s/%s.java".formatted(packagePath, className)));

        Files.writeString(testPluginFile, """
            package %s;
            import org.gradle.api.Plugin;
            import org.gradle.api.Project;

            public class %s implements Plugin<Project> {
                @Override
                public void apply(Project project) {
                    // No logic needed for this test
                }
            }
            """.formatted(packageName, className));
    }

    protected void withGroovyBuildScript(String content) throws IOException {
        Files.writeString(file("build.gradle"), """
            plugins {
                id('java-gradle-plugin')
                id('org.gradle.plugin.devel.compatibility')
            }

            %s
            """.formatted(content));
    }

    protected void withKotlinBuildScript(String content) throws IOException {
        withKotlinBuildScript("build.gradle.kts", content);
    }

    protected void withKotlinBuildScript(String path, String content) throws IOException {
        Files.writeString(file(path), """
            plugins {
                `java-gradle-plugin`
                id("org.gradle.plugin.devel.compatibility")
            }

            %s
            """.formatted(content));
    }

    protected void withSettingsFile() throws IOException {
        Files.writeString(file("settings.gradle"), "rootProject.name = 'test-plugin'");
    }

    protected List<String> buildArguments(List<String> argumentsBuffer, String... userArgs) {
        // Use Java 8 for running Gradle if configured via org.gradle.java.home
        String java8Home = System.getProperty("java8Home");
        if (java8Home != null) {
            argumentsBuffer.add("-Dorg.gradle.java.home=" + java8Home);
        }
        argumentsBuffer.add("--stacktrace");
        argumentsBuffer.addAll(Arrays.asList(userArgs));
        return argumentsBuffer;
    }

    protected BuildResult runGradle(String... args) {
        return createRunner(args).build();
    }

    protected BuildResult runGradleAndFail(String... args) {
        return createRunner(args).buildAndFail();
    }

    protected Path file(String filePath) throws IOException {
        return file(testProjectDir.resolve(filePath));
    }

    protected Path file(Path theFile) throws IOException {
        Files.createDirectories(theFile.getParent());
        return theFile;
    }

    private GradleRunner createRunner(String... args) {
        return GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.toFile())
                .forwardOutput()
                .withArguments(buildArguments(new ArrayList<>(), args))
                .withPluginClasspath();
    }

    protected PluginDescriptorAssertion assertPluginDescriptor(String pluginId) {
        Path propertiesFile = testProjectDir.resolve(
                "build/resources/main/META-INF/gradle-plugins/" + pluginId + ".properties"
        );
        assertThat(propertiesFile)
                .as("Plugin descriptor file should exist")
                .exists();
        try {
            String content = Files.readString(propertiesFile);
            return new PluginDescriptorAssertion(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read plugin descriptor: " + propertiesFile, e);
        }
    }

    protected PluginDescriptorAssertion assertPluginDescriptor(String subproject, String pluginId) {
        Path propertiesFile = testProjectDir.resolve(
                subproject + "/build/resources/main/META-INF/gradle-plugins/" + pluginId + ".properties"
        );
        assertThat(propertiesFile)
                .as("Plugin descriptor file should exist")
                .exists();
        try {
            String content = Files.readString(propertiesFile);
            return new PluginDescriptorAssertion(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read plugin descriptor: " + propertiesFile, e);
        }
    }

    protected static class PluginDescriptorAssertion extends AbstractAssert<PluginDescriptorAssertion, String> {

        PluginDescriptorAssertion(String content) {
            super(content, PluginDescriptorAssertion.class);
        }

        public PluginDescriptorAssertion hasImplementationClass(String implementationClass) {
            isNotNull();
            String expectedLine = "implementation-class=" + implementationClass + "\n";
            if (!actual.contains(expectedLine)) {
                failWithMessage("Expected plugin descriptor to contain implementation-class=<%s> but did not", implementationClass);
            }
            return this;
        }

        public PluginDescriptorAssertion hasFeature(String featureName, String supportLevel) {
            isNotNull();
            String expectedLine = "compatibility.feature." + featureName + "=" + supportLevel + "\n";
            if (!actual.contains(expectedLine)) {
                failWithMessage("Expected plugin descriptor to contain feature <%s>=<%s> but did not", featureName, supportLevel);
            }
            return this;
        }

        public PluginDescriptorAssertion hasConfigurationCache(String supportLevel) {
            return hasFeature("configuration-cache", supportLevel);
        }

        public PluginDescriptorAssertion hasIsolatedProjects(String supportLevel) {
            return hasFeature("isolated-projects", supportLevel);
        }
    }
}
