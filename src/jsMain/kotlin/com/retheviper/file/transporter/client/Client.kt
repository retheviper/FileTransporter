package com.retheviper.file.transporter.client

import com.retheviper.file.transporter.constant.ApiRoutes
import com.retheviper.file.transporter.model.PathItem
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json

val API_URL = "${window.location.origin}${ApiRoutes.BASE_PATH}"

private val json = Json {
    ignoreUnknownKeys = true
}

suspend fun listPathItem(target: String): List<PathItem> {
    val encodedTarget = encodeURIComponent(target)
    val response = window.fetch("$API_URL${ApiRoutes.LIST}?target=$encodedTarget").await()
    if (!response.ok) {
        error("Unable to load files: ${response.status} ${response.statusText}")
    }
    val payload = response.text().await()
    return json.decodeFromString(payload)
}

private fun encodeURIComponent(value: String): String = js("encodeURIComponent(value)") as String
