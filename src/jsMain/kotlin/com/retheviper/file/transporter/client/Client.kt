package com.retheviper.file.transporter.client

import com.retheviper.file.transporter.constant.API_BASE_PATH
import com.retheviper.file.transporter.model.Clicked
import com.retheviper.file.transporter.model.Clicked.Companion.endpoint
import com.retheviper.file.transporter.model.FileTree
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.InternalAPI
import kotlinx.browser.window
import org.w3c.files.File

val API_URL = "${window.location.origin}$API_BASE_PATH"

val jsonClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun sendClicked(number: Int) {
    jsonClient.post("$API_URL$endpoint") {
        contentType(ContentType.Application.Json)
        setBody(Clicked(number))
    }
}

suspend fun getFileTree(target: String): List<FileTree> {
    return jsonClient.get("$API_URL/list") {
        parameter("target", target)
        contentType(ContentType.Application.Json)
    }.body()
}

@OptIn(InternalAPI::class)
suspend fun test(file: File) {
    val client = HttpClient(Js)

    val response: HttpResponse = client.submitFormWithBinaryData(
        url = "$API_URL/upload",
        formData = formData {
            append("file", file, Headers.build {
                append(HttpHeaders.ContentType, file.type)
                append(HttpHeaders.ContentDisposition, "filename=${file.name}")
            })
        }
    )

//    println(response.bodyAsText())
}