package org.gradle.plugin.compatibility;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

/**
 * The main entry point to configuring Plugin Compatibility declarations. Can be obtained in the
 * {@code PluginDeclaration} context.
 * <p>
 * In Kotlin DSL, you can use {@code PluginDeclaration.compatibility} extension
 * function. It must be explicitly imported.
 * <pre>
 * <i>// build.gradle.kts</i>
 * <i> Import is necessary in Kotlin Build Script </i>
 * import org.gradle.plugin.compatibility.compatibility
 * plugins {
 *     `java-gradle-plugin`
 *     id("org.gradle.plugin-compatibility") version "9.1.0"
 * }
 *
 * gradlePlugin {
 *     plugins {
 *         register("my-plugin") {
 *             id = "com.example.my.plugin"
 *             implementationClass = "com.example.MyPlugin"
 *             compatibility {
 *                 features {
 *                     configurationCache = true
 *                 }
 *             }
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>
 * In Groovy DSL, the import is not needed. However, the syntax differs slightly between Gradle 8.14+ and older
 * versions.
 * <pre>
 * <i>// build.gradle</i>
 * plugins {
 *     id("java-gradle-plugin")
 *     id("org.gradle.plugin-compatibility") version "9.1.0"
 * }
 *
 * gradlePlugin {
 *     plugins {
 *         register("my-plugin") {
 *             id = "com.example.my.plugin"
 *             implementationClass = "com.example.MyPlugin"
 *             compatibility { <i>// Gradle 8.14+</i>
 *                 features {
 *                     configurationCache = true
 *                 }
 *             }
 *
 *             compatibility(it) { <i>// Gradle 7.4&mdash;8.13, note the <b>it</b> argument. </i>
 *                features {
 *                    configurationCache = true
 *                }
 *            }
 *         }
 *     }
 * }
 * </pre>
 */
@Incubating
public abstract class CompatibilityExtension {

    private final CompatibleFeatures features;

    /**
     * Users should not be creating this class directly.
     */
    @Inject
    public CompatibilityExtension(ObjectFactory objectFactory) {
        this.features = objectFactory.newInstance(CompatibleFeatures.class);
    }

    /**
     * Returns the features to configure.
     *
     * @return the {@link CompatibleFeatures} instance
     */
    public CompatibleFeatures getFeatures() {
        return features;
    }

    /**
     * Applies the action to configure {@link CompatibleFeatures}.
     *
     * @param action the configuration action
     */
    public void features(Action<? super CompatibleFeatures> action) {
        action.execute(features);
    }
}
