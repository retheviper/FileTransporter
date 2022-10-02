package com.retheviper.file.transporter.service

import com.retheviper.file.transporter.constant.ROOT_DIRECTORY
import com.retheviper.file.transporter.model.FileTree
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.streams.toList

object FileService {
    suspend fun saveFile(multipart: MultiPartData) {
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    println("FormItem: ${part.name} = ${part.value}")
                }

                is PartData.FileItem -> {
                    withContext(Dispatchers.IO) {
                        Files.createTempFile("ktor", ".tmp")
                    }.apply {
                        println(this)
                        Files.copy(part.streamProvider(), this)
                        println("FileItem: ${part.originalFileName} = $this")
                    }
                }

                else -> {
                    println("Unknown part: $part")
                }
            }
            part.dispose()
        }
    }

    fun getFullPath(path: String): Path {
        return Path.of(ROOT_DIRECTORY, path)
    }

    suspend fun listFileTree(target: String): List<FileTree> {
        return withContext(Dispatchers.IO) {
            try {
                Files.list(getFullPath(target))
                    .toList()
                    .filter { !it.isHidden() }
                    .map { it.toFileTree() }
                    .sortedBy { it.name }
                    .sortedBy { it.type }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun Path.toFileTree(): FileTree {
        return FileTree(
            name = this.fileName.toString(),
            size = if (this.isDirectory()) null else this.fileSize(),
            type = if (this.isDirectory()) "directory" else "file",
            mimeType = if (this.isDirectory()) null else Files.probeContentType(this),
            path = this.parent.toString().substringAfter(ROOT_DIRECTORY)
        )
    }
}