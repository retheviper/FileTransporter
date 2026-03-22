package com.retheviper.file.transporter.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClientTest {

    @Test
    fun listPathItemEncodesTargetAndParsesPayload() = kotlinx.coroutines.test.runTest {
        val dependencies = FakeClientDependencies(
            response = ClientResponse(
                ok = true,
                status = 200,
                statusText = "OK",
                body = """[{"name":"hello.txt","type":"FILE","path":"/uploads","size":5}]"""
            )
        )

        val items = listPathItem("/공유 폴더/report 1", dependencies)

        assertEquals("https://example.test/api/v1/list?target=%2F%EA%B3%B5%EC%9C%A0%20%ED%8F%B4%EB%8D%94%2Freport%201", dependencies.requests.single())
        assertEquals(listOf("hello.txt"), items.map { it.name })
        assertEquals(listOf("/uploads"), items.map { it.path })
    }

    @Test
    fun listPathItemThrowsHelpfulErrorOnFailureResponse() = kotlinx.coroutines.test.runTest {
        val dependencies = FakeClientDependencies(
            response = ClientResponse(
                ok = false,
                status = 404,
                statusText = "Not Found",
                body = ""
            )
        )

        val error = assertFailsWith<IllegalStateException> {
            listPathItem("/missing", dependencies)
        }

        assertEquals("Unable to load files: 404 Not Found", error.message)
    }
}

private class FakeClientDependencies(
    private val response: ClientResponse
) : ClientDependencies {
    override val apiUrl: String = "https://example.test/api/v1"
    val requests = mutableListOf<String>()

    override suspend fun fetch(url: String): ClientResponse {
        requests += url
        return response
    }
}
