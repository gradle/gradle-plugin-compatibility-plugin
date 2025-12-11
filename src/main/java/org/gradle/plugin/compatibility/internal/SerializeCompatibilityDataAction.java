package org.gradle.plugin.compatibility.internal;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.plugin.devel.PluginDeclaration;
import org.gradle.plugin.compatibility.CompatibilityDeclarationProtocol;
import org.gradle.plugin.compatibility.CompatibleFeatures;
import org.gradle.plugin.devel.tasks.GeneratePluginDescriptors;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class SerializeCompatibilityDataAction implements Action<Task> {
    private final ObjectFactory objectFactory;
    private final Provider<Directory> outputDirectory;
    private final Provider<Map<String, CompatibleFeatures>> compatibilityData;

    @Inject
    public SerializeCompatibilityDataAction(ObjectFactory objectFactory, GeneratePluginDescriptors task) {
        this.objectFactory = objectFactory;
        Project project = task.getProject();
        CompatibilityStrategy strategy = CompatibilityStrategyFactory.getStrategy();
        outputDirectory = task.getOutputDirectory();
        compatibilityData = project.provider(() ->
                task.getDeclarations()
                        .get()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        PluginDeclaration::getId,
                                        declaration -> strategy.extractFeatures(declaration, project)
                                )
                        )
        );
    }

    /**
     * Returns a provider that resolves compatibility data to a serializable format for task inputs.
     * This is necessary because Property&lt;Boolean&gt; values (especially nulls) cannot be reliably
     * serialized by Gradle's task input tracking in older Gradle versions.
     */
    public Provider<Map<String, Map<String, String>>> getSerializableCompatibilityData() {
        return compatibilityData.flatMap(data -> {
            MapProperty<String, Map<String, String>> result = uncheckedCast(
                    objectFactory.mapProperty(String.class, Map.class)
            );
            data.forEach((id, features) -> result.put(id, compatibilityAsMap(features)));
            return result;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T, U> T uncheckedCast(U object) {
        return (T) object;
    }

    private static Provider<Map<String, String>> compatibilityAsMap(CompatibleFeatures features) {
        return toSupportLevel(features.getConfigurationCache())
                .zip(
                        toSupportLevel(features.getIsolatedProjects()),
                        (configurationCache, isolatedProjects) -> {
                            Map<String, String> featureMap = new HashMap<>();
                            featureMap.put("configurationCache", configurationCache);
                            featureMap.put("isolatedProjects", isolatedProjects);
                            return featureMap;
                        });
    }

    @Override
    public void execute(Task task) {
        if (!(task instanceof GeneratePluginDescriptors)) {
            throw new GradleException("Task must be of type GeneratePluginDescriptors");
        }
        compatibilityData.get().forEach(this::addSupportedFlagsToPluginDescriptors);
    }

    private void addSupportedFlagsToPluginDescriptors(String pluginId, CompatibleFeatures features) throws GradleException {
        Path propertiesFile = outputDirectory.get().file(pluginId + ".properties").getAsFile().toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(propertiesFile, StandardOpenOption.APPEND)) {
            writeFeatureSupportLevel(
                    writer,
                    CompatibilityDeclarationProtocol.FEATURE_CONFIGURATION_CACHE,
                    features.getConfigurationCache()
            );
            writeFeatureSupportLevel(
                    writer,
                    CompatibilityDeclarationProtocol.FEATURE_ISOLATED_PROJECTS,
                    features.getIsolatedProjects()
            );
        } catch (IOException ex) {
            throw new GradleException("Failed to write supported features to " + propertiesFile, ex);
        }
    }

    private static void writeFeatureSupportLevel(BufferedWriter writer, String name, Property<Boolean> support) throws IOException {
        writer.write(CompatibilityDeclarationProtocol.SUPPORT_FLAGS_PREFIX);
        writer.write(name);
        writer.write("=");
        writer.write(toSupportLevel(support).get());
        writer.write('\n');
    }

    private static Provider<String> toSupportLevel(Property<Boolean> property) {
        return property.map(
                        value -> value
                                ? CompatibilityDeclarationProtocol.DECLARED_SUPPORTED
                                : CompatibilityDeclarationProtocol.DECLARED_UNSUPPORTED)
                .orElse(CompatibilityDeclarationProtocol.UNDECLARED);
    }
}
