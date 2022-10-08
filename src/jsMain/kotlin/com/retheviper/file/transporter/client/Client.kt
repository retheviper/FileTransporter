package com.retheviper.file.transporter.client

import com.retheviper.file.transporter.constant.API_BASE_PATH
import com.retheviper.file.transporter.constant.ENDPOINT_LIST
import com.retheviper.file.transporter.model.PathItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window

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