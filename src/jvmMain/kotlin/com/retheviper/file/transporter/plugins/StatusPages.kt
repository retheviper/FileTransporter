package com.retheviper.file.transporter.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond
import java.io.IOException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IOException> { call, cause ->
            if (cause.isMultipartLimitError()) {
                call.respond(
                    HttpStatusCode.PayloadTooLarge,
                    "Upload payload exceeds the server's configured multipart limit."
                )
            } else {
                throw cause
            }
        }
    }
}
