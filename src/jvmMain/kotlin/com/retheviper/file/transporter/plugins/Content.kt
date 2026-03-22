package com.retheviper.file.transporter.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.partialcontent.PartialContent

fun Application.configureContent() {
    install(Compression) {
        gzip()
        deflate()
    }
    install(PartialContent)
    install(AutoHeadResponse)
}
