package com.atkinsondev.opentelemetry.build.util

import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.CopyOnWriteArrayList

class OtlpGrpcMockExtension :
    BeforeEachCallback,
    AfterEachCallback {
    private lateinit var server: Server
    val receivedRequests = CopyOnWriteArrayList<ExportTraceServiceRequest>()
    private var boundPort: Int = 0

    override fun beforeEach(context: ExtensionContext) {
        server =
            NettyServerBuilder
                .forPort(0) // dynamic free port
                .addService(
                    object : TraceServiceGrpc.TraceServiceImplBase() {
                        override fun export(
                            request: ExportTraceServiceRequest,
                            responseObserver: StreamObserver<ExportTraceServiceResponse>,
                        ) {
                            receivedRequests.add(request)
                            responseObserver.onNext(ExportTraceServiceResponse.getDefaultInstance())
                            responseObserver.onCompleted()
                        }
                    },
                ).build()
                .start()
        boundPort = server.port
    }

    override fun afterEach(context: ExtensionContext) {
        server.shutdownNow()
        receivedRequests.clear()
    }

    fun endpoint(): String = "http://localhost:$boundPort"
}
