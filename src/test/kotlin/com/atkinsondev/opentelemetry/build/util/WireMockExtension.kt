package com.atkinsondev.opentelemetry.build.util

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class WireMockExtension :
    BeforeEachCallback,
    AfterEachCallback {
    val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

    override fun beforeEach(context: ExtensionContext?) {
        server.start()
        configureFor("localhost", server.port())
    }

    override fun afterEach(context: ExtensionContext?) {
        server.stop()
    }

    fun baseUrl(): String = "http://localhost:${server.port()}"
}
