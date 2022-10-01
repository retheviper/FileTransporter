package com.retheviper.file.transporter

import com.retheviper.file.transporter.plugins.configureRouting
import com.retheviper.file.transporter.plugins.configureSecurity
import com.retheviper.file.transporter.plugins.configureSerialization
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
//        configureSecurity()
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}
