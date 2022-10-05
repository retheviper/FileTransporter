package com.retheviper.file.transporter.plugins

import com.retheviper.file.transporter.constant.API_BASE_PATH
import com.retheviper.file.transporter.constant.ENDPOINT_DOWNLOAD
import com.retheviper.file.transporter.constant.ENDPOINT_LIST
import com.retheviper.file.transporter.constant.ENPOINT_UPLOAD
import com.retheviper.file.transporter.constant.SLASH
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
        get {
            call.respondRedirect("/index.html")
        }
        static {
            resources(SLASH)
        }

        route(API_BASE_PATH) {
            post(ENPOINT_UPLOAD) {
                val multipart = call.receiveMultipart()
                FileService.saveFile(multipart)
            }

            get(ENDPOINT_LIST) {
                val target = call.request.queryParameters["target"]?.ifBlank { SLASH } ?: SLASH
                val tree = FileService.listFileTree(target)
                call.respond(tree)
            }

            get(ENDPOINT_DOWNLOAD) {
                try {
                    val filepath = call.request.queryParameters["filepath"] ?: ""
                    val path = FileService.getFullPath(filepath)
                    if (Files.notExists(path)) {
                        call.respond(HttpStatusCode.BadRequest, "File not found")
                    } else {
                        call.response.header(
                            name = HttpHeaders.ContentDisposition,
                            value = ContentDisposition.Attachment.withParameter(
                                key = ContentDisposition.Parameters.FileName,
                                value = path.fileName.toString().encodeURLPath()
                            ).toString()
                        )
                        call.respondFile(path.toFile())
                    }
                } catch (e: Exception) {
                    call.application.environment.log.info(e.message)
                }
            }
        }
    }
}