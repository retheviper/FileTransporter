package com.retheviper.file.transporter

import com.retheviper.file.transporter.config.AppConfig
import com.retheviper.file.transporter.config.AppSettings
import com.retheviper.file.transporter.di.applicationModule
import com.retheviper.file.transporter.plugins.configureContent
import com.retheviper.file.transporter.plugins.configureLogging
import com.retheviper.file.transporter.plugins.configureRouting
import com.retheviper.file.transporter.plugins.configureSerialization
import com.retheviper.file.transporter.plugins.configureStatusPages
import com.retheviper.file.transporter.service.FileStorageService
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.server.application.Application
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory
import java.io.File

fun main() {
    embeddedServer(
        factory = Netty,
        environment = applicationEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
        },
        configure = {
            connectors()
        },
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    stopKoin()
    val koin = startKoin {
        modules(applicationModule())
    }.koin
    val settings = koin.get<AppSettings>()
    val fileStorageService = koin.get<FileStorageService>()

    configureLogging()
    configureSerialization()
    configureStatusPages()
    configureRouting(
        fileStorageService = fileStorageService,
        maxUploadFileSizeBytes = settings.maxUploadFileSizeBytes
    )
    configureContent()
}

private fun ApplicationEngine.Configuration.connectors() {
    val keyStoreFile = File("keystore.jks")
    val keyStore = buildKeyStore {
        certificate("sampleAlias") {
            password = "foobar"
            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
        }
    }
    keyStore.saveToFile(keyStoreFile, "foobar")

    connector {
        host = "0.0.0.0"
        port = 8080
    }
    sslConnector(
        keyStore = keyStore,
        keyAlias = "sampleAlias",
        keyStorePassword = { "foobar".toCharArray() },
        privateKeyPassword = { "foobar".toCharArray() }
    ) {
        host = "0.0.0.0"
        port = 8443
        keyStorePath = keyStoreFile
    }
}
