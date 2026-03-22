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

data class ClientResponse(
    val ok: Boolean,
    val status: Short,
    val statusText: String,
    val body: String
)

interface ClientDependencies {
    val apiUrl: String
    suspend fun fetch(url: String): ClientResponse
}

object WindowClientDependencies : ClientDependencies {
    override val apiUrl: String = API_URL

    override suspend fun fetch(url: String): ClientResponse {
        val response = window.fetch(url).await()
        return ClientResponse(
            ok = response.ok,
            status = response.status,
            statusText = response.statusText,
            body = response.text().await()
        )
    }
}

suspend fun listPathItem(
    target: String,
    dependencies: ClientDependencies = WindowClientDependencies
): List<PathItem> {
    val encodedTarget = encodeURIComponent(target)
    val response = dependencies.fetch("${dependencies.apiUrl}${ApiRoutes.LIST}?target=$encodedTarget")
    if (!response.ok) {
        error("Unable to load files: ${response.status} ${response.statusText}")
    }
    return json.decodeFromString(response.body)
}

private fun encodeURIComponent(value: String): String = js("encodeURIComponent(value)") as String
