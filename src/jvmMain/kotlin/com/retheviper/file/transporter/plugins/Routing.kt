package com.retheviper.file.transporter.plugins

import com.retheviper.file.transporter.constant.API_BASE_PATH
import com.retheviper.file.transporter.service.FileService
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPath
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.nio.file.Files

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondRedirect("/index.html")
        }
        // Static plugin. Try to access `/static/index.html`
        static("/") {
            resources("/")
        }

        route(API_BASE_PATH) {
            post("/upload") {
                call.application.environment.log.info("[upload] request")
                val multipart = call.receiveMultipart()
                FileService.saveFile(multipart)
                call.respondRedirect("/")
            }

            get("/list") {
                val target = call.request.queryParameters["target"] ?: "/"
                call.application.environment.log.info("[list] request with: $target")
                val tree = FileService.listFileTree(target)
                call.application.environment.log.info("[list] responses with: $tree")
                call.respond(tree)
            }

            get("/download") {
                try {
                    val filepath = call.request.queryParameters["filepath"] ?: ""
                    call.application.environment.log.info("[download] request with: $filepath")
                    val path = FileService.getFullPath(filepath)
                    if (Files.notExists(path)) {
                        call.respond(HttpStatusCode.BadRequest, "File not found")
                    } else {
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName, path.fileName.toString().encodeURLPath()
                            ).toString()
                        )
                        call.respondFile(path.toFile())
                        call.application.environment.log.info("[download] responses with: ${path.fileName}")
                    }
                } catch (e: Exception) {
                    call.application.environment.log.info("[download] ended with: $e")
                }
            }
        }
    }
}
