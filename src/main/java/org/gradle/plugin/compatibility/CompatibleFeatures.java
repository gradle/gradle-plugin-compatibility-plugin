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

package org.gradle.plugin.compatibility;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

/**
 * Defines the plugin's compatibility with various Gradle features.
 *
 * @since 1.0.0
 */
public abstract class CompatibleFeatures {
    /**
     * Defines the compatibility with the Configuration Cache.
     * <p>
     * Due to internal limitations, the value must be computable at configuration time. In particular, it cannot be
     * derived from a task output.
     * <p>
     * When this property has no value, the compatibility is considered "undefined".
     * In the future, the Gradle Plugin Portal may stop accepting plugins with undefined compatibility status.
     *
     * @return the Property object
     * @since 1.0.0
     */
    @Input
    @Optional
    public abstract Property<Boolean> getConfigurationCache();
}
