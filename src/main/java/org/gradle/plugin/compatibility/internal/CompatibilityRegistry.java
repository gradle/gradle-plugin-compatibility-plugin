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
import org.gradle.plugin.compatibility.CompatibilityExtension;
import org.gradle.plugin.devel.PluginDeclaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A holder for configurations of plugin declarations in the "legacy" mode.
 * <p>
 * We want to provide nice user API, where people can take a {@code PluginDeclaration} and apply compatibility configuration onto it.
 * Then later, the descriptor-generating task action can consume the data and enrich the plugin descriptor.
 * The task has plugin declarations as its input, so sounds easy, right?
 * <p>
 * It is easy with modern Gradle - just use ExtensionAware and add the data directly to PluginDeclaration.
 * However, older versions have bugs that prevent that, starting from the fact that {@code PluginDeclaration} is not extensible.
 * <p>
 * This is where this class enters. It offers a global storage for configuration actions applied to plugin declarations.
 * It also takes classloader shenanigans into account.
 * <p>
 * This class has to be static-only because of Kotlin: there is no Project context available to the extension function.
 * This is also why we're storing actions rather than applying them immediately - there is no way to instantiate the {@link CompatibilityExtension}
 * to configure.
 */
public class CompatibilityRegistry {
    // All three qualifiers of this HashMap are important:
    // - Concurrent: with Isolated projects, multiple projects may add configuration actions concurrently.
    // - Weak: due to classloader reuse, this class may live across multiple builds. Stale plugin declarations should be cleaned up.
    // - Identity: PluginDeclaration has name-based `equals` implementation.
    //     We don't want to mix different instances of them, between projects or build invocations even.
    //     Identity works well within the same project - task is able to find necessary declarations.
    private static final ConcurrentWeakIdentityHashMap<PluginDeclaration, List<Action<CompatibilityExtension>>> FEATURE_CONFIGURATORS =
        new ConcurrentWeakIdentityHashMap<>();

    public static void store(PluginDeclaration declaration, Action<? super CompatibilityExtension> action) {
        // It is unlikely that we're going to configure the same plugin declaration concurrently,
        // but it doesn't hurt to be prepared for that.
        List<Action<CompatibilityExtension>> actions = FEATURE_CONFIGURATORS.computeIfAbsent(
            declaration,
            d -> Collections.synchronizedList(new ArrayList<>())
        );
        actions.add(action::execute);
    }

    public static List<Action<CompatibilityExtension>> getForDeclaration(PluginDeclaration declaration) {
        // This should never return null, as we provide a non-null default value
        return Objects.requireNonNull(FEATURE_CONFIGURATORS.getOrDefault(declaration, Collections.emptyList()));
    }
}
