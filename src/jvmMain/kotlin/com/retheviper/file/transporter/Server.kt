package com.retheviper.file.transporter

import com.retheviper.file.transporter.plugins.configureRouting
import com.retheviper.file.transporter.plugins.configureSerialization
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.partialcontent.PartialContent

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
//        configureSecurity()
        configureSerialization()
        configureRouting()
        install(PartialContent)
        install(AutoHeadResponse)
    }.start(wait = true)
}
