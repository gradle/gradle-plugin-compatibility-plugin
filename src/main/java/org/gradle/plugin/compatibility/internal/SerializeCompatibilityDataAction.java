/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.gradle.plugin.compatibility.CompatibilityDeclarationProtocol;
import org.gradle.plugin.compatibility.CompatibleFeatures;
import org.gradle.plugin.devel.PluginDeclaration;
import org.gradle.plugin.devel.tasks.GeneratePluginDescriptors;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class SerializeCompatibilityDataAction implements Action<Task> {
    private final ObjectFactory objectFactory;
    private final Provider<Directory> outputDirectory;
    private final Provider<Map<String, CompatibleFeatures>> compatibilityData;

    @SuppressWarnings("InjectOnConstructorOfAbstractClass")
    @Inject
    public SerializeCompatibilityDataAction(ObjectFactory objectFactory, GeneratePluginDescriptors task) {
        this.objectFactory = objectFactory;
        Project project = task.getProject();
        CompatibilityStrategy strategy = CompatibilityStrategy.getInstance();
        outputDirectory = task.getOutputDirectory();
        compatibilityData = project.provider(() ->
            task.getDeclarations().get().stream()
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

    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    private static <T, U> T uncheckedCast(U object) {
        return (T) object;
    }

    private static Provider<Map<String, String>> compatibilityAsMap(CompatibleFeatures features) {
        return toSupportLevel(features.getConfigurationCache()).map(
            configurationCache -> Collections.singletonMap("configurationCache", configurationCache)
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
            writeFeatureSupportLevel(
                writer,
                CompatibilityDeclarationProtocol.FEATURE_CONFIGURATION_CACHE,
                features.getConfigurationCache()
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
