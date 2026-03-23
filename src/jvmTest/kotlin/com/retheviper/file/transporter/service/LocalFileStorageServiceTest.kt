package com.retheviper.file.transporter.service

import com.retheviper.file.transporter.constant.FileType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel
import java.nio.file.Files
import java.nio.file.FileSystemException
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalFileStorageServiceTest {

    @Test
    fun resolvePathRejectsTraversalOutsideRoot() {
        val tempRoot = Files.createTempDirectory("file-transporter-service-root")
        try {
            val service = LocalFileStorageService(tempRoot)

            val exception = assertFailsWith<InvalidPathException> {
                service.resolvePath("../../outside")
            }

            assertEquals("Invalid target path", exception.message)
        } finally {
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun listPathExcludesHiddenEntriesAndSortsDirectoriesFirst() {
        val tempRoot = Files.createTempDirectory("file-transporter-service-list")
        try {
            val directory = tempRoot.resolve("uploads").createDirectories()
            directory.resolve(".hidden.txt").writeText("hidden")
            directory.resolve("zeta.txt").writeText("zeta")
            directory.resolve("Alpha.txt").writeText("alpha")
            directory.resolve("beta").createDirectories()
            directory.resolve("Omega").createDirectories()

            val service = LocalFileStorageService(tempRoot)
            val items = kotlinx.coroutines.runBlocking {
                service.listPath("/uploads")
            }

            assertEquals(listOf("beta", "Omega", "Alpha.txt", "zeta.txt"), items.map { it.name })
            assertEquals(
                listOf(FileType.DIRECTORY, FileType.DIRECTORY, FileType.FILE, FileType.FILE),
                items.map { it.type }
            )
            assertEquals(listOf("/uploads", "/uploads", "/uploads", "/uploads"), items.map { it.path })
        } finally {
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun saveFileStoresBasenameOnlyAndCountsUploadedFiles() {
        val tempRoot = Files.createTempDirectory("file-transporter-service-save")
        try {
            val service = LocalFileStorageService(tempRoot)
            val target = tempRoot.resolve("uploads").createDirectories()

            val uploaded = runBlocking {
                service.saveFile(
                    TestMultiPartData(
                        listOf(
                            formItem("target", "/uploads"),
                            fileItem("nested/path/hello.txt", "hello"),
                            fileItem("nested\\\\windows.txt", "windows"),
                            fileItem("", "ignored")
                        )
                    )
                )
            }

            assertEquals(2, uploaded)
            assertEquals("hello", target.resolve("hello.txt").readText())
            assertEquals("windows", target.resolve("windows.txt").readText())
            assertEquals(false, target.resolve("nested").exists())
        } finally {
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun saveFileUsesRootDirectoryWhenTargetIsMissing() {
        val tempRoot = Files.createTempDirectory("file-transporter-service-default-target")
        try {
            val service = LocalFileStorageService(tempRoot)

            val uploaded = runBlocking {
                service.saveFile(
                    TestMultiPartData(
                        listOf(
                            fileItem("root.txt", "root content")
                        )
                    )
                )
            }

            assertEquals(1, uploaded)
            assertEquals("root content", tempRoot.resolve("root.txt").readText())
        } finally {
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun saveFileUsesTargetEvenWhenFilePartComesFirst() {
        val tempRoot = Files.createTempDirectory("file-transporter-service-out-of-order-target")
        try {
            val service = LocalFileStorageService(tempRoot)
            val target = tempRoot.resolve("uploads").createDirectories()

            val uploaded = runBlocking {
                service.saveFile(
                    TestMultiPartData(
                        listOf(
                            fileItem("hello.txt", "hello"),
                            formItem("target", "/uploads")
                        )
                    )
                )
            }

            assertEquals(1, uploaded)
            assertEquals("hello", target.resolve("hello.txt").readText())
            assertEquals(false, tempRoot.resolve("hello.txt").exists())
        } finally {
            tempRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun resolvePathRejectsSymlinkThatEscapesRoot() {
        val tempRoot = Files.createTempDirectory("file-transporter-service-symlink-root")
        val outsideDirectory = Files.createTempDirectory("file-transporter-service-symlink-outside")
        try {
            val linkedDirectory = tempRoot.resolve("linked")
            try {
                Files.createSymbolicLink(linkedDirectory, outsideDirectory)
            } catch (_: UnsupportedOperationException) {
                return
            } catch (_: FileSystemException) {
                return
            }

            val service = LocalFileStorageService(tempRoot)

            assertFailsWith<InvalidPathException> {
                service.resolvePath("/linked/file.txt")
            }
        } finally {
            tempRoot.toFile().deleteRecursively()
            outsideDirectory.toFile().deleteRecursively()
        }
    }
}

private class TestMultiPartData(
    private val parts: List<PartData>
) : MultiPartData {
    private val iterator = parts.iterator()

    override suspend fun readPart(): PartData? {
        return if (iterator.hasNext()) iterator.next() else null
    }
}

private fun formItem(name: String, value: String): PartData.FormItem {
    return PartData.FormItem(
        value = value,
        dispose = {},
        partHeaders = Headers.build {
            append(HttpHeaders.ContentDisposition, """form-data; name="$name"""")
        }
    )
}

private fun fileItem(filename: String, content: String): PartData.FileItem {
    return PartData.FileItem(
        provider = { ByteReadChannel(content.toByteArray()) },
        dispose = {},
        partHeaders = Headers.build {
            append(
                HttpHeaders.ContentDisposition,
                """form-data; name="file"; filename="$filename""""
            )
        }
    )
}
