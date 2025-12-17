/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `jvm-test-suite`
    checkstyle
    id("net.ltgt.errorprone") version "4.3.0"
    id("net.ltgt.nullaway") version "2.3.0"
}

group = "org.gradle.plugin"
version = "0.1.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.45.0")
    errorprone("com.uber.nullaway:nullaway:0.12.14")

    api("org.jspecify:jspecify:1.0.0")

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

kotlin {
    // Also sets the Java toolchain
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        register("compatibility-plugin") {
            id = "org.gradle.plugin-compatibility"
            implementationClass = "org.gradle.plugin.compatibility.internal.CompatibilityPlugin"
        }
    }
}

val java8Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(8))
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
                        val pluginMetadata = tasks.named("pluginUnderTestMetadata")
                        dependsOn(pluginMetadata)
                        classpath += files(pluginMetadata)
                    }
                }

                // Java 8 target - runs tests with Java 8, skips Gradle 9+
                register("integTestsWithJava8") {
                    testTask.configure {
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

tasks {
    compileJava {
        options.release = 8
    }

    withType<JavaCompile>().configureEach {
        options.errorprone {
            disable("InjectOnConstructorOfAbstractClass") // We use abstract injection as a pattern
            nullaway {
                error()
                onlyNullMarked = true
            }
        }
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions {
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_8)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_8)
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    check {
        dependsOn(testing.suites.named("integTests"))
    }

    register("checkstyle") {
        dependsOn("checkstyleMain", "checkstyleTest", "checkstyleIntegTests")
    }
}
