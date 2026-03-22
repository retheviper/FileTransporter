package com.retheviper

import com.retheviper.file.transporter.plugins.configureContent
import com.retheviper.file.transporter.plugins.configureRouting
import com.retheviper.file.transporter.plugins.configureSerialization
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerTest {

    @Test
    fun uploadReplacesExistingFileAndReturnsCreated() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            val targetDirectory = tempRoot.resolve("uploads").createDirectories()
            targetDirectory.resolve("hello.txt").writeText("old")

            application {
                configureSerialization()
                configureRouting()
                configureContent()
            }

            val response = client.post("/api/v1/upload") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("target", "/uploads")
                            append(
                                key = "file",
                                value = "new content".toByteArray(),
                                headers = io.ktor.http.Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        """form-data; name="file"; filename="hello.txt""""
                                    )
                                    append(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
                                }
                            )
                        }
                    )
                )
            }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("""{"uploaded":1}""", response.bodyAsText())
            assertEquals("new content", targetDirectory.resolve("hello.txt").readText())
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun listEndpointReturnsUploadedFile() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-list-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            val targetDirectory = tempRoot.resolve("uploads").createDirectories()
            targetDirectory.resolve("hello.txt").writeText("content")

            application {
                configureSerialization()
                configureRouting()
            }

            val response = client.get("/api/v1/list?target=/uploads") {
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("hello.txt"))
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }
}
