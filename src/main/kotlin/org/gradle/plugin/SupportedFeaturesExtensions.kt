package org.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the
import org.gradle.plugin.devel.PluginDeclaration

fun PluginDeclaration.supportedFeatures(action: Action<SupportedFeatures>) {
    require(this is ExtensionAware)
    action.execute(the<SupportedFeatures>())
}
