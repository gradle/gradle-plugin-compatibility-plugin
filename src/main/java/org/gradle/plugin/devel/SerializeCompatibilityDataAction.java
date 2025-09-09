package org.gradle.plugin.devel;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
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
            CompatibleFeatures compatibleFeatures = compatibilityExtension.getFeatures();

            try (BufferedWriter writer = Files.newBufferedWriter(propertiesFile, StandardOpenOption.APPEND)) {
                // Configuration cache
                writer.write(formatSupportFlag("configuration-cache", compatibleFeatures.getConfigurationCache().get()));
                writer.newLine();
                // Isolated projects
                writer.write(formatSupportFlag("isolated-projects", compatibleFeatures.getIsolatedProjects().get()));
                writer.newLine();
            } catch (IOException ex) {
                throw new GradleException("Failed to write supported features to " + propertiesFile, ex);
            }
        }
    }

    private static String formatSupportFlag(String name, FeatureCompatibilityState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(SUPPORT_FLAG_PACKAGE);
        sb.append(".");
        sb.append(name);
        sb.append("=");
        switch (state) {
            case UNKNOWN:
                sb.append("UNKNOWN");
                break;
            case SUPPORTED:
                sb.append("SUPPORTED");
                break;
            case NOT_SUPPORTED:
                sb.append("NOT-SUPPORTED");
                break;
            default:
                throw new IllegalArgumentException("Unsupported feature state: " + state);
        }
        return sb.toString();
    }

    private static CompatibilityExtension getCompatibilityExtension(PluginDeclaration declaration) {
        ExtensionAware extensionAwareDeclaration = (ExtensionAware) declaration;
        return extensionAwareDeclaration.getExtensions().getByType(CompatibilityExtension.class);
    }

}
