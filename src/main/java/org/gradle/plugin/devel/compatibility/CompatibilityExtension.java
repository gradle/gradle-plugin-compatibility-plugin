package org.gradle.plugin.devel.compatibility;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

public abstract class CompatibilityExtension {

    private final CompatibleFeatures features;

    @Inject
    public CompatibilityExtension(ObjectFactory objectFactory) {
        this.features = objectFactory.newInstance(CompatibleFeatures.class);
    }

    public CompatibleFeatures getFeatures() {
        return features;
    }

    public void features(Action<CompatibleFeatures> action) {
        action.execute(features);
    }
}
