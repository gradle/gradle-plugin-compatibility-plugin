plugins {
    `java-gradle-plugin`
    `maven-publish`
    `kotlin-dsl`
}

group = "org.gradle"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        // Local directory for testing
        url = uri("file://${rootProject.projectDir}/local-repo")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

gradlePlugin {
    plugins {
        register("supported-features-plugin") {
            id = "org.gradle.supported-plugin-features"
            implementationClass = "org.gradle.plugin.SupportedPluginFeaturesPlugin"
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<JavaCompile>("compileTestJava") {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
