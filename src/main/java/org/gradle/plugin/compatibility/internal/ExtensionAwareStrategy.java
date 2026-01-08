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
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.plugin.compatibility.CompatibilityExtension;
import org.gradle.plugin.compatibility.CompatibleFeatures;
import org.gradle.plugin.devel.PluginDeclaration;

/**
 * Strategy for Gradle >= 8.14 where PluginDeclaration implements ExtensionAware.
 * Uses the ExtensionAware API to directly attach and configure compatibility extensions.
 */
class ExtensionAwareStrategy implements CompatibilityStrategy {

    @Override
    public void createExtension(PluginDeclaration declaration, Project project) {
        if (!(declaration instanceof ExtensionAware)) {
            throw new IllegalStateException("ExtensionAwareStrategy requires PluginDeclaration to implement ExtensionAware");
        }
        ExtensionAware extensionAware = (ExtensionAware) declaration;
        extensionAware.getExtensions().create("compatibility", CompatibilityExtension.class, project.getObjects());
    }

    @Override
    public CompatibleFeatures extractFeatures(PluginDeclaration declaration, Project project) {
        if (!(declaration instanceof ExtensionAware)) {
            throw new IllegalStateException("ExtensionAwareStrategy requires PluginDeclaration to implement ExtensionAware");
        }
        ExtensionAware extensionAware = (ExtensionAware) declaration;
        return extensionAware.getExtensions().getByType(CompatibilityExtension.class).getFeatures();
    }

    @Override
    public void configure(PluginDeclaration declaration, Project project, Action<CompatibilityExtension> configuration) {
        if (!(declaration instanceof ExtensionAware)) {
            throw new IllegalStateException("ExtensionAwareStrategy requires PluginDeclaration to implement ExtensionAware");
        }
        ExtensionAware extensionAware = (ExtensionAware) declaration;
        CompatibilityExtension extension = extensionAware.getExtensions().getByType(CompatibilityExtension.class);
        configuration.execute(extension);
    }
}
