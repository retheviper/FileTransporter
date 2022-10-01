package com.retheviper.file.transporter.plugins

import com.retheviper.file.transporter.constant.API_URL
import com.retheviper.file.transporter.model.Clicked
import com.retheviper.file.transporter.service.FileService
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
import java.nio.file.Path

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
                call.application.environment.log.info("[clicked] with request body: $clicked")
                call.respond("OK")
            }
            post("/upload") {
                call.application.environment.log.info("[upload]")
                val multipart = call.receiveMultipart()
                FileService.saveFile(multipart)
                call.respondRedirect("/")
            }
            get("/list") {
                val target = call.request.queryParameters["target"] ?: "/"
                call.application.environment.log.info("[list] with target: $target")
                val root = "/Users/youngbinkim"
                val path = Path.of(root, target)
                val tree = FileService.getFileTree(path)
                call.application.environment.log.info("[list] with response body: $tree")
                call.respond(tree)
            }
        }
    }
}
