package com.retheviper.file.transporter.client

import com.retheviper.file.transporter.constant.ApiRoutes
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.w3c.files.File
import org.w3c.xhr.FormData
import org.w3c.xhr.XMLHttpRequest

interface FileUploadClient {
    suspend fun uploadFile(
        file: File,
        currentPath: String,
        onProgress: (Int) -> Unit
    )
}

object XmlHttpFileUploadClient : FileUploadClient {
    override suspend fun uploadFile(
        file: File,
        currentPath: String,
        onProgress: (Int) -> Unit
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val formData = FormData()
        formData.append("target", currentPath)
        formData.append("file", file, file.name)
        val xhr = XMLHttpRequest()

        xhr.open("POST", "$API_URL${ApiRoutes.UPLOAD}")
        xhr.upload.onprogress = { event ->
            if (event.lengthComputable) {
                val percent = ((event.loaded.toDouble() / event.total.toDouble()) * 100)
                    .toInt()
                    .coerceIn(0, 100)
                onProgress(percent)
            }
        }
        xhr.onload = {
            if (xhr.status.toInt() in 200..299) {
                onProgress(100)
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(
                    IllegalStateException("Upload failed with status ${xhr.status.toInt()}.")
                )
            }
        }
        xhr.onerror = {
            continuation.resumeWithException(IllegalStateException("File upload failed."))
        }
        continuation.invokeOnCancellation {
            xhr.abort()
        }
        xhr.send(formData)
    }
}
