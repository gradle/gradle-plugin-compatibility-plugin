package org.gradle.plugin.devel

import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the
import org.gradle.plugin.devel.CompatibilityExtension
import org.gradle.plugin.devel.PluginDeclaration

fun PluginDeclaration.compatibility(action: Action<CompatibilityExtension>) {
//    require(this is ExtensionAware)
//    action.execute(the<CompatibilityExtension>())

    CompatibilityRegistry.store(this, action)
}
