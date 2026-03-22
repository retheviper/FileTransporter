package com.retheviper

import com.retheviper.file.transporter.config.AppSettings
import com.retheviper.file.transporter.plugins.configureContent
import com.retheviper.file.transporter.plugins.configureRouting
import com.retheviper.file.transporter.plugins.configureSerialization
import com.retheviper.file.transporter.plugins.configureStatusPages
import com.retheviper.file.transporter.service.LocalFileStorageService
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream
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
                configureRoutingForTest(tempRoot)
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
                configureRoutingForTest(tempRoot)
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

    @Test
    fun listEndpointRejectsPathTraversal() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-invalid-list-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            application {
                configureSerialization()
                configureRoutingForTest(tempRoot)
            }

            val response = client.get("/api/v1/list?target=../../outside") {
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid target path"))
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun uploadRejectsFileTarget() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-upload-target-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            tempRoot.resolve("not-a-directory.txt").writeText("existing")

            application {
                configureSerialization()
                configureRoutingForTest(tempRoot)
                configureContent()
            }

            val response = client.post("/api/v1/upload") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("target", "/not-a-directory.txt")
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

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Target is not a directory.", response.bodyAsText())
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun downloadReturnsNotFoundForMissingFile() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-download-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            application {
                configureSerialization()
                configureRoutingForTest(tempRoot)
            }

            val response = client.get("/api/v1/download?filepath=/missing.txt")

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("File not found.", response.bodyAsText())
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun downloadRejectsPathTraversal() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-download-invalid-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            application {
                configureSerialization()
                configureRoutingForTest(tempRoot)
            }

            val response = client.get("/api/v1/download?filepath=../../outside.txt")

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Invalid target path", response.bodyAsText())
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun downloadReturnsZipForDirectory() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-download-directory-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            val targetDirectory = tempRoot.resolve("folder").createDirectories()
            targetDirectory.resolve("hello.txt").writeText("content")
            targetDirectory.resolve("nested").createDirectories().resolve("child.txt").writeText("nested content")

            application {
                configureSerialization()
                configureRoutingForTest(tempRoot)
            }

            val response = client.get("/api/v1/download?filepath=/folder")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("attachment; filename=folder.zip", response.headers[HttpHeaders.ContentDisposition])

            val entries = mutableMapOf<String, String>()
            ZipInputStream(response.bodyAsChannel().toInputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        entries[entry.name] = zip.readBytes().decodeToString()
                    }
                    entry = zip.nextEntry
                }
            }

            assertEquals("content", entries["hello.txt"])
            assertEquals("nested content", entries["nested/child.txt"])
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun downloadReturnsAttachmentHeaderForSingleFile() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-download-file-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            tempRoot.resolve("hello world.txt").writeText("content")

            application {
                configureSerialization()
                configureRoutingForTest(tempRoot)
                configureContent()
            }

            val response = client.get("/api/v1/download?filepath=/hello world.txt")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("attachment; filename=hello%20world.txt", response.headers[HttpHeaders.ContentDisposition])
            assertEquals("content", response.bodyAsText())
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun downloadReturnsEmptyZipForEmptyDirectory() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-download-empty-directory-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            tempRoot.resolve("empty folder").createDirectories()

            application {
                configureSerialization()
                configureRoutingForTest(tempRoot)
            }

            val response = client.get("/api/v1/download?filepath=/empty folder")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("attachment; filename=empty%20folder.zip", response.headers[HttpHeaders.ContentDisposition])

            ZipInputStream(response.bodyAsChannel().toInputStream()).use { zip ->
                assertEquals(null, zip.nextEntry)
            }
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun downloadUsesArchiveNameForRootDirectory() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-download-root-directory-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            tempRoot.resolve("hello.txt").writeText("content")

            application {
                configureSerialization()
                configureRoutingForTest(tempRoot)
            }

            val response = client.get("/api/v1/download?filepath=/")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                "attachment; filename=${tempRoot.fileName}.zip",
                response.headers[HttpHeaders.ContentDisposition]
            )
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun uploadReturnsNotFoundWhenTargetDirectoryDoesNotExist() = testApplication {
        val tempRoot = Files.createTempDirectory("file-transporter-upload-missing-target-test")
        System.setProperty("file.transporter.root", tempRoot.toString())
        try {
            application {
                configureSerialization()
                configureRoutingForTest(tempRoot)
                configureContent()
            }

            val response = client.post("/api/v1/upload") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("target", "/missing")
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

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("Target directory not found.", response.bodyAsText())
        } finally {
            System.clearProperty("file.transporter.root")
            tempRoot.toFile().deleteRecursively()
        }
    }

}

private fun io.ktor.server.application.Application.configureRoutingForTest(rootDirectory: java.nio.file.Path) {
    val settings = AppSettings(
        rootDirectory = rootDirectory,
        maxUploadFileSizeBytes = 1024L * 1024L
    )
    configureStatusPages()
    configureRouting(
        fileStorageService = LocalFileStorageService(settings.rootDirectory),
        maxUploadFileSizeBytes = settings.maxUploadFileSizeBytes
    )
}
