package com.atkinsondev.opentelemetry.build

import com.atkinsondev.opentelemetry.build.util.WireMockExtension
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.awaitility.Awaitility.await
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.io.File
import java.nio.file.Path

class OpenTelemetryBuildPluginCrossVersionTest {
    @JvmField
    @RegisterExtension
    val wireMock = WireMockExtension()

    @ParameterizedTest
    @ValueSource(strings = ["6.1.1", "6.9.1", "7.0", "7.6.3", "8.4", "8.5"])
    fun `should send data to OpenTelemetry with HTTP with different Gradle versions`(
        gradleVersion: String,
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
                .withGradleVersion(gradleVersion)
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
}
