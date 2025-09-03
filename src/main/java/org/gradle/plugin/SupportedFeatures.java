package org.gradle.plugin;

import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class SupportedFeatures {
    public abstract Property<SupportedFeatureState> getConfigurationCache();
    public abstract Property<SupportedFeatureState> getIsolatedProjects();

    @Inject
    public SupportedFeatures() {
        getIsolatedProjects().convention(SupportedFeatureState.UNKNOWN);
        getConfigurationCache().convention(SupportedFeatureState.UNKNOWN);
    }

}
