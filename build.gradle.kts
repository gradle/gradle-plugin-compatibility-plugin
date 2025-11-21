plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `jvm-test-suite`
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

    testImplementation("org.assertj:assertj-core:3.25.3")
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_8)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_8)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

val testCompiler = javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(17))
}
val testLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(17))
}
val java8Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(8))
}

gradlePlugin {
    plugins {
        register("compatibility-plugin") {
            id = "org.gradle.plugin.devel.compatibility"
            implementationClass = "org.gradle.plugin.devel.compatibility.internal.CompatibilityPlugin"
        }
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        val integTests by registering(JvmTestSuite::class) {
            useJUnitJupiter()

            dependencies {
                implementation(project())
                implementation(gradleTestKit())
                implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
                implementation(platform("org.junit:junit-bom:6.0.0"))
                implementation("org.junit.jupiter:junit-jupiter-api")
                implementation("org.junit.jupiter:junit-jupiter-params")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
                implementation("org.assertj:assertj-core:3.25.3")
            }

            targets {
                all {
                    testTask.configure {
                        javaLauncher = testLauncher
                        val pluginMetadata = tasks.named("pluginUnderTestMetadata")
                        dependsOn(pluginMetadata)
                        classpath += files(pluginMetadata)
                    }
                }

                // Java 8 target - runs tests with Java 8, skips Gradle 9+
                register("java8") {
                    testTask.configure {
                        javaLauncher = testLauncher
                        systemProperty("java8Home", java8Launcher.get().metadata.installationPath.asFile.absolutePath)
                        val pluginMetadata = tasks.named("pluginUnderTestMetadata")
                        dependsOn(pluginMetadata)
                        classpath += files(pluginMetadata)
                    }
                }
            }
        }
    }
}

configurations {
    named("integTestsCompileClasspath") {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }

    named("integTestsRuntimeClasspath") {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
}

tasks {

    compileJava {
        options.release = 8
    }

    compileTestJava {
        javaCompiler = testCompiler
    }

    withType<JavaCompile>().matching { it.name == "compileIntegTestsJava" }.configureEach {
        javaCompiler = testCompiler
    }

    test {
        useJUnitPlatform()
        javaLauncher = testLauncher
    }

    check {
        dependsOn(testing.suites.named("integTests"))
    }
}
