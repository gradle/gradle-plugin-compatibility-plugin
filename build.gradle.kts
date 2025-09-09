plugins {
    `java-gradle-plugin`
    `groovy-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "org.gradle.plugin"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        // Local directory for testing
        url = uri("file://${rootProject.projectDir}/local-repo")
    }
}

dependencies {
    testImplementation(platform("org.spockframework:spock-bom:2.3-groovy-4.0"))
    testImplementation("org.spockframework:spock-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

gradlePlugin {
    plugins {
        register("feature-compatibility-plugin") {
            id = "org.gradle.plugin.feature-compatibility"
            implementationClass = "org.gradle.plugin.FeatureCompatibilityPlugin"
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
