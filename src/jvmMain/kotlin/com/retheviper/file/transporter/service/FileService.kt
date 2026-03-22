package com.retheviper.file.transporter.service

import com.retheviper.file.transporter.constant.FileType
import com.retheviper.file.transporter.constant.ROOT_DIRECTORY
import com.retheviper.file.transporter.model.PathItem
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.readTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.streams.toList

object FileService {

    suspend fun saveFile(multipart: MultiPartData): Int {
        var path = getFullPath("")
        var uploadCount = 0
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    if (part.name == "target") {
                        path = getFullPath(part.value)
                    }
                }

                is PartData.FileItem -> {
                    withContext(Dispatchers.IO) {
                        val filename = part.originalFileName?.substringAfterLast("/")?.substringAfterLast("\\")
                        if (!filename.isNullOrBlank()) {
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
        return uploadCount
    }

    fun getFullPath(path: String): Path {
        val root = rootDirectory()
        val candidate = root.resolve(path.removePrefix("/")).normalize()
        require(candidate.startsWith(root)) { "Invalid target path" }
        return candidate
    }

    private fun rootDirectory(): Path {
        return Path.of(System.getProperty("file.transporter.root", ROOT_DIRECTORY)).toAbsolutePath().normalize()
    }

    suspend fun listPath(target: String): List<PathItem> {
        return withContext(Dispatchers.IO) {
            try {
                Files.list(getFullPath(target))
                    .toList()
                    .filter { !it.isHidden() }
                    .map { it.toPathItem() }
                    .sortedBy { it.name }
                    .sortedBy { it.type }
            } catch (e: Exception) {
                emptyList()
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
}
