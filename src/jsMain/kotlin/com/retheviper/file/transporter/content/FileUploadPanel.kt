package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.API_URL
import com.retheviper.file.transporter.constant.ApiRoutes
import com.retheviper.file.transporter.style.AppTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.ref
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.files.File
import org.w3c.xhr.FormData
import org.w3c.xhr.XMLHttpRequest

@Composable
internal fun FileUploadForm(
    theme: AppTheme,
    currentPath: String,
    uploadProgress: Int?,
    onUploadStarted: (String) -> Unit,
    onUploadProgress: (String, Int) -> Unit,
    onUploadFinished: (String) -> Unit,
    onUploadFailed: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var fileInput by remember { mutableStateOf<HTMLInputElement?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isDragActive by remember { mutableStateOf(false) }

    fun beginUpload(selectedFile: File?) {
        if (selectedFile == null || isUploading) {
            return
        }

        scope.launch {
            isUploading = true
            onUploadStarted(selectedFile.name)
            try {
                uploadFile(
                    file = selectedFile,
                    currentPath = currentPath,
                    onProgress = { percent ->
                        onUploadProgress(selectedFile.name, percent)
                    }
                )
                fileInput?.value = ""
                onUploadFinished(selectedFile.name)
            } catch (e: Throwable) {
                onUploadFailed(selectedFile.name, e.message ?: "File upload failed.")
            } finally {
                isUploading = false
                isDragActive = false
            }
        }
    }

    Div({
        style {
            panelStyle(theme)
            property("display", "grid")
            property("gap", "18px")
            property("align-content", "start")
        }
    }) {
        Div({
            style {
                property("display", "grid")
                property("gap", "8px")
            }
        }) {
            SectionEyebrow(theme, "Drop Zone")
        }

        Div({
            style {
                property("display", "grid")
                property("gap", "12px")
                property("padding", "18px")
                property("border", "1px solid ${theme.cardBorder}")
                property("border-radius", "22px")
                property("background", theme.cardBackground)
            }
        }) {
            Input(InputType.File) {
                name("file")
                style {
                    property("display", "none")
                }
                ref { element ->
                    fileInput = element
                    onDispose {
                        if (fileInput == element) {
                            fileInput = null
                        }
                    }
                }
                onChange {
                    beginUpload(fileInput?.files?.asList()?.firstOrNull())
                }
            }

            Div(attrs = {
                style {
                    property("display", "grid")
                    property("gap", "14px")
                    property("padding", "22px")
                    property("border-radius", "22px")
                    property("border", if (isDragActive) "1px solid ${theme.buttonPrimaryBorder}" else "1px dashed ${theme.dragBorder}")
                    property("background", if (isDragActive) "rgba(20, 184, 166, 0.10)" else theme.dragBackground)
                    property("transition", "border-color 180ms ease, background 180ms ease")
                    property("justify-items", "start")
                }
                onDragEnter {
                    it.preventDefault()
                    if (!isUploading) isDragActive = true
                }
                onDragOver {
                    it.preventDefault()
                    if (!isUploading) isDragActive = true
                }
                onDragLeave {
                    it.preventDefault()
                    isDragActive = false
                }
                onDrop {
                    it.preventDefault()
                    val droppedFile = it.dataTransfer?.files?.asList()?.firstOrNull()
                    beginUpload(droppedFile)
                }
            }) {
                Div({
                    style {
                        property("font-size", "14px")
                        property("line-height", "1.6")
                        property("color", theme.mutedText)
                    }
                }) {
                    Text(if (isDragActive) "Drop the file to upload now." else "Drag and drop a file here, or choose one manually.")
                }

                Button(attrs = {
                    style {
                        actionButtonStyle(theme, primary = true)
                    }
                    if (isUploading) disabled()
                    onClick {
                        fileInput?.click()
                    }
                }) {
                    Text(if (isUploading) "Uploading..." else "Choose and upload")
                }
            }

            if (uploadProgress != null) {
                UploadProgress(theme = theme, progress = uploadProgress)
            }
        }
    }
}

@Composable
private fun UploadProgress(theme: AppTheme, progress: Int) {
    Div({
        style {
            property("display", "grid")
            property("gap", "8px")
        }
    }) {
        Div({
            style {
                property("display", "flex")
                property("justify-content", "space-between")
                property("font-size", "12px")
                property("letter-spacing", "0.12em")
                property("text-transform", "uppercase")
                property("color", theme.subtleText)
            }
        }) {
            Text("Progress")
            Text("$progress%")
        }
        Div({
            style {
                property("height", "10px")
                property("border-radius", "999px")
                property("overflow", "hidden")
                property("background", theme.progressTrack)
                property("border", "1px solid ${theme.cardBorder}")
            }
        }) {
            Div({
                style {
                    property("width", "${progress}%")
                    property("height", "100%")
                    property("background", "linear-gradient(90deg, #14b8a6 0%, #38bdf8 100%)")
                    property("transition", "width 180ms ease")
                }
            })
        }
    }
}

private suspend fun uploadFile(
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
            val percent = ((event.loaded.toDouble() / event.total.toDouble()) * 100).toInt().coerceIn(0, 100)
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
