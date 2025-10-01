plugins {
    id("com.gradle.develocity") version("4.2")
}

rootProject.name = "gradle-plugin-compatibility-plugin"
includeBuild("example")

develocity {
    server.set("https://ge.gradle.org")
}
