package org.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the
import org.gradle.plugin.devel.PluginDeclaration

fun PluginDeclaration.featureCompatibility(action: Action<FeatureCompatibility>) {
    require(this is ExtensionAware)
    action.execute(the<FeatureCompatibility>())
}
