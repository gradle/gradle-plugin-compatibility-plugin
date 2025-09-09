package org.gradle.plugin.devel;

import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class CompatibleFeatures {
    public abstract Property<FeatureCompatibilityState> getConfigurationCache();
    public abstract Property<FeatureCompatibilityState> getIsolatedProjects();

    @Inject
    public CompatibleFeatures() {
        getIsolatedProjects().convention(FeatureCompatibilityState.UNKNOWN);
        getConfigurationCache().convention(FeatureCompatibilityState.UNKNOWN);
    }
}
