/*
 * Copyright 2025 the original author or authors.
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
 * Strategy for configuring and retrieving compatibility information from plugin declarations.
 * Different strategies are used depending on the Gradle version.
 */
public interface CompatibilityStrategy {

    /**
     * Creates or registers a compatibility extension for the given plugin declaration.
     */
    void createExtension(PluginDeclaration declaration, Project project);

    /**
     * Extracts compatibility features from the given plugin declaration.
     */
    CompatibleFeatures extractFeatures(PluginDeclaration declaration, Project project);

    /**
     * Configures the compatibility extension for the given plugin declaration.
     */
    void configure(PluginDeclaration declaration, Project project, Action<CompatibilityExtension> configuration);
}
