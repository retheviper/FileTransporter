package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.API_URL
import com.retheviper.file.transporter.constant.ApiRoutes
import com.retheviper.file.transporter.model.PathItem
import com.retheviper.file.transporter.style.pointerCursor
import com.retheviper.file.transporter.util.FileInfoUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.ref
import org.jetbrains.compose.web.css.StyleScope
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
fun FileBrowser() {
    val browserState = rememberFileBrowserState()
    var selectedFile by remember { mutableStateOf<PathItem?>(null) }

    LaunchedEffect(browserState.currentPath) {
        browserState.refresh()
        selectedFile = null
    }

    Div({
        style {
            property("min-height", "100vh")
            property("padding", "40px 20px 56px")
            property("box-sizing", "border-box")
        }
    }) {
        Div({
            style {
                property("max-width", "1120px")
                property("margin", "0 auto")
                property("display", "grid")
                property("gap", "20px")
            }
        }) {
            BreadcrumbSection(
                currentPath = browserState.currentPath,
                onNavigate = browserState::navigateTo
            )

            Div({
                style {
                    property("display", "grid")
                    property("grid-template-columns", "minmax(0, 1.15fr) minmax(320px, 0.85fr)")
                    property("gap", "20px")
                    property("align-items", "start")
                }
            }) {
                BrowserPanel(
                    currentPath = browserState.currentPath,
                    selectedPathItems = browserState.pathItems,
                    isLoading = browserState.isLoading,
                    browserError = browserState.browserError,
                    onItemSelected = { pathItem ->
                        val targetPath = "${pathItem.path}/${pathItem.name}"
                        if (pathItem.isDirectory) {
                            browserState.navigateTo(targetPath)
                        } else {
                            selectedFile = pathItem
                        }
                    },
                    onItemDownload = { pathItem ->
                        val targetPath = "${pathItem.path}/${pathItem.name}"
                        browserState.download(
                            item = pathItem,
                            downloadUrl = "$API_URL${ApiRoutes.DOWNLOAD}?filepath=${encodeURIComponent(targetPath)}"
                        )
                    }
                )

                Div({
                    style {
                        property("display", "grid")
                        property("gap", "14px")
                        property("align-content", "start")
                    }
                }) {
                    FileUploadForm(
                        currentPath = browserState.currentPath,
                        uploadProgress = browserState.uploadProgress,
                        onUploadStarted = browserState::startUpload,
                        onUploadProgress = browserState::updateUploadProgress,
                        onUploadFinished = browserState::finishUpload,
                        onUploadFailed = browserState::failUpload
                    )

                    browserState.uploadError?.let { message ->
                        StatusMessage(
                            message = message,
                            tone = "error"
                        )
                    }

                    TransferHistoryPanel(browserState.transferHistory)
                }
            }
        }
    }

    selectedFile?.let { item ->
        FileDetailsModal(
            item = item,
            onClose = { selectedFile = null },
            onDownload = {
                val targetPath = "${item.path}/${item.name}"
                browserState.download(
                    item = item,
                    downloadUrl = "$API_URL${ApiRoutes.DOWNLOAD}?filepath=${encodeURIComponent(targetPath)}"
                )
            }
        )
    }
}

@Composable
private fun FileUploadForm(
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
            panelStyle()
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
            SectionEyebrow("Drop Zone")
        }

        Div({
            style {
                property("display", "grid")
                property("gap", "12px")
                property("padding", "18px")
                property("border", "1px solid rgba(148, 163, 184, 0.16)")
                property("border-radius", "22px")
                property("background", "linear-gradient(180deg, rgba(15, 23, 42, 0.92) 0%, rgba(8, 15, 28, 0.86) 100%)")
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
                    property("border", if (isDragActive) "1px solid rgba(45, 212, 191, 0.42)" else "1px dashed rgba(148, 163, 184, 0.22)")
                    property("background", if (isDragActive) "rgba(20, 184, 166, 0.10)" else "rgba(15, 23, 42, 0.52)")
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
                        property("color", "rgba(226, 232, 240, 0.76)")
                    }
                }) {
                    Text(if (isDragActive) "Drop the file to upload now." else "Drag and drop a file here, or choose one manually.")
                }

                Button(attrs = {
                    style {
                        actionButtonStyle(primary = true)
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
                UploadProgress(progress = uploadProgress)
            }
        }
    }
}

@Composable
private fun UploadProgress(progress: Int) {
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
                property("color", "rgba(148, 163, 184, 0.82)")
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
                property("background", "rgba(15, 23, 42, 0.9)")
                property("border", "1px solid rgba(148, 163, 184, 0.14)")
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

@Composable
private fun TransferHistoryPanel(history: List<TransferHistoryEntry>) {
    Div({
        style {
            panelStyle()
            property("display", "grid")
            property("gap", "14px")
        }
    }) {
        Div({
            style {
                property("display", "grid")
                property("gap", "6px")
            }
        }) {
            SectionEyebrow("Recent Activity")
        }

        if (history.isEmpty()) {
            Div({
                style {
                    property("padding", "18px")
                    property("border-radius", "20px")
                    property("background", "rgba(15, 23, 42, 0.5)")
                    property("color", "rgba(148, 163, 184, 0.82)")
                    property("font-size", "14px")
                }
            }) {
                Text("Recent transfer activity will appear here.")
            }
        } else {
            Div({
                style {
                    property("display", "grid")
                    property("gap", "10px")
                }
            }) {
                history.forEach { entry ->
                    TransferHistoryItem(entry)
                }
            }
        }
    }
}

@Composable
private fun TransferHistoryItem(entry: TransferHistoryEntry) {
    val stateColor = when (entry.state) {
        "Failed" -> "#fca5a5"
        "Running" -> "#67e8f9"
        else -> "#86efac"
    }

    Div({
        style {
            property("display", "grid")
            property("gap", "8px")
            property("padding", "16px 18px")
            property("border-radius", "20px")
            property("background", "rgba(15, 23, 42, 0.55)")
            property("border", "1px solid rgba(148, 163, 184, 0.12)")
        }
    }) {
        Div({
            style {
                property("display", "flex")
                property("justify-content", "space-between")
                property("gap", "12px")
                property("align-items", "center")
                property("flex-wrap", "wrap")
            }
        }) {
            Div({
                style {
                    property("font-size", "15px")
                    property("font-weight", "600")
                    property("color", "#f8fafc")
                }
            }) {
                Text("${entry.type}: ${entry.fileName}")
            }
            Div({
                style {
                    property("font-size", "11px")
                    property("font-weight", "700")
                    property("letter-spacing", "0.14em")
                    property("text-transform", "uppercase")
                    property("color", stateColor)
                }
            }) {
                Text(entry.state)
            }
        }
        Div({
            style {
                property("font-size", "13px")
                property("color", "rgba(148, 163, 184, 0.82)")
            }
        }) {
            Text("${entry.location} • ${entry.detail}")
        }
        Div({
            style {
                property("font-size", "12px")
                property("color", "rgba(148, 163, 184, 0.65)")
            }
        }) {
            Text(entry.timestamp)
        }
    }
}

@Composable
private fun FileDetailsModal(
    item: PathItem,
    onClose: () -> Unit,
    onDownload: () -> Unit
) {
    val details = listOf(
        "Type" to FileInfoUtil.guessTypeLabel(item.mimeType),
        "Path" to "${item.path}/${item.name}".replace("//", "/"),
        "Size" to item.size?.let(FileInfoUtil::formatFileSizeWithUnit).orEmpty().ifBlank { "Unavailable" },
        "MIME" to item.mimeType.orEmpty().ifBlank { "Unknown" }
    )

    Div({
        style {
            property("position", "fixed")
            property("inset", "0")
            property("display", "grid")
            property("place-items", "center")
            property("padding", "20px")
            property("background", "rgba(2, 6, 23, 0.72)")
            property("backdrop-filter", "blur(12px)")
            property("z-index", "1000")
        }
        onClick { onClose() }
    }) {
        Div({
            style {
                panelStyle()
                property("width", "min(560px, 100%)")
                property("display", "grid")
                property("gap", "16px")
            }
            onClick { it.stopPropagation() }
        }) {
            Div({
                style {
                    property("display", "flex")
                    property("justify-content", "space-between")
                    property("align-items", "start")
                    property("gap", "12px")
                }
            }) {
                Div({
                    style {
                        property("display", "grid")
                        property("gap", "6px")
                    }
                }) {
                    SectionEyebrow("File Info")
                    Div({
                        style {
                            property("font-size", "20px")
                            property("font-weight", "700")
                            property("color", "#f8fafc")
                            property("word-break", "break-word")
                        }
                    }) {
                        Text(item.name)
                    }
                }

                Button(attrs = {
                    style {
                        actionButtonStyle(primary = false)
                        property("padding", "10px 12px")
                    }
                    onClick { onClose() }
                }) {
                    Text("Close")
                }
            }

            Div({
                style {
                    property("display", "grid")
                    property("gap", "10px")
                }
            }) {
                details.forEach { (label, value) ->
                    Div({
                        style {
                            property("display", "grid")
                            property("gap", "4px")
                            property("padding", "12px 14px")
                            property("border-radius", "16px")
                            property("background", "rgba(15, 23, 42, 0.5)")
                            property("border", "1px solid rgba(148, 163, 184, 0.12)")
                        }
                    }) {
                        Div({
                            style {
                                property("font-size", "11px")
                                property("font-weight", "700")
                                property("letter-spacing", "0.14em")
                                property("text-transform", "uppercase")
                                property("color", "rgba(148, 163, 184, 0.72)")
                            }
                        }) {
                            Text(label)
                        }
                        Div({
                            style {
                                property("font-size", "14px")
                                property("color", "#e2e8f0")
                                property("word-break", "break-word")
                            }
                        }) {
                            Text(value)
                        }
                    }
                }
            }

            Button(attrs = {
                style {
                    actionButtonStyle(primary = false)
                }
                onClick { onDownload() }
            }) {
                Text("Download")
            }
        }
    }
}

@Composable
private fun FileItem(
    pathItem: PathItem,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val icon = if (pathItem.isDirectory) "📁" else FileInfoUtil.getIconByMimeType(pathItem.mimeType)
    val meta = if (pathItem.isDirectory) "Folder" else FileInfoUtil.formatFileSizeWithUnit(pathItem.size ?: 0)

    Div({
        style {
            pointerCursor()
            property("display", "grid")
            property("grid-template-columns", "auto minmax(0, 1fr) auto")
            property("gap", "16px")
            property("align-items", "center")
            property("padding", "16px 18px")
            property("border-radius", "20px")
            property("border", "1px solid rgba(148, 163, 184, 0.12)")
            property("background", "rgba(15, 23, 42, 0.55)")
        }
        onClick { onClick() }
    }) {
        Div({
            style {
                property("display", "grid")
                property("place-items", "center")
                property("width", "46px")
                property("height", "46px")
                property("border-radius", "16px")
                property("background", if (pathItem.isDirectory) "rgba(20, 184, 166, 0.16)" else "rgba(59, 130, 246, 0.16)")
                property("font-size", "20px")
            }
        }) {
            Text(icon)
        }

        Div({
            style {
                property("min-width", "0")
                property("display", "grid")
                property("gap", "4px")
            }
        }) {
            Div({
                style {
                    property("font-size", "16px")
                    property("font-weight", "600")
                    property("color", "#f8fafc")
                    property("overflow", "hidden")
                    property("text-overflow", "ellipsis")
                    property("white-space", "nowrap")
                }
            }) {
                Text(pathItem.name)
            }
            Div({
                style {
                    property("font-size", "13px")
                    property("color", "rgba(148, 163, 184, 0.82)")
                }
            }) {
                Text(meta)
            }
        }

        Button(attrs = {
            style {
                actionButtonStyle(primary = false)
                property("padding", "10px 14px")
                property("font-size", "12px")
            }
            onClick {
                it.stopPropagation()
                onDownloadClick()
            }
        }) {
            Text("Download")
        }
    }
}

@Composable
private fun BrowserPanel(
    currentPath: String,
    selectedPathItems: List<PathItem>,
    isLoading: Boolean,
    browserError: String?,
    onItemSelected: (PathItem) -> Unit,
    onItemDownload: (PathItem) -> Unit
) {
    Div({
        style {
            panelStyle()
            property("display", "grid")
            property("gap", "18px")
            property("align-content", "start")
        }
    }) {
        Div({
            style {
                property("display", "flex")
                property("justify-content", "space-between")
                property("align-items", "center")
                property("gap", "16px")
                property("flex-wrap", "wrap")
            }
        }) {
            Div({
                style {
                    property("display", "grid")
                    property("gap", "0")
                }
            }) {
                SectionEyebrow("Browser")
            }

            Div({
                style {
                    property("font-size", "13px")
                    property("color", "rgba(148, 163, 184, 0.82)")
                }
            }) {
                val itemCount = selectedPathItems.size
                Text("$itemCount item" + if (itemCount == 1) "" else "s")
            }
        }

        browserError?.let {
            StatusMessage(
                message = it,
                tone = "error"
            )
        }

        when {
            isLoading -> {
                StatusMessage(
                    message = "Loading current directory...",
                    tone = "neutral"
                )
            }

            selectedPathItems.isEmpty() -> {
                Div({
                    style {
                        property("padding", "42px 20px")
                        property("border", "1px dashed rgba(148, 163, 184, 0.20)")
                        property("border-radius", "24px")
                        property("text-align", "center")
                        property("color", "rgba(148, 163, 184, 0.9)")
                    }
                }) {
                    Text("This directory is empty.")
                }
            }

            else -> {
                Div({
                    style {
                        property("display", "grid")
                        property("gap", "12px")
                    }
                }) {
                    selectedPathItems.forEach { pathItem ->
                        FileItem(
                            pathItem = pathItem,
                            onClick = { onItemSelected(pathItem) },
                            onDownloadClick = { onItemDownload(pathItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbSection(
    currentPath: String,
    onNavigate: (String) -> Unit
) {
    Div({
        style {
            panelStyle()
            property("display", "grid")
            property("gap", "14px")
        }
    }) {
        SectionEyebrow("Path")
        Div({
            style {
                property("display", "flex")
                property("gap", "10px")
                property("align-items", "center")
                property("flex-wrap", "wrap")
            }
        }) {
            breadcrumbItems(currentPath).forEachIndexed { index, item ->
                if (index > 0) {
                    Div({
                        style {
                            property("color", "rgba(148, 163, 184, 0.55)")
                            property("font-size", "14px")
                        }
                    }) {
                        Text("/")
                    }
                }

                Button(attrs = {
                    style {
                        pointerCursor()
                        property("padding", "10px 14px")
                        property("border-radius", "999px")
                        property("border", "1px solid rgba(148, 163, 184, 0.14)")
                        property("background", if (item.path == currentPath) "rgba(20, 184, 166, 0.14)" else "rgba(255, 255, 255, 0.04)")
                        property("color", if (item.path == currentPath) "#ccfbf1" else "#e2e8f0")
                        property("font-size", "14px")
                        property("font-weight", "600")
                    }
                    onClick { onNavigate(item.path) }
                }) {
                    Text(item.label)
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(message: String, tone: String) {
    val palette = when (tone) {
        "error" -> Triple("rgba(239, 68, 68, 0.12)", "rgba(248, 113, 113, 0.28)", "#fecaca")
        else -> Triple("rgba(148, 163, 184, 0.08)", "rgba(148, 163, 184, 0.16)", "#cbd5e1")
    }

    Div({
        style {
            property("padding", "14px 16px")
            property("border-radius", "18px")
            property("background", palette.first)
            property("border", "1px solid ${palette.second}")
            property("color", palette.third)
            property("font-size", "14px")
        }
    }) {
        Text(message)
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Div({
        style {
            property("font-size", "11px")
            property("font-weight", "700")
            property("letter-spacing", "0.2em")
            property("text-transform", "uppercase")
            property("color", "#5eead4")
        }
    }) {
        Text(text)
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

fun formatDisplayPath(path: String): String = if (path.isBlank()) "Root /" else path

private fun StyleScope.panelStyle() {
    property("padding", "26px")
    property("border-radius", "28px")
    property("border", "1px solid rgba(148, 163, 184, 0.14)")
    property("background", "linear-gradient(180deg, rgba(8, 15, 28, 0.90) 0%, rgba(8, 15, 28, 0.76) 100%)")
    property("box-shadow", "0 20px 50px rgba(2, 8, 23, 0.28)")
    property("backdrop-filter", "blur(18px)")
}

private fun StyleScope.actionButtonStyle(primary: Boolean) {
    pointerCursor()
    property("display", "inline-flex")
    property("align-items", "center")
    property("gap", "10px")
    property("padding", "13px 18px")
    property("border-radius", "999px")
    property("border", if (primary) "1px solid rgba(45, 212, 191, 0.42)" else "1px solid rgba(148, 163, 184, 0.18)")
    property("background", if (primary) "linear-gradient(135deg, #14b8a6 0%, #0f766e 100%)" else "rgba(255, 255, 255, 0.04)")
    property("color", "#f8fafc")
    property("font-size", "13px")
    property("font-weight", "700")
    property("letter-spacing", "0.08em")
    property("text-transform", "uppercase")
}

data class TransferHistoryEntry(
    val type: String,
    val fileName: String,
    val location: String,
    val detail: String,
    val state: String,
    val timestamp: String = nowLabel()
)

fun prependHistory(
    history: List<TransferHistoryEntry>,
    entry: TransferHistoryEntry
): List<TransferHistoryEntry> = listOf(entry) + history.take(7)

fun updateLatestHistory(
    history: List<TransferHistoryEntry>,
    type: String,
    fileName: String,
    location: String,
    state: String,
    detail: String
): List<TransferHistoryEntry> {
    val index = history.indexOfFirst { it.type == type && it.fileName == fileName }
    if (index == -1) {
        return prependHistory(
            history,
            TransferHistoryEntry(
                type = type,
                fileName = fileName,
                location = location,
                detail = detail,
                state = state
            )
        )
    }

    return history.mapIndexed { currentIndex, entry ->
        if (currentIndex == index) {
            entry.copy(
                location = location,
                detail = detail,
                state = state,
                timestamp = nowLabel()
            )
        } else {
            entry
        }
    }
}

private fun nowLabel(): String = js(
    "new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })"
) as String

private data class BreadcrumbItem(
    val label: String,
    val path: String
)

private fun breadcrumbItems(path: String): List<BreadcrumbItem> {
    if (path.isBlank()) return listOf(BreadcrumbItem("Root", ""))

    val normalized = path.trim('/').split('/').filter { it.isNotBlank() }
    val items = mutableListOf(BreadcrumbItem("Root", ""))
    var current = ""
    normalized.forEach { segment ->
        current = "$current/$segment"
        items += BreadcrumbItem(segment, current)
    }
    return items
}

private fun encodeURIComponent(value: String): String = js("encodeURIComponent(value)") as String
