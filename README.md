# Gradle Plugin Compatibility Plugin

This plugin extends a Gradle plugin block, and adds extra metainformation that the plugin portal can use.
If you want a badge for your plugin in the Plugin Portal, this is the plugin that you need.

## How to use

### Getting started

First, apply the plugin.

In your `build.gradle.kts`:

```kotlin
plugins {
    id("org.gradle.plugin-compatibility") version "1.0.0"
}
```

Or in your `build.gradle`:

```groovy
plugins {
    id 'org.gradle.plugin-compatibility' version '1.0.0'
}
```

### Extending the plugins

The plugin adds an extension to each `plugins.plugin` block, which you can use to define the extra metadata.

Here is an example how it looks like:

```kotlin
plugins {
    id("java-gradle-plugin")
    id("org.gradle.plugin-compatibility") version "1.0.0"
}

gradlePlugin {
    plugins {
        create("myPlugin") {
            id = "com.example.myplugin"
            implementationClass = "com.example.MyPlugin"
            // This plugin adds the following extension
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
```

## Supported features

In the `compatibility` block, you can define the following `features`:

| Feature              | Description                                                                | Since version |
|----------------------|----------------------------------------------------------------------------|---------------|
| `configurationCache` | Indicates that the plugin is compatible with Gradle's configuration cache. | 1.0.0         |

## Storage

The plugin adds the metainformation into each plugin JAR's `META-INF/gradle-plugins/<plugin-id>.properties` file.
