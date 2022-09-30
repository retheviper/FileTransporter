package com.retheviper.file_transporter.client

import com.retheviper.file_transporter.constant.API_URL
import com.retheviper.file_transporter.model.Clicked
import com.retheviper.file_transporter.model.Clicked.Companion.endpoint
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window

private val apiUrl ="${window.location.origin}$API_URL"

val jsonClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun sendClicked(number: Int) {
    jsonClient.post("$apiUrl$endpoint") {
        contentType(ContentType.Application.Json)
        setBody(Clicked(number))
    }
}