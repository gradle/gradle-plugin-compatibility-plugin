package org.gradle.plugin.devel.compatibility

import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the
import org.gradle.plugin.devel.compatibility.CompatibilityExtension
import org.gradle.plugin.devel.compatibility.internal.CompatibilityRegistry
import org.gradle.plugin.devel.PluginDeclaration

fun PluginDeclaration.compatibility(action: Action<CompatibilityExtension>) {
    if (this is ExtensionAware) {
        this.extensions.configure(CompatibilityExtension::class.java, action)
    } else {
        // Use the workaround for older Gradle version.
        CompatibilityRegistry.store(this, action)
    }
}
