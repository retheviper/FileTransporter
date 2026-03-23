package com.retheviper.file.transporter.plugins

import com.retheviper.file.transporter.constant.ApiRoutes
import com.retheviper.file.transporter.constant.ROOT_PATH
import com.retheviper.file.transporter.service.FileStorageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(
    fileStorageService: FileStorageService,
    maxUploadFileSizeBytes: Long
) {
    routing {
        get {
            call.respondRedirect("/index.html")
        }
        staticResources(remotePath = ROOT_PATH, basePackage = ROOT_PATH)

        route(ApiRoutes.BASE_PATH) {
            post(ApiRoutes.UPLOAD) {
                val multipart = call.receiveMultipart(
                    formFieldLimit = maxUploadFileSizeBytes
                )
                val uploadCount = fileStorageService.saveFile(multipart)
                call.respond(HttpStatusCode.Created, mapOf("uploaded" to uploadCount))
            }

            get(ApiRoutes.LIST) {
                val target = call.request.queryParameters["target"]?.ifBlank { ROOT_PATH } ?: ROOT_PATH
                val tree = fileStorageService.listPath(target)
                call.respond(tree)
            }

            get(ApiRoutes.DOWNLOAD) {
                val filepath = call.request.queryParameters["filepath"] ?: ""
                val path = fileStorageService.prepareDownload(filepath)
                call.respondDownload(path)
            }
        }
    }
}

internal fun Throwable.isMultipartLimitError(): Boolean {
    return generateSequence(this) { it.cause }.any { throwable ->
        val errorMessage = throwable.message ?: return@any false
        errorMessage.contains("exceeded while searching for", ignoreCase = true) ||
            errorMessage.contains("content length exceeds limit", ignoreCase = true)
    }
}
