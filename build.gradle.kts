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
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `jvm-test-suite`
    `kotlin-dsl`
    `maven-publish`
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
    // We are stuck on 2.42.0, because
    //  - Grade 7.4.2 (our oldest cross-version test target) can only use maximum JDK 17
    //  - ErrorProne 2.42.0+ required JDK 21
    errorprone("com.google.errorprone:error_prone_core:2.42.0")
    errorprone("com.uber.nullaway:nullaway:0.12.14")
}

kotlin {
    // Set up the JDK used to compile Java and Kotlin code.
    jvmToolchain(17)
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

val java8Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(8))
}

testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()

            dependencies {
                implementation(platform("org.junit:junit-bom:6.0.0"))
                implementation("org.junit.jupiter:junit-jupiter-api")
                implementation("org.junit.jupiter:junit-jupiter-params")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
                implementation("org.assertj:assertj-core:3.25.3")
            }
        }

        named<JvmTestSuite>("test") {
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")  {
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
        options.release = 8
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
