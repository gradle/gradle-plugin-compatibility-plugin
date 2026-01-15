import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    id("org.gradle.plugin-compatibility") version "1.0.0"
}

gradlePlugin {
    plugins {
        register("dummy-plugin") {
            id = "com.example.dummy"
            implementationClass = "com.example.DummyPlugin"
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
