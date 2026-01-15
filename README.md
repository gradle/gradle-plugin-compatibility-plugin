# Gradle Plugin Compatibility Plugin

This plugin extends a Gradle plugin block, and adds extra metainformation that the plugin portal can use.
If you want a badge for your plugin in the Plugin Portal, this is the plugin that you need.

## How to use

### Getting started

First, apply the plugin. It is designed to work with [Gradle Plugin development plugin](https://docs.gradle.org/current/userguide/java_gradle_plugin.html).

In your `build.gradle`/`build.gradle.kts`:

```kotlin
plugins {
    id("java-gradle-plugin") // You need the Gradle Plugin development plugin to define plugins
    id("org.gradle.plugin-compatibility") version "1.0.0"
}
```

### Declare the capabilities

The plugin adds an extension to each `plugins.plugin` block, which you can use to define the extra metadata.

Here is an example how it looks like in Kotlin:

```kotlin
import org.gradle.plugin.compatibility // You have to explicitly import the extension function. 

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
                    configurationCache = true
                }
            }
        }
    }
}
```

It works almost the same way with Groovy:
```groovy
plugins {
    id("java-gradle-plugin") // You need the Gradle Plugin plugin to define plugins 
    id("org.gradle.plugin-compatibility") version "1.0.0"
}

gradlePlugin {
    plugins {
        create("myPlugin") {
            id = "com.example.myplugin"
            implementationClass = "com.example.MyPlugin"
            compatibility { // Extension function added by the plugin. Available since Gradle 8.14.
                features {
                    // Declare that the plugin supports the configuration cache
                    configurationCache = true
                }
            }
            
            // For Gradle versions older than 8.14, you need to use the "legacy" application syntax.
            compatibility(it) { // Pass the PluginDeclaration instance explicitly.
                features {
                    configurationCache = true
                }
            }
        }
    }
}
```

You can also declare that your plugin *doesn't support* a feature (hopefully, yet):
```
// ...
    features {
        configurationCache = false
    }
// ...
```

Gradle may emit warnings for builds that include such a plugin and enable the unsupported feature.
This can help users, who try to adopt a feature in their build, to figure out that they may need to update your plugin to a newer version.

### Features available for declaring

In the `compatibility` block, you can define the following `features`:

| Feature              | Description                                                                | Since version |
|----------------------|----------------------------------------------------------------------------|---------------|
| `configurationCache` | Indicates that the plugin is compatible with Gradle's configuration cache. | 1.0.0         |

See [`org.gradle.plugin.compatibility.CompatibleFeatures`](src/main/java/org/gradle/plugin/compatibility/CompatibleFeatures.java) for the full list.

## Protocol

The plugin adds the metainformation into each plugin JAR's `META-INF/gradle-plugins/<plugin-id>.properties` file.
Metadata definitions can be found in [`org.gradle.plugin.compatibility.CompatibilityDeclarationProtocol`](src/main/java/org/gradle/plugin/compatibility/CompatibilityDeclarationProtocol.java).

## Gradle version support

This plugin supports Gradle versions starting with Gradle 7.4.2. In Groovy DSL, for versions of Gradle older than 8.14, the "legacy" configuration syntax (`compatibility(it)`) must be used.
There are no additional restrictions to the JVM version, anything capable of running the supported Gradle version will do.

This plugin fully supports [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html) and [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html).