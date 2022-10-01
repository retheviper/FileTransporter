package com.retheviper.file.transporter.plugins

import com.retheviper.file.transporter.constant.API_URL
import com.retheviper.file.transporter.model.Clicked
import com.retheviper.file.transporter.model.FileTree
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.streams.toList

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondRedirect("/index.html")
        }
        // Static plugin. Try to access `/static/index.html`
        static("/") {
            resources("/")
        }

        route(API_URL) {
            post(Clicked.endpoint) {
                val clicked = call.receive<Clicked>()
                println("Clicked: $clicked")
            }
            post("/upload") {
                println("called")
                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            println("FormItem: ${part.name} = ${part.value}")
                        }

                        is PartData.FileItem -> {
                            withContext(Dispatchers.IO) {
                                Files.createTempFile("ktor", ".tmp")
                            }.apply {
                                Files.copy(part.streamProvider(), this)
                                println("FileItem: ${part.originalFileName} = $this")
                            }
                        }

                        else -> {
                            println("Unknown part: $part")
                        }
                    }
                    part.dispose()
                }
                call.respondRedirect("/")
            }
            get("/download") {

            }

            get("/list") {
                val target = call.request.queryParameters["target"] ?: "/"

                val root = Path.of("/Users", "youngbinkim", target)

                fun Path.toFileTree(): FileTree {
                    return FileTree(
                        name = this.fileName.toString(),
                        size = if (this.isDirectory()) null else this.fileSize(),
                        type = if (this.isDirectory()) "directory" else "file",
                        children = if (this.isDirectory()) {
                            Files.list(this)
                                .filter { !it.isHidden() }
                                .map { it.toFileTree() }
                                .toList()
                        } else {
                            null
                        }
                    )
                }

                val files = withContext(Dispatchers.IO) {
                    Files.list(root)
                        .filter { !it.isHidden() }
                        .map { it.toFileTree() }
                        .toList()
                }

                call.respond(files)
            }
        }
    }
}
