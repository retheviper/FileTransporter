package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.listPathItem
import com.retheviper.file.transporter.model.PathItem
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberFileBrowserState(): FileBrowserState {
    val scope = rememberCoroutineScope()
    return remember(scope) { FileBrowserState(scope) }
}

class FileBrowserState(
    private val scope: CoroutineScope
) {
    var currentPath by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(true)
        private set

    var pathItems by mutableStateOf(emptyList<PathItem>())
        private set

    var uploadError by mutableStateOf<String?>(null)
        private set

    var browserError by mutableStateOf<String?>(null)
        private set

    var uploadProgress by mutableStateOf<Int?>(null)
        private set

    var transferHistory by mutableStateOf(emptyList<TransferHistoryEntry>())
        private set

    suspend fun refresh() {
        isLoading = true
        browserError = null
        runCatching {
            listPathItem(currentPath).sortedWith(
                compareBy<PathItem>({ !it.isDirectory }, { it.name.lowercase() })
            )
        }.onSuccess { items ->
            pathItems = items
        }.onFailure { error ->
            browserError = error.message ?: "Unable to load files."
            pathItems = emptyList()
        }
        isLoading = false
    }

    fun navigateTo(path: String) {
        currentPath = path
    }

    fun download(item: PathItem, downloadUrl: String) {
        transferHistory = prependHistory(
            transferHistory,
            TransferHistoryEntry(
                type = "Download",
                fileName = item.name,
                location = formatDisplayPath(currentPath),
                detail = if (item.isDirectory) "ZIP archive" else item.size?.let { formatFileSize(it) } ?: "Ready",
                state = "Completed"
            )
        )
        window.open(downloadUrl, "_blank")
    }

    fun startUpload(fileName: String) {
        uploadError = null
        uploadProgress = 0
        transferHistory = prependHistory(
            transferHistory,
            TransferHistoryEntry(
                type = "Upload",
                fileName = fileName,
                location = formatDisplayPath(currentPath),
                detail = "Preparing transfer",
                state = "Running"
            )
        )
    }

    fun updateUploadProgress(fileName: String, percent: Int) {
        uploadProgress = percent
        transferHistory = updateLatestHistory(
            history = transferHistory,
            type = "Upload",
            fileName = fileName,
            location = formatDisplayPath(currentPath),
            state = "Running",
            detail = "$percent% uploaded"
        )
    }

    fun finishUpload(fileName: String) {
        uploadProgress = 100
        browserError = null
        transferHistory = updateLatestHistory(
            history = transferHistory,
            type = "Upload",
            fileName = fileName,
            location = formatDisplayPath(currentPath),
            state = "Completed",
            detail = "Upload complete"
        )
        scope.launch {
            refresh()
        }
    }

    fun failUpload(fileName: String, message: String) {
        uploadProgress = null
        uploadError = message
        transferHistory = updateLatestHistory(
            history = transferHistory,
            type = "Upload",
            fileName = fileName,
            location = formatDisplayPath(currentPath),
            state = "Failed",
            detail = message
        )
    }
}

private fun formatFileSize(size: Long): String =
    com.retheviper.file.transporter.util.FileInfoUtil.formatFileSizeWithUnit(size)
