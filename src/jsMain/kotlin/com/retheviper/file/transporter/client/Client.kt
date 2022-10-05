package com.retheviper.file.transporter.client

import com.retheviper.file.transporter.constant.API_BASE_PATH
import com.retheviper.file.transporter.constant.ENDPOINT_LIST
import com.retheviper.file.transporter.constant.ENPOINT_UPLOAD
import com.retheviper.file.transporter.model.PathItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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
@Deprecated("Prints error message to console 'Unknown form content type: [object File]'")
suspend fun uploadFile(path: String, file: File) {
    val client = HttpClient()

    client.post("$API_URL$ENPOINT_UPLOAD") {
        this.headers {
            append(HttpHeaders.ContentType, ContentType.MultiPart.FormData.toString())
            append(
                name = HttpHeaders.ContentDisposition,
                value = "filename=${file.name.encodeURLParameter()}"
            )
        }
        setBody(
            MultiPartFormDataContent(
                formData {
                    append("target", path)
                    append("file", file)
                },
                boundary = "WebAppBoundary"
            )
        )
        onUpload { bytesSentTotal, contentLength ->
            println("Sent $bytesSentTotal bytes from $contentLength")
        }
    }
}