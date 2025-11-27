package org.gradle.plugin.compatibility

import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.plugin.devel.PluginDeclaration
import org.gradle.plugin.compatibility.internal.CompatibilityRegistry

fun PluginDeclaration.compatibility(action: Action<in CompatibilityExtension>) {
    if (this is ExtensionAware) {
        this.extensions.configure(CompatibilityExtension::class.java, action)
    } else {
        // Use the workaround for older Gradle versions.
        CompatibilityRegistry.store(this, action)
    }
}
