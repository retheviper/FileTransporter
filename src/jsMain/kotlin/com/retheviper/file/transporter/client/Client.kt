package com.retheviper.file.transporter.client

import com.retheviper.file.transporter.constant.API_BASE_PATH
import com.retheviper.file.transporter.constant.ENDPOINT_LIST
import com.retheviper.file.transporter.constant.ENPOINT_UPLOAD
import com.retheviper.file.transporter.model.PathItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
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

suspend fun listPathItem(target: String): List<PathItem> {
    return jsonClient.get("$API_URL$ENDPOINT_LIST") {
        parameter("target", target)
        contentType(ContentType.Application.Json)
    }.body()
}

@OptIn(InternalAPI::class)
@Deprecated("Not working with File")
suspend fun upload(file: File) {
    val client = HttpClient(Js)

    client.submitFormWithBinaryData(
        url = "$API_URL$ENPOINT_UPLOAD",
        formData = formData {
            append(
                key = "file",
                value = file,
                headers = Headers.build {
                    append(name = HttpHeaders.ContentType, value = file.type)
                    append(name = HttpHeaders.ContentDisposition, value = "filename=${file.name.encodeURLParameter()}")
                }
            )
        }
    )
}