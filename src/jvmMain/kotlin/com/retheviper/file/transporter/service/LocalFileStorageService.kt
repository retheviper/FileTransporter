package com.retheviper.file.transporter.service

import com.retheviper.file.transporter.constant.FileType
import com.retheviper.file.transporter.model.PathItem
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.readTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.streams.toList

interface FileStorageService {
    suspend fun saveFile(multipart: MultiPartData): Int
    suspend fun listPath(target: String): List<PathItem>
    fun prepareDownload(path: String): Path
    fun resolvePath(path: String): Path
}

class LocalFileStorageService(
    private val rootDirectory: Path
) : FileStorageService {
    private val normalizedRootDirectory = rootDirectory.toAbsolutePath().normalize()
    private val realRootDirectory = normalizedRootDirectory.toRealPath()

    override suspend fun saveFile(multipart: MultiPartData): Int {
        val pendingUploads = mutableListOf<PendingUpload>()
        var targetPathValue: String? = null

        try {
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "target") {
                            targetPathValue = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        withContext(Dispatchers.IO) {
                            val filename = part.originalFileName?.substringAfterLast("/")?.substringAfterLast("\\")
                            if (!filename.isNullOrBlank()) {
                                val temporaryPath = Files.createTempFile("file-transporter-upload-", ".tmp")
                                Files.newOutputStream(temporaryPath).use { output ->
                                    part.provider().readTo(output.asSink())
                                }
                                pendingUploads += PendingUpload(
                                    filename = filename,
                                    temporaryPath = temporaryPath
                                )
                            }
                        }
                    }

                    else -> Unit
                }
                part.dispose()
            }

            val destinationDirectory = resolvePath(targetPathValue.orEmpty())
            requireDirectory(destinationDirectory) { _ ->
                TargetDirectoryNotFoundException("Target directory not found.")
            }

            withContext(Dispatchers.IO) {
                pendingUploads.forEach { upload ->
                    Files.move(
                        upload.temporaryPath,
                        destinationDirectory.resolve(upload.filename),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
            return pendingUploads.size
        } finally {
            pendingUploads.forEach { upload ->
                Files.deleteIfExists(upload.temporaryPath)
            }
        }
    }

    override fun resolvePath(path: String): Path {
        val candidate = normalizedRootDirectory.resolve(path.removePrefix("/")).normalize()
        val confinedCandidate = when {
            Files.exists(candidate, LinkOption.NOFOLLOW_LINKS) -> candidate.toRealPath()
            else -> {
                val realParent = candidate.parent?.toRealPath() ?: realRootDirectory
                realParent.resolve(candidate.fileName ?: Path.of("")).normalize()
            }
        }
        if (!confinedCandidate.startsWith(realRootDirectory)) {
            throw InvalidPathException("Invalid target path")
        }
        return candidate
    }

    override suspend fun listPath(target: String): List<PathItem> {
        return withContext(Dispatchers.IO) {
            val directory = resolvePath(target)
            requireDirectory(directory) { _ ->
                DirectoryNotFoundException("Directory not found.")
            }
            Files.list(directory).use { stream ->
                stream
                    .toList()
                    .filter { !it.isHidden() }
                    .map { it.toPathItem() }
                    .sortedWith(compareBy({ it.type }, { it.name.lowercase() }))
            }
        }
    }

    override fun prepareDownload(path: String): Path {
        val resolvedPath = resolvePath(path)
        if (Files.notExists(resolvedPath)) {
            throw FileNotFoundException("File not found.")
        }
        return resolvedPath
    }

    private fun Path.toPathItem(): PathItem {
        val parentPath = parent?.toString()?.substringAfter(rootDirectory.toString()).orEmpty()
        return PathItem(
            name = fileName.toString(),
            size = if (isDirectory()) null else fileSize(),
            type = if (isDirectory()) FileType.DIRECTORY else FileType.FILE,
            mimeType = if (isDirectory()) null else Files.probeContentType(this),
            path = parentPath
        )
    }

    private fun requireDirectory(
        path: Path,
        createNotFoundException: (Path) -> FileStorageException
    ) {
        if (Files.notExists(path)) {
            throw createNotFoundException(path)
        }
        if (!Files.isDirectory(path)) {
            throw TargetNotDirectoryException("Target is not a directory.")
        }
    }

    private data class PendingUpload(
        val filename: String,
        val temporaryPath: Path
    )
}
