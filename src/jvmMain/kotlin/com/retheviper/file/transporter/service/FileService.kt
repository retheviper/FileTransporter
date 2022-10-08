package com.retheviper.file.transporter.service

import com.retheviper.file.transporter.constant.FileType
import com.retheviper.file.transporter.constant.ROOT_DIRECTORY
import com.retheviper.file.transporter.model.PathItem
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
        var path = Path.of(ROOT_DIRECTORY)
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    if (part.name == "target") {
                        path = getFullPath(part.value)
                    }
                }

                is PartData.FileItem -> {
                    withContext(Dispatchers.IO) {
                        val file = Files.createFile(path.resolve(part.originalFileName!!))
                        part.streamProvider().use { input ->
                            Files.newOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
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
            path = this.parent.toString().substringAfter(ROOT_DIRECTORY)
        )
    }
}