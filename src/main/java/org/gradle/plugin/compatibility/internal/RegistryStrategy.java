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
import org.gradle.api.Project;
import org.gradle.plugin.compatibility.CompatibilityExtension;
import org.gradle.plugin.compatibility.CompatibleFeatures;
import org.gradle.plugin.devel.PluginDeclaration;

/**
 * Strategy for Gradle < 8.14 where PluginDeclaration does not implement ExtensionAware.
 * Uses a registry-based approach to store and retrieve compatibility configuration.
 */
class RegistryStrategy implements CompatibilityStrategy {

    @Override
    public void createExtension(PluginDeclaration declaration, Project project) {
        // For the registry strategy, we don't create extensions upfront
        // They're registered on-demand when configure() is called
    }

    @Override
    public CompatibleFeatures extractFeatures(PluginDeclaration declaration, Project project) {
        CompatibilityExtension extension = project.getObjects().newInstance(CompatibilityExtension.class);

        CompatibilityRegistry
                .getForDeclaration(declaration)
                .forEach(action -> action.execute(extension));

        return extension.getFeatures();
    }

    @Override
    public void configure(PluginDeclaration declaration, Action<? super CompatibilityExtension> configuration) {
        CompatibilityRegistry.store(declaration, configuration);
    }
}
