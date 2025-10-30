plugins {
    `java-gradle-plugin`
    `groovy-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "org.gradle.plugin"
version = "9.1.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(platform("org.spockframework:spock-bom:2.3-groovy-4.0"))
    testImplementation("org.spockframework:spock-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    // Also sets the Java toolchain
    jvmToolchain(11)
}

val testCompiler = javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(17))
}
val testLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(17))
}

gradlePlugin {
    plugins {
        register("compatibility-plugin") {
            id = "org.gradle.plugin.devel.compatibility"
            implementationClass = "org.gradle.plugin.devel.CompatibilityPlugin"
        }
    }
}

tasks {
    compileTestJava {
        javaCompiler = testCompiler
    }

    test {
        useJUnitPlatform()
        javaLauncher = testLauncher
    }
}
