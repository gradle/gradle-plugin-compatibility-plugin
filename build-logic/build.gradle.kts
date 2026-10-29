/*
 * Copyright 2026 the original author or authors.
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

plugins {
    `java-gradle-plugin`
    `jvm-test-suite`
}

dependencies {
    // Gradle 9.7+ ships this library and exposes it to plugins, so compile against the version Gradle ships
    compileOnly(embeddedKotlin("metadata-jvm"))
}

gradlePlugin {
    plugins {
        register("kotlinFacade") {
            id = "gradlebuild.kotlin-facade"
            implementationClass = "gradlebuild.kotlinfacade.KotlinFacadePlugin"
        }
    }
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter()

            dependencies {
                implementation(gradleTestKit())
                implementation(libs.test.assertj.core)
                implementation(platform(libs.test.junit.bom))
                implementation(libs.test.junit.jupiter.api)

                runtimeOnly(libs.test.junit.jupiter.engine)
                runtimeOnly(libs.test.junit.platform.launcher)
            }
        }
    }
}
