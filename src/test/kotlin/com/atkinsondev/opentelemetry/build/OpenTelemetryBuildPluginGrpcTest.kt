package com.atkinsondev.opentelemetry.build

import com.atkinsondev.opentelemetry.build.util.OtlpGrpcMockExtension
import org.awaitility.Awaitility.await
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import java.io.File
import java.nio.file.Path

class OpenTelemetryBuildPluginGrpcTest {
    @JvmField
    @RegisterExtension
    val grpcMock = OtlpGrpcMockExtension()

    @Test
    fun `should send data to OpenTelemetry with GRPC`(
        @TempDir projectRootDirPath: Path,
    ) {
        val grpcMockBaseUrl = grpcMock.endpoint()
        val buildFileContents =
            """
            ${baseBuildFileContents()}
            
            openTelemetryBuild {
                endpoint = '$grpcMockBaseUrl/opentelemetry.proto.collector.trace.v1.TraceService/Export'
                exporterMode = com.atkinsondev.opentelemetry.build.OpenTelemetryExporterMode.GRPC
            }
            """.trimIndent()

        File(projectRootDirPath.toFile(), "build.gradle").writeText(buildFileContents)

        createSrcDirectoryAndClassFile(projectRootDirPath)
        createTestDirectoryAndClassFile(projectRootDirPath)

        val buildResult =
            GradleRunner
                .create()
                .withProjectDir(projectRootDirPath.toFile())
                .withArguments("test", "--info", "--stacktrace")
                .withPluginClasspath()
                .build()

        expectThat(buildResult.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        await().untilAsserted {
            expectThat(grpcMock.receivedRequests).isNotEmpty()
        }
    }
}
