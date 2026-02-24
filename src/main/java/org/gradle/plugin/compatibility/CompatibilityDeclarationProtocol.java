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

/**
 * Various definitions for the plugin descriptor fields. These can be used by other plugins to integrate with defined
 * features.
 *
 * @since 1.0.0
 */
public interface CompatibilityDeclarationProtocol {
    /**
     * The feature is declared as supported. The actual support status may differ due to bugs.
     *
     * @since 1.0.0
     */
    String DECLARED_SUPPORTED = "DECLARED_SUPPORTED";

    /**
     * The feature is declared as unsupported.
     *
     * @since 1.0.0
     */
    String DECLARED_UNSUPPORTED = "DECLARED_UNSUPPORTED";

    /**
     * The status of the feature isn't declared at all.
     *
     * @since 1.0.0
     */
    String UNDECLARED = "UNDECLARED";

    /**
     * The prefix for the properties defining the compatibility with Gradle features. Each feature has its own property.
     *
     * @since 1.0.0
     */
    String SUPPORT_FLAGS_PREFIX = "compatibility.feature.";

    /**
     * The Configuration Cache feature name.
     *
     * @since 1.0.0
     */
    String FEATURE_CONFIGURATION_CACHE = "configuration-cache";
}
