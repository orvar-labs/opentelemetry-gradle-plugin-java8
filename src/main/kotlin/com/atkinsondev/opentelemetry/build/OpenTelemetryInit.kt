package com.atkinsondev.opentelemetry.build

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME
import io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_NAME
import io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_VERSION
import org.gradle.api.logging.Logger
import java.util.concurrent.TimeUnit

class OpenTelemetryInit(
    private val logger: Logger,
) {
    fun init(
        endpoint: String,
        headers: Map<String, String>,
        serviceName: String,
        exporterMode: OpenTelemetryExporterMode,
        customTags: Map<String, String>,
    ): OpenTelemetrySdk {
        val customAttributesBuilder = Attributes.builder()
        customTags.forEach { (key, value) -> customAttributesBuilder.put(key, value) }
        val customAttributes = customAttributesBuilder.build()

        val customResourceAttributes =
            Resource
                .builder()
                .put(SERVICE_NAME, serviceName)
                .put(TELEMETRY_SDK_NAME, SDK_NAME)
                .put(TELEMETRY_SDK_VERSION, SDK_VERSION)
                .putAll(customAttributes)
                .build()

        val resource: Resource = Resource.getDefault().merge(customResourceAttributes)

        val spanExporter =
            when (exporterMode) {
                OpenTelemetryExporterMode.GRPC -> {
                    val spanExporterBuilder =
                        OtlpGrpcSpanExporter
                            .builder()
                            .setTimeout(2, TimeUnit.SECONDS)
                            .setEndpoint(endpoint)
                            .addHeader("User-Agent", USER_AGENT_VALUE)

                    headers.forEach { (key, value) -> spanExporterBuilder.addHeader(key, value) }

                    spanExporterBuilder.build()
                }

                OpenTelemetryExporterMode.ZIPKIN -> {
                    val spanExporterBuilder = ZipkinSpanExporter.builder().setReadTimeout(2, TimeUnit.SECONDS).setEndpoint(endpoint)

                    spanExporterBuilder.build()
                }

                else -> {
                    val spanExporterBuilder =
                        OtlpHttpSpanExporter
                            .builder()
                            .setTimeout(2, TimeUnit.SECONDS)
                            .setEndpoint(endpoint)
                            .addHeader("User-Agent", USER_AGENT_VALUE)

                    headers.forEach { (key, value) -> spanExporterBuilder.addHeader(key, value) }

                    spanExporterBuilder.build()
                }
            }

        logger.info("Registering OpenTelemetry with mode $exporterMode")

        val openTelemetrySdk =
            OpenTelemetrySdk
                .builder()
                .setTracerProvider(
                    SdkTracerProvider
                        .builder()
                        .setResource(resource)
                        .addSpanProcessor(
                            BatchSpanProcessor.builder(spanExporter).setScheduleDelay(100, TimeUnit.MILLISECONDS).build(),
                        ).build(),
                ).build()

        return openTelemetrySdk
    }

    companion object {
        const val SDK_NAME = "gradle-opentelemetry-build-plugin-java8"
        const val SDK_VERSION = "1.14.0-java8.1" // TODO: Find a way to pull this from the Gradle file
        const val USER_AGENT_VALUE = "$SDK_NAME/$SDK_VERSION"
    }
}
