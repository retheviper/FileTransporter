package com.retheviper.file.transporter.plugins

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

suspend fun ApplicationCall.respondDownload(path: Path) {
    if (Files.isDirectory(path)) {
        respondDirectoryArchive(path)
    } else {
        respondSingleFile(path)
    }
}

private suspend fun ApplicationCall.respondDirectoryArchive(path: Path) {
    val archiveName = "${path.fileName?.toString().orEmpty().ifBlank { "archive" }}.zip"
    response.header(
        name = HttpHeaders.ContentDisposition,
        value = ContentDisposition.Attachment.withParameter(
            key = ContentDisposition.Parameters.FileName,
            value = archiveName.encodeURLPath()
        ).toString()
    )
    respondOutputStream(ContentType.Application.Zip) {
        ZipOutputStream(this).use { zip ->
            path.writeDirectoryToZip(zip)
        }
    }
}

private suspend fun ApplicationCall.respondSingleFile(path: Path) {
    response.header(
        name = HttpHeaders.ContentDisposition,
        value = ContentDisposition.Attachment.withParameter(
            key = ContentDisposition.Parameters.FileName,
            value = path.fileName.toString().encodeURLPath()
        ).toString()
    )
    respondFile(path.toFile())
}

private fun Path.writeDirectoryToZip(zip: ZipOutputStream) {
    Files.walk(this).use { stream ->
        stream.sorted().forEach { currentPath ->
            val entryName = relativize(currentPath).toString().replace("\\", "/")
            if (entryName.isBlank()) return@forEach

            if (Files.isDirectory(currentPath)) {
                zip.putNextEntry(ZipEntry("$entryName/"))
                zip.closeEntry()
            } else {
                zip.putNextEntry(ZipEntry(entryName))
                Files.newInputStream(currentPath).use { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }
        }
    }
}
