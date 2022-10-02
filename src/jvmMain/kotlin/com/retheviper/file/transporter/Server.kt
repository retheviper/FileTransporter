package com.retheviper.file.transporter

import com.retheviper.file.transporter.plugins.configureContent
import com.retheviper.file.transporter.plugins.configureRouting
import com.retheviper.file.transporter.plugins.configureSerialization
import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.server.application.Application
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import java.io.File

fun main() {
    val environment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        connector {
            port = 8080
        }
        val keyStoreFile = File("keystore.jks")
        val keyStore = generateCertificate(
            file = keyStoreFile,
            keyAlias = "sampleAlias",
            keyPassword = "foobar",
            jksPassword = "foobar"
        )
        sslConnector(
            keyStore = keyStore,
            keyAlias = "sampleAlias",
            keyStorePassword = { "foobar".toCharArray() },
            privateKeyPassword = { "foobar".toCharArray() }) {
            port = 8443
            keyStorePath = keyStoreFile
        }

//        module(Application::configureSecurity)
        module(Application::configureSerialization)
        module(Application::configureRouting)
        module(Application::configureContent)
    }

    embeddedServer(Netty, environment).start(wait = true)
}