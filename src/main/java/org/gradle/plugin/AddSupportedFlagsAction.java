package org.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.plugin.devel.PluginDeclaration;
import org.gradle.plugin.devel.tasks.GeneratePluginDescriptors;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class AddSupportedFlagsAction implements Action<Task> {

    public static final String SUPPORT_FLAG_PACKAGE = "feature-support";

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
            SupportedFeatures supportedFeatures = getSupportedFeatures(declaration);

            try (BufferedWriter writer = Files.newBufferedWriter(propertiesFile, StandardOpenOption.APPEND)) {
                // Configuration cache
                writer.write(formatSupportFlag("configuration-cache", supportedFeatures.getConfigurationCache().get()));
                writer.newLine();
                // Isolated projects
                writer.write(formatSupportFlag("isolated-projects", supportedFeatures.getIsolatedProjects().get()));
                writer.newLine();
            } catch (IOException ex) {
                throw new GradleException("Failed to write supported features to " + propertiesFile, ex);
            }
        }
    }

    private static String formatSupportFlag(String name, SupportedFeatureState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(SUPPORT_FLAG_PACKAGE);
        sb.append(".");
        sb.append(name);
        sb.append("=");
        switch (state) {
            case UNKNOWN:
                sb.append("unknown");
                break;
            case SUPPORTED:
                sb.append("supported");
                break;
            case NOT_SUPPORTED:
                sb.append("not-supported");
                break;
            default:
                throw new IllegalArgumentException("Unsupported feature state: " + state);
        }
        return sb.toString();
    }

    private static SupportedFeatures getSupportedFeatures(PluginDeclaration declaration) {
        ExtensionAware extensionAwareDeclaration = (ExtensionAware) declaration;
        return extensionAwareDeclaration.getExtensions().getByType(SupportedFeatures.class);
    }

}
