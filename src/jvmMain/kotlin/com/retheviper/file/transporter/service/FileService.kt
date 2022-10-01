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

    suspend fun getFileTree(root: Path): List<FileTree> {
        return withContext(Dispatchers.IO) {
            try {
                Files.list(root)
                    .filter { !it.isHidden() }
                    .map { it.toFileTree() }
                    .toList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun Path.toFileTree(): FileTree {
        return FileTree(
            name = this.fileName.toString(),
            size = this.fileSize(),
            type = if (this.isDirectory()) "directory" else "file",
            path = this.parent.toString().substringAfter(ROOT_DIRECTORY)
        )
    }
}