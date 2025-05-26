package com.atkinsondev.opentelemetry.build

import com.atkinsondev.opentelemetry.build.OpenTelemetryInit.Companion.USER_AGENT_VALUE
import com.atkinsondev.opentelemetry.build.util.WireMockExtension
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.awaitility.Awaitility.await
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.*
import java.io.File
import java.nio.file.Path

class OpenTelemetryBuildPluginTest {
    @JvmField
    @RegisterExtension
    val wireMock = WireMockExtension()

    @Test
    fun `should send data to OpenTelemetry with HTTP`(
        @TempDir projectRootDirPath: Path,
    ) {
        val wiremockBaseUrl = wireMock.baseUrl()

        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$wiremockBaseUrl/otel'
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.HTTP
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndClassFile(projectRootDirPath)

        stubFor(post("/otel").willReturn(ok()))

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("test", "--info", "--stacktrace")
                .withPluginClasspath()
                .build()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))

            val otelRequestBodies = otelRequests.map { it.bodyAsString }

            val rootSpanName = "gradle-build"
            expectThat(otelRequestBodies.find { it.contains(rootSpanName) }).isNotNull()
        }

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))

            val otelRequestBodies = otelRequests.map { it.bodyAsString }

            val testSpanName = ":test"
            expectThat(otelRequestBodies.find { it.contains(testSpanName) }).isNotNull()
        }
    }

    @Test
    fun `should include custom SDK name in OTel payload`(
        @TempDir projectRootDirPath: Path,
    ) {
        val wiremockBaseUrl = wireMock.baseUrl()

        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$wiremockBaseUrl/otel'
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.HTTP
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndClassFile(projectRootDirPath)

        stubFor(post("/otel").willReturn(ok()))

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("test", "--info", "--stacktrace")
                .withPluginClasspath()
                .build()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))

            val otelRequestBodies = otelRequests.map { it.bodyAsString }

            val customSdkName = "gradle-opentelemetry-build-plugin"
            expectThat(otelRequestBodies.find { it.contains(customSdkName) }).isNotNull()
        }
    }

    @Test
    fun `should include user agent HTTP header`(
        @TempDir projectRootDirPath: Path,
    ) {
        val wiremockBaseUrl = wireMock.baseUrl()

        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$wiremockBaseUrl/otel'
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.HTTP
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndClassFile(projectRootDirPath)

        stubFor(post("/otel").willReturn(ok()))

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("test", "--info", "--stacktrace")
                .withPluginClasspath()
                .build()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))

            val otelRequestUserAgentHeaders = otelRequests.map { it.header("User-Agent") }

            expectThat(otelRequestUserAgentHeaders).any {
                get { values() }.contains(USER_AGENT_VALUE)
            }
        }
    }

    @Test
    fun `should send data to OpenTelemetry with HTTP and headers`(
        @TempDir projectRootDirPath: Path,
    ) {
        val wiremockBaseUrl = wireMock.baseUrl()

        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$wiremockBaseUrl/otel'
                headers = ["foo1": "bar1", "foo2": "bar2"]
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.HTTP
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndClassFile(projectRootDirPath)

        stubFor(post("/otel").willReturn(ok()))

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("test", "--info", "--stacktrace")
                .withPluginClasspath()
                .build()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))

            expectThat(otelRequests).isNotEmpty()

            expectThat(otelRequests[0].header("foo1").firstValue()).isEqualTo("bar1")
            expectThat(otelRequests[0].header("foo2").firstValue()).isEqualTo("bar2")
        }
    }

    @Test
    fun `should send data to OpenTelemetry with custom attributes`(
        @TempDir projectRootDirPath: Path,
    ) {
        val wiremockBaseUrl = wireMock.baseUrl()

        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$wiremockBaseUrl/otel'
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.HTTP
                customTags = ["foo1": "bar1", "foo2": "bar2"]
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndClassFile(projectRootDirPath)

        stubFor(post("/otel").willReturn(ok()))

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("test", "--info", "--stacktrace")
                .withPluginClasspath()
                .build()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))
            val otelRequestBodies = otelRequests.map { it.bodyAsString }
            System.out.print(otelRequestBodies)

            expectThat(otelRequests).isNotEmpty()
            expectThat(otelRequestBodies.find { it.contains("foo1") }).isNotNull()
            expectThat(otelRequestBodies.find { it.contains("bar1") }).isNotNull()
            expectThat(otelRequestBodies.find { it.contains("foo2") }).isNotNull()
            expectThat(otelRequestBodies.find { it.contains("bar2") }).isNotNull()
        }
    }

    @Test
    fun `when test fails should send failure data`(
        @TempDir projectRootDirPath: Path,
    ) {
        val wiremockBaseUrl = wireMock.baseUrl()

        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$wiremockBaseUrl/otel'
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.HTTP
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndFailingClassFile(projectRootDirPath)

        stubFor(post("/otel").willReturn(ok()))

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("test", "--info", "--stacktrace")
                .withPluginClasspath()
                .buildAndFail()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.FAILED)

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))

            val otelRequestBodies = otelRequests.map { it.bodyAsString }

            val testFailureMessage = "Assertion failed"
            expectThat(otelRequestBodies.find { it.contains(testFailureMessage) }).isNotNull()
        }
    }

    @Test
    fun `when plugin run in CI should include is-CI attribute`(
        @TempDir projectRootDirPath: Path,
    ) {
        val wiremockBaseUrl = wireMock.baseUrl()

        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$wiremockBaseUrl/otel'
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.HTTP
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndClassFile(projectRootDirPath)

        stubFor(post("/otel").willReturn(ok()))

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("test", "--info", "--stacktrace")
                .withEnvironment(mapOf("CI" to "true"))
                .withPluginClasspath()
                .build()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        println(buildResult.output)

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))

            val otelRequestBodies = otelRequests.map { it.bodyAsString }

            val ciSpanAttributeName = "system.is_ci"
            val ciAttributeBody = otelRequestBodies.find { it.contains(ciSpanAttributeName) }
            expectThat(ciAttributeBody).isNotNull()
        }
    }

    @Test
    fun `should include task names`(
        @TempDir projectRootDirPath: Path,
    ) {
        val wiremockBaseUrl = wireMock.baseUrl()

        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$wiremockBaseUrl/otel'
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.HTTP
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndClassFile(projectRootDirPath)

        stubFor(post("/otel").willReturn(ok()))

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("compileKotlin", "test", "--info")
                .withPluginClasspath()
                .build()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))

            val otelRequestBodies = otelRequests.map { it.bodyAsString }

            val taskNames = "compileKotlin test"
            expectThat(otelRequestBodies.find { it.contains(taskNames) }).isNotNull()
        }
    }

    @Test
    fun `should include task type`(
        @TempDir projectRootDirPath: Path,
    ) {
        val wiremockBaseUrl = wireMock.baseUrl()

        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$wiremockBaseUrl/otel'
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.HTTP
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndClassFile(projectRootDirPath)

        stubFor(post("/otel").willReturn(ok()))

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("compileKotlin", "test", "--info")
                .withPluginClasspath()
                .build()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        await().untilAsserted {
            val otelRequests = findAll(postRequestedFor(urlEqualTo("/otel")))

            val otelRequestBodies = otelRequests.map { it.bodyAsString }

            val taskType = "org.gradle.api.tasks.testing.Test"
            expectThat(otelRequestBodies.find { it.contains(taskType) }).isNotNull()
        }
    }
}
