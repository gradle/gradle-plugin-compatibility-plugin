package org.gradle.plugin.devel.compatibility.internal.groovy;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.plugin.devel.PluginDeclaration;
import org.gradle.plugin.devel.compatibility.internal.CompatibilityStrategy;
import org.gradle.plugin.devel.compatibility.internal.CompatibilityStrategyFactory;
import org.jspecify.annotations.Nullable;

import javax.inject.Inject;

public abstract class CompatibilityProjectExtension extends Closure<@Nullable Void> {
    @Inject
    public CompatibilityProjectExtension(Project owner) {
        super(owner);
    }

    @Override
    public Project getOwner() {
        return (Project) super.getOwner();
    }

    public @Nullable Void doCall(Closure<?> ignored) {
        throw new UnsupportedOperationException("compatibility { ... } syntax is only supported since Gradle 8.14+. " +
                "Pass PluginDeclaration explicitly, like plugins { create('foo') { compatibility(it) { ... } }");
    }

    public @Nullable Void doCall(PluginDeclaration declaration, Closure<?> configuration) {
        Project owner = getOwner();
        CompatibilityStrategy strategy = CompatibilityStrategyFactory.getStrategy();
        strategy.configure(declaration, owner, extension -> owner.configure(extension, configuration));
        return null;
    }

    /**
     * Register the necessary extensions so the compatibility syntax works. This method should be only used internally.
     *
     * @param project the project to which the plugin is applied
     */
    public static void install(Project project) {
        // Expose the extension through extra properties, so Groovy lookup can find it, but there is no generated
        // accessor for the Kotlin DSL. Kotlin DSL relies on the explicit import instead.
        project.getExtensions().getExtraProperties().set(
                "compatibility",
                project.getObjects().newInstance(CompatibilityProjectExtension.class, project)
        );
    }
}
