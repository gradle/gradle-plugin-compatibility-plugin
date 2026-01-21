# Gradle Plugin Compatibility Plugin

This plugin extends a Gradle plugin block and adds extra metainformation that the [Gradle Plugin Portal](https://plugins.gradle.org/) can use.
If you want a badge for your plugin in the Plugin Portal, this is the plugin that you need.

## How to use

This plugin works with the [Gradle Plugin development plugin](https://docs.gradle.org/current/userguide/java_gradle_plugin.html).
It adds an extension to each `PluginDeclaration` that you can use to define compatibility metadata.

### Kotlin DSL

```kotlin
// IMPORTANT: You must explicitly import the extension function in Kotlin DSL
import org.gradle.plugin.compatibility.compatibility

plugins {
    id("java-gradle-plugin")
    id("org.gradle.plugin-compatibility") version "1.0.0"
}

gradlePlugin {
    plugins {
        create("myPlugin") {
            id = "com.example.myplugin"
            implementationClass = "com.example.MyPlugin"
            compatibility { // Extension function added by the plugin
                features {
                    // Declare that the plugin supports the configuration cache
                    configurationCache.set(true) // configurationCache = true also works in Gradle 8.2+
                }
            }
        }
    }
}
```

### Groovy DSL

The import is not needed in Groovy DSL, but the syntax differs between Gradle versions.

**For Gradle 8.14 and newer:**
```groovy
plugins {
    id("java-gradle-plugin")
    id("org.gradle.plugin-compatibility") version "1.0.0"
}

gradlePlugin {
    plugins {
        create("myPlugin") {
            id = "com.example.myplugin"
            implementationClass = "com.example.MyPlugin"
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
```

**For Gradle 8.13 and older (legacy syntax):**
```groovy
plugins {
    id("java-gradle-plugin")
    id("org.gradle.plugin-compatibility") version "1.0.0"
}

gradlePlugin {
    plugins {
        create("myPlugin") {
            id = "com.example.myplugin"
            implementationClass = "com.example.MyPlugin"
            compatibility(it) { // Note: pass 'it' explicitly
                features {
                    configurationCache = true
                }
            }
        }
    }
}
```

### Declaring unsupported features

You can also declare that your plugin *doesn't support* a feature.
This is useful when your plugin is known to be incompatible with a Gradle feature:

```kotlin
compatibility {
    features {
        configurationCache.set(false)
    }
}
```

When a build enables the unsupported feature, Gradle may emit warnings pointing at the unsupported plugin.
This helps users understand why a feature isn't working and signals that they may need to update your plugin to a newer version that adds support.

### Features available for declaring

In the `compatibility` block, you can define the following `features`:

| Feature              | Description                                                                | Since version |
|----------------------|----------------------------------------------------------------------------|---------------|
| `configurationCache` | Indicates that the plugin is compatible with Gradle's [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html). | 1.0.0         |

See [`org.gradle.plugin.compatibility.CompatibleFeatures`](src/main/java/org/gradle/plugin/compatibility/CompatibleFeatures.java) for the full list.

### Important notes

**Configuration-time values:** Feature compatibility values must be computable at configuration time.
They cannot be derived from task outputs or other values that are only available during task execution.

## Gradle version support

This plugin supports Gradle versions starting with Gradle 7.4.2.
In Groovy DSL, for versions of Gradle older than 8.14, the "legacy" configuration syntax (`compatibility(it)`) must be used.
There are no additional restrictions on the JVM version; anything capable of running the supported Gradle version will do.

This plugin fully supports [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html) and [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html).