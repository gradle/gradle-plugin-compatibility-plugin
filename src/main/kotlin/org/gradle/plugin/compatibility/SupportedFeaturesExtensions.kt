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

package org.gradle.plugin.compatibility

import org.gradle.api.Action
import org.gradle.plugin.devel.PluginDeclaration
import org.gradle.plugin.compatibility.internal.CompatibilityStrategy

/**
 * Applies compatibility configuration defined in `action` to the `PluginDeclaration`.
 */
fun PluginDeclaration.compatibility(action: Action<in CompatibilityExtension>) {
    CompatibilityStrategy.getInstance().configure(this, action)
}
