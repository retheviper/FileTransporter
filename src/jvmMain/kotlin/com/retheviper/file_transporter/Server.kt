package com.retheviper.file_transporter

import com.retheviper.file_transporter.plugins.configureRouting
import com.retheviper.file_transporter.plugins.configureSecurity
import com.retheviper.file_transporter.plugins.configureSerialization
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureSecurity()
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}
