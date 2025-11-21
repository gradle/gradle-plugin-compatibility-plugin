package org.gradle.plugin.devel.compatibility;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

public abstract class CompatibleFeatures {
    @Input
    @Optional
    public abstract Property<Boolean> getConfigurationCache();
    @Input
    @Optional
    public abstract Property<Boolean> getIsolatedProjects();
}
