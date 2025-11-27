package org.gradle.plugin.compatibility;

import org.gradle.api.Incubating;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

/**
 * Defines the plugin's compatibility with various Gradle features.
 */
@Incubating
public abstract class CompatibleFeatures {
    /**
     * Defines the compatibility with the Configuration Cache.
     * <p>
     * Due to internal limitations, the value must be computable at configuration time. In particular, it cannot be
     * derived from a task output.
     *
     * @return the Property object
     */
    @Input
    @Optional
    public abstract Property<Boolean> getConfigurationCache();

    /**
     * Defines the compatibility with the Isolated Projects. The Isolated Projects feature is in the pre-alpha state,
     * so the compatibility target isn't yet defined. Therefore, we do not recommend declaring the
     * compatibility/incompatibility for published plugins.
     * <p>
     * Due to internal limitations, the value must be computable at configuration time. In particular, it cannot be
     * derived from a task output.
     *
     * @return the Property object
     * @deprecated Do not use in production yet.
     */
    @Input
    @Optional
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public abstract Property<Boolean> getIsolatedProjects();
}
