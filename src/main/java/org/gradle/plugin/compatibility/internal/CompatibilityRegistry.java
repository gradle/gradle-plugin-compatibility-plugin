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

public class CompatibilityRegistry {
    private static final ConcurrentWeakIdentityHashMap<
            PluginDeclaration,
            List<Action<CompatibilityExtension>>
            > FEATURE_CONFIGURATORS = new ConcurrentWeakIdentityHashMap<>();

    public static void store(PluginDeclaration declaration, Action<? super CompatibilityExtension> action) {
        FEATURE_CONFIGURATORS.computeIfAbsent(declaration, d -> new ArrayList<>()).add(action::execute);
    }

    public static List<Action<CompatibilityExtension>> getForDeclaration(PluginDeclaration declaration) {
        // This should never return null, as we provide a non-null default value
        return Objects.requireNonNull(FEATURE_CONFIGURATORS.getOrDefault(declaration, Collections.emptyList()));
    }
}
