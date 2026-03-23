package com.retheviper.file.transporter.plugins

import com.retheviper.file.transporter.service.DirectoryNotFoundException
import com.retheviper.file.transporter.service.FileNotFoundException
import com.retheviper.file.transporter.service.InvalidPathException
import com.retheviper.file.transporter.service.TargetDirectoryNotFoundException
import com.retheviper.file.transporter.service.TargetNotDirectoryException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond
import java.io.IOException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<InvalidPathException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "Invalid target path")
        }

        exception<TargetNotDirectoryException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "Target is not a directory.")
        }

        exception<TargetDirectoryNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.message ?: "Target directory not found.")
        }

        exception<DirectoryNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.message ?: "Directory not found.")
        }

        exception<FileNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.message ?: "File not found.")
        }

        exception<IOException> { call, cause ->
            if (cause.isMultipartLimitError()) {
                call.respond(
                    HttpStatusCode.PayloadTooLarge,
                    "Upload payload exceeds the server's configured multipart limit."
                )
            } else {
                call.application.environment.log.error("Unhandled I/O error.", cause)
                call.respond(HttpStatusCode.InternalServerError, "Internal server error.")
            }
        }

        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled server error.", cause)
            call.respond(HttpStatusCode.InternalServerError, "Internal server error.")
        }
    }
}
