package org.gradle.plugin.compatibility.internal.groovy;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.plugin.devel.PluginDeclaration;
import org.gradle.plugin.compatibility.internal.CompatibilityStrategy;
import org.gradle.plugin.compatibility.internal.CompatibilityStrategyFactory;
import org.jspecify.annotations.Nullable;

import javax.inject.Inject;

/**
 * An extension that provides {@code Project.compatibility(PluginDeclaration, Closure)} for Gradle before 8.14. The
 * {@link #doCall(PluginDeclaration, Closure)} is effectively the public API.
 */
public abstract class CompatibilityProjectExtension extends Closure<@Nullable Void> {
    @Inject
    public CompatibilityProjectExtension(Project owner) {
        super(owner);
    }

    @Override
    public Project getOwner() {
        return (Project) super.getOwner();
    }

    /**
     * A helper that provides a nicer error message when the build logic uses modern syntax on the old Gradle version.
     *
     * @param ignored configuration closure
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    @SuppressWarnings("unused") // Groovy magic
    public @Nullable Void doCall(Closure<?> ignored) {
        throw new UnsupportedOperationException("compatibility { ... } syntax is only supported since Gradle 8.14+. " +
                "Pass PluginDeclaration explicitly, like plugins { create('foo') { compatibility(it) { ... } }");
    }

    /**
     * Configure the provided declaration, maybe lazily.
     * <p>
     * <b>This method is part of the public API.</b>
     *
     * @param declaration   the declaration to configure
     * @param configuration the configuration closure
     * @return {@code null}
     */
    @SuppressWarnings("unused") // Groovy magic
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
