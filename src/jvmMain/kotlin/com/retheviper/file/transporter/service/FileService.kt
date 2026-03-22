package com.retheviper.file.transporter.service

import com.retheviper.file.transporter.constant.FileType
import com.retheviper.file.transporter.config.AppConfig
import com.retheviper.file.transporter.model.PathItem
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.readTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.streams.toList

object FileService {

    suspend fun saveFile(multipart: MultiPartData): Int {
        var path = getFullPath("")
        var uploadCount = 0
        var pendingFailure: Exception? = null
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    if (part.name == "target") {
                        pendingFailure = runCatching {
                            getFullPath(part.value).also(::requireDirectory)
                        }.onSuccess { validatedPath ->
                            path = validatedPath
                        }.exceptionOrNull() as? Exception
                    }
                }

                is PartData.FileItem -> {
                    withContext(Dispatchers.IO) {
                        val filename = part.originalFileName?.substringAfterLast("/")?.substringAfterLast("\\")
                        if (pendingFailure == null && !filename.isNullOrBlank()) {
                            requireDirectory(path)
                            Files.createDirectories(path)
                            val file = path.resolve(filename)
                            Files.newOutputStream(file).use { output ->
                                part.provider().readTo(output.asSink())
                            }
                            uploadCount += 1
                        }
                    }
                }

                else -> {
                    println("Unknown part: $part")
                }
            }
            part.dispose()
        }
        pendingFailure?.let { throw it }
        return uploadCount
    }

    fun getFullPath(path: String): Path {
        val root = rootDirectory()
        val candidate = root.resolve(path.removePrefix("/")).normalize()
        require(candidate.startsWith(root)) { "Invalid target path" }
        return candidate
    }

    private fun rootDirectory(): Path {
        return AppConfig.settings().rootDirectory
    }

    suspend fun listPath(target: String): List<PathItem> {
        return withContext(Dispatchers.IO) {
            val directory = getFullPath(target)
            requireDirectory(directory)
            Files.list(directory).use { stream ->
                stream
                    .toList()
                    .filter { !it.isHidden() }
                    .map { it.toPathItem() }
                    .sortedWith(compareBy({ it.type }, { it.name.lowercase() }))
            }
        }
    }

    private fun Path.toPathItem(): PathItem {
        return PathItem(
            name = this.fileName.toString(),
            size = if (this.isDirectory()) null else this.fileSize(),
            type = if (this.isDirectory()) FileType.DIRECTORY else FileType.FILE,
            mimeType = if (this.isDirectory()) null else Files.probeContentType(this),
            path = this.parent.toString().substringAfter(rootDirectory().toString())
        )
    }

    private fun requireDirectory(path: Path) {
        if (Files.notExists(path)) {
            throw NoSuchFileException(path.toString())
        }
        if (!Files.isDirectory(path)) {
            throw NotDirectoryException(path.toString())
        }
    }
}
