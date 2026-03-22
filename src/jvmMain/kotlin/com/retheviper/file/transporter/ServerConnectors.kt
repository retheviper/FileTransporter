package com.retheviper.file.transporter

import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.connector
import io.ktor.server.engine.sslConnector
import java.io.File

fun ApplicationEngine.Configuration.configureServerConnectors() {
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
