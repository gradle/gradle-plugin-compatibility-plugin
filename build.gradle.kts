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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `jvm-test-suite`
    `kotlin-dsl`
    `maven-publish`
    checkstyle

    alias(libs.plugins.errorprone)
    alias(libs.plugins.nullaway)
}

group = "org.gradle.plugin"
version = libs.versions.project.get()

repositories {
    mavenCentral()
}

dependencies {
    errorprone(libs.build.errorprone)
    errorprone(libs.build.nullaway)
}

kotlin {
    // Set up the JDK used to compile Java and Kotlin code.
    jvmToolchain(libs.versions.jvm.compileJdk)
}

gradlePlugin {
    plugins {
        register("compatibility-plugin") {
            id = "org.gradle.plugin-compatibility"
            implementationClass = "org.gradle.plugin.compatibility.internal.CompatibilityPlugin"
        }
    }
    // TODO(mlopatkin) Apply the plugin and define its compatibility.
}

testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()

            dependencies {
                implementation(libs.test.assertj.core)
                implementation(platform(libs.test.junit.bom))
                implementation(libs.test.junit.jupiter.api)
                implementation(libs.test.junit.jupiter.params)

                runtimeOnly(libs.test.junit.jupiter.engine)
                runtimeOnly(libs.test.junit.platform.launcher)
            }
        }

        named<JvmTestSuite>("test") {
            dependencies {
                implementation(libs.test.jackson.databind)  {
                    because("Needed for parsing the Gradle releases metadata JSON")
                }
            }
        }

        val integTests by registering(JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(gradleTestKit())
            }

            targets {
                all {
                    testTask.configure {
                        val pluginMetadata = tasks.pluginUnderTestMetadata
                        dependsOn(pluginMetadata)
                        classpath += files(pluginMetadata)
                    }
                }

                val java8Launcher = javaToolchains.launcherFor {
                    languageVersion(libs.versions.jvm.productionTarget)
                }

                // Java 8 target - runs tests with Java 8, skips Gradle 9+
                register("java8IntegTest") {
                    testTask.configure {
                        systemProperty("java8Home", java8Launcher.get().metadata.installationPath.asFile.absolutePath)
                    }
                }
            }
        }

        tasks.check {
            dependsOn(integTests)
        }
    }
}

tasks {
    compileJava {
        // Compile production code to Java 8 bytecode with Java 8 APIs
        options.release(libs.versions.jvm.productionTarget)
    }

    withType<JavaCompile>().configureEach {
        options.errorprone {
            disable("InjectOnConstructorOfAbstractClass") // We use abstract injection as a pattern
            disable("EqualsGetClass") // We have complex inner types where getClass makes sense
            nullaway {
                error()
                onlyNullMarked = true
            }
        }
    }

    withType<KotlinCompile>().configureEach {
        // I don't understand why, but setting this config in `kotlin` block doesn't work, compilation on Gradle 7.4 fails
        // with an error in Protobuf metadata parsing.
        compilerOptions {
            // Compile production code to Kotlin 1.8 to ensure compatibility with older Gradle versions, like 7.4
            languageVersion = KotlinVersion.KOTLIN_1_8
            apiVersion = KotlinVersion.KOTLIN_1_8
            // Compile production code to Java 8 bytecode
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    register("checkstyle") {
        dependsOn("checkstyleMain", "checkstyleTest", "checkstyleIntegTests")
    }
}


fun KotlinJvmProjectExtension.jvmToolchain(version: Provider<out String>) {
    this.jvmToolchain(version.get().toInt())
}

fun JavaToolchainSpec.languageVersion(version: Provider<out String>) {
    this.languageVersion = version.map(JavaLanguageVersion::of)
}

fun CompileOptions.release(version: Provider<out String>) {
    this.release.set(version.get().toInt())
}
