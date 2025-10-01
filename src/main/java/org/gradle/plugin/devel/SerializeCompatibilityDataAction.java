package org.gradle.plugin.devel;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;
import org.gradle.plugin.devel.tasks.GeneratePluginDescriptors;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SerializeCompatibilityDataAction implements Action<Task> {

    public static final String SUPPORT_FLAG_PACKAGE = "compatibility.feature";

    @Override
    public void execute(Task task) {
        if (!(task instanceof GeneratePluginDescriptors)) {
            throw new GradleException("Task must be of type GeneratePluginDescriptors");
        }

        GeneratePluginDescriptors generatePluginDescriptorsTask = (GeneratePluginDescriptors) task;
        addSupportedFlagsToPluginDescriptors(generatePluginDescriptorsTask);
    }

    private static void addSupportedFlagsToPluginDescriptors(GeneratePluginDescriptors task) throws GradleException {
        Path outputDirectory = task.getOutputDirectory().get().getAsFile().toPath();

        for (PluginDeclaration declaration : task.getDeclarations().get()) {
            Path propertiesFile = outputDirectory.resolve(declaration.getId() + ".properties");
            CompatibilityExtension compatibilityExtension = getCompatibilityExtension(declaration);
            CompatibleFeatures features = compatibilityExtension.getFeatures();

            try (BufferedWriter writer = Files.newBufferedWriter(propertiesFile, StandardOpenOption.APPEND)) {
                writeFeatureSupportLevel(writer, "configuration-cache", features.getConfigurationCache());
                writeFeatureSupportLevel(writer, "isolated-projects", features.getIsolatedProjects());
            } catch (IOException ex) {
                throw new GradleException("Failed to write supported features to " + propertiesFile, ex);
            }
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

    private static CompatibilityExtension getCompatibilityExtension(PluginDeclaration declaration) {
        ExtensionAware extensionAwareDeclaration = (ExtensionAware) declaration;
        return extensionAwareDeclaration.getExtensions().getByType(CompatibilityExtension.class);
    }

}
