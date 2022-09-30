package com.retheviper.file.transporter.plugins

import com.retheviper.file.transporter.constant.API_URL
import com.retheviper.file.transporter.model.Clicked
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.io.File

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
                    println(part.name)
                    if (part is PartData.FormItem) {
                        println("Form item: ${part.name} = ${part.value}")
                    }
                    if (part is PartData.FileItem) {
                        println("fileName:" + part.originalFileName)
                        val file = File("upload/${part.originalFileName}")
//                        part.streamProvider()
//                            .use { its -> file.outputStream().buffered().use { its.copyTo(it) } }
                    }
                    part.dispose()
                }
                call.respondRedirect("/")
            }
            get("/download") {

            }
        }
    }
}
