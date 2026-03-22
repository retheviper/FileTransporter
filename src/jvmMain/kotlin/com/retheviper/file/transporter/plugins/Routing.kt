package com.retheviper.file.transporter.plugins

import com.retheviper.file.transporter.config.AppConfig
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
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.BadRequestException
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
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException

fun Application.configureRouting() {
    routing {
        get {
            call.respondRedirect("/index.html")
        }
        staticResources(remotePath = SLASH, basePackage = SLASH)

        route(API_BASE_PATH) {
            post(ENPOINT_UPLOAD) {
                try {
                    val multipart = call.receiveMultipart(
                        formFieldLimit = AppConfig.settings().maxUploadFileSizeBytes
                    )
                    val uploadCount = FileService.saveFile(multipart)
                    call.respond(HttpStatusCode.Created, mapOf("uploaded" to uploadCount))
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException(e.message ?: "Invalid upload request", e)
                } catch (e: NoSuchFileException) {
                    call.respond(HttpStatusCode.NotFound, "Target directory not found.")
                } catch (e: NotDirectoryException) {
                    call.respond(HttpStatusCode.BadRequest, "Target is not a directory.")
                } catch (e: Throwable) {
                    if (e.isMultipartLimitError()) {
                        call.respond(
                            HttpStatusCode.PayloadTooLarge,
                            "Upload payload exceeds the server's configured multipart limit."
                        )
                    } else {
                        throw e
                    }
                }
            }

            get(ENDPOINT_LIST) {
                val target = call.request.queryParameters["target"]?.ifBlank { SLASH } ?: SLASH
                try {
                    val tree = FileService.listPath(target)
                    call.respond(tree)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid target path.")
                } catch (e: NoSuchFileException) {
                    call.respond(HttpStatusCode.NotFound, "Directory not found.")
                } catch (e: NotDirectoryException) {
                    call.respond(HttpStatusCode.BadRequest, "Target is not a directory.")
                }
            }

            get(ENDPOINT_DOWNLOAD) {
                try {
                    val filepath = call.request.queryParameters["filepath"] ?: ""
                    val path = FileService.getFullPath(filepath)
                    if (Files.notExists(path)) {
                        call.respond(HttpStatusCode.NotFound, "File not found.")
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
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid file path.")
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to download file.", e)
                    call.respond(HttpStatusCode.InternalServerError, "Unable to download file.")
                }
            }
        }
    }
}

private fun Throwable.isMultipartLimitError(): Boolean {
    return message?.contains("exceeded while searching for", ignoreCase = true) == true
}
