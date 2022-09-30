package com.retheviper.file_transporter.plugins

import com.retheviper.file_transporter.constant.API_URL
import com.retheviper.file_transporter.model.Clicked
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.request.receive
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

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
        }
    }
}
