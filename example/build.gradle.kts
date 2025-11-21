import org.gradle.plugin.devel.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    id("org.gradle.plugin.devel.compatibility") version "9.1.0"
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
