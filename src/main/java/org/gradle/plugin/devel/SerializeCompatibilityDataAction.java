package org.gradle.plugin.devel;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.plugin.devel.tasks.GeneratePluginDescriptors;
import org.jspecify.annotations.NullMarked;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Collectors;

@NullMarked
public class SerializeCompatibilityDataAction implements Action<Task> {

    public static final String SUPPORT_FLAG_PACKAGE = "compatibility.feature";
    private final Provider<Directory> outputDirectory;
    private final Provider<Map<String, CompatibleFeatures>> compatibilityData;

    public SerializeCompatibilityDataAction(GeneratePluginDescriptors task) {
        outputDirectory = task.getOutputDirectory();
        compatibilityData = task.getProject().provider(() ->
                task.getDeclarations()
                        .get()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        PluginDeclaration::getId,
                                        SerializeCompatibilityDataAction::extractCompatibilityFeatures
                                )
                        )
        );
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
            writeFeatureSupportLevel(writer, "configuration-cache", features.getConfigurationCache());
            writeFeatureSupportLevel(writer, "isolated-projects", features.getIsolatedProjects());
        } catch (IOException ex) {
            throw new GradleException("Failed to write supported features to " + propertiesFile, ex);
        }
    }

    private static void writeFeatureSupportLevel(BufferedWriter writer, String name, Property<Boolean> support) throws IOException {
        writer.write(SUPPORT_FLAG_PACKAGE);
        writer.write(".");
        writer.write(name);
        writer.write("=");
        writer.write(support.map(supported -> supported ? "SUPPORTED" : "NOT_SUPPORTED").getOrElse("UNKNOWN"));
        writer.write('\n');
    }

    private static CompatibleFeatures extractCompatibilityFeatures(PluginDeclaration declaration) {
        ExtensionAware extensionAwareDeclaration = (ExtensionAware) declaration;
        CompatibilityExtension compatibilityExtension = extensionAwareDeclaration.getExtensions().getByType(CompatibilityExtension.class);
        return compatibilityExtension.getFeatures();
    }

}
