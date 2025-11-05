plugins {
    `java-gradle-plugin`
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
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.20.0") {
        because("Needed for parsing the Gradle releases metadata JSON")
    }

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations {
    testCompileClasspath {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }

    testRuntimeClasspath {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
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
