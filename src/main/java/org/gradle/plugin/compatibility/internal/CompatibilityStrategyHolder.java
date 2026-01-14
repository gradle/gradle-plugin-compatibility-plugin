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

import org.gradle.util.GradleVersion;

/**
 * Lazy holder for the singleton. Use {@link CompatibilityStrategy#getInstance()} to get the strategy.
 */
class CompatibilityStrategyHolder {
    public static final CompatibilityStrategy INSTANCE = createStrategy();

    private static CompatibilityStrategy createStrategy() {
        if (GradleVersion.current().compareTo(CompatibilityStrategy.EXTENSION_AWARE_MIN_VERSION) >= 0) {
            return new ExtensionAwareStrategy();
        } else {
            return new RegistryStrategy();
        }
    }
}
