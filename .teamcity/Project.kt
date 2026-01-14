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

import jetbrains.buildServer.configs.kotlin.AbsoluteId
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CheckoutMode
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs

private val vcsRoot = AbsoluteId("GradlePluginCompatibilityPlugin")

object Project : Project({
    buildType(Verify)
    buildType(PublishToPluginPortal)
    buildType(ReleaseSnapshot)

    params {
         param("env.DEVELOCITY_ACCESS_KEY", "!awssm://teamcity/gradle-plugin-compatibility-plugin/_all/DEVELOCITY_ACCESS_KEY")
    }
})

private val defaultGradleParameters = listOf(
    "-Dorg.gradle.java.installations.auto-download=false",
    "-Dorg.gradle.java.installations.auto-detect=false",
    "-Dorg.gradle.java.installations.fromEnv=JAVA_HOME,JDK8",
    // TODO(mlopatkin): Remove after upgrading to Gradle 9.2+
    "-Porg.gradle.java.installations.auto-download=false",
    "-Porg.gradle.java.installations.auto-detect=false",
    "-Porg.gradle.java.installations.fromEnv=JAVA_HOME,JDK8",
)

private fun buildGradleParams(vararg extraParams: String) = buildList {
    addAll(defaultGradleParameters)
    addAll(extraParams)
}.joinToString(" ")


abstract class AbstractBuildType(block: BuildType.() -> Unit) : BuildType({
    vcs {
        root(vcsRoot)

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    block()
})

/**
 * Runs plugin tests and validations.
 */
object Verify : AbstractBuildType({
    id = AbsoluteId("VerifyGradlePluginCompatibilityPlugin")
    uuid = "VerifyGradlePluginCompatibilityPlugin"
    name = "Verify Gradle Plugin Compatibility Plugin"
    description = "Verify Gradle Plugin Compatibility Plugin"

    triggers {
        vcs {
            branchFilter = """
                +:*
                """.trimIndent()
        }
    }

    steps {
        gradle {
            useGradleWrapper = true
            tasks = "check"
            gradleParams = buildGradleParams()
        }
    }

    features {
        feature {
            type = "aws-secrets-build-feature"
        }
        commitStatusPublisher {
            vcsRootExtId = vcsRoot.absoluteId
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "%github.bot-gradle.token%"
                }
            }
        }

        pullRequests {
            vcsRootExtId = vcsRoot.absoluteId
            provider = github {
                authType = vcsRoot()
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER
                ignoreDrafts = true
            }
        }
    }
})

/**
 * Publishes release builds of the plugin to the Plugin Portal and to the internal repository.
 */
object PublishToPluginPortal : AbstractBuildType({
    id = AbsoluteId("PublishGradlePluginCompatibilityPlugin")
    uuid = "PublishGradlePluginCompatibilityPlugin"
    type = Type.DEPLOYMENT
    name = "Publish Gradle Plugin Compatibility Plugin"
    description = "Release Gradle Plugin Compatibility Plugin to Plugin Portal"

    steps {
        gradle {
            useGradleWrapper = true
            gradleParams = buildGradleParams(
                "-PpublishSnapshot=false"
            )
            tasks = "publish publishPlugins"
        }
    }

    params {
        param("env.PGP_SIGNING_KEY", "%pgpSigningKey%")
        password("env.PGP_SIGNING_KEY_PASSPHRASE", "%pgpSigningPassphrase%")

        param("env.GRADLE_INTERNAL_REPO_URL", "%gradle.internal.repository.url%")
        param("env.GRADLE_INTERNAL_REPO_USERNAME", "%gradle.internal.repository.build-tool.publish.username%")
        password("env.GRADLE_INTERNAL_REPO_PASSWORD", "%gradle.internal.repository.build-tool.publish.password%")

        param("env.GRADLE_PUBLISH_KEY", "%plugin.portal.publish.key%")
        param("env.GRADLE_PUBLISH_SECRET", "%plugin.portal.publish.secret%")
    }
})

/**
 * Publishes snapshots of the plugin to Artifactory.
 */
object ReleaseSnapshot : AbstractBuildType({
    id = AbsoluteId("PublishSnapshotGradlePluginCompatibilityPlugin")
    uuid = "PublishSnapshotGradlePluginCompatibilityPlugin"
    type = Type.DEPLOYMENT
    name = "Publish Snapshot of Gradle Plugin Compatibility Plugin"
    description = "Publish Snapshot Gradle Plugin Compatibility Plugin to Internal Repo"

    steps {
        gradle {
            useGradleWrapper = true
            gradleParams = buildGradleParams(
                "-PpublishSnapshot=true"
            )
            tasks = "publish"
        }
    }

    params {
        param("env.PGP_SIGNING_KEY", "%pgpSigningKey%")
        password("env.PGP_SIGNING_KEY_PASSPHRASE", "%pgpSigningPassphrase%")

        param("env.GRADLE_INTERNAL_REPO_URL", "%gradle.internal.repository.url%")
        param("env.GRADLE_INTERNAL_REPO_USERNAME", "%gradle.internal.repository.build-tool.publish.username%")
        password("env.GRADLE_INTERNAL_REPO_PASSWORD", "%gradle.internal.repository.build-tool.publish.password%")
    }
})