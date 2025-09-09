package org.gradle.plugin;

import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class FeatureCompatibility {
    public abstract Property<FeatureCompatibilityState> getConfigurationCache();
    public abstract Property<FeatureCompatibilityState> getIsolatedProjects();

    @Inject
    public FeatureCompatibility() {
        getIsolatedProjects().convention(FeatureCompatibilityState.UNKNOWN);
        getConfigurationCache().convention(FeatureCompatibilityState.UNKNOWN);
    }

}
