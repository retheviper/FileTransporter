package com.retheviper.file.transporter.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.model.PathItem

class FileTransferState {
    var uploadError by mutableStateOf<String?>(null)
        private set

    var uploadProgress by mutableStateOf<Int?>(null)
        private set

    var transferHistory by mutableStateOf(emptyList<TransferHistoryEntry>())
        private set

    fun recordDownload(item: PathItem, currentPath: String) {
        transferHistory = prependHistory(
            transferHistory,
            TransferHistoryEntry(
                type = "Download",
                fileName = item.name,
                location = formatDisplayPath(currentPath),
                detail = if (item.isDirectory) "ZIP archive" else item.size?.let(::formatFileSize) ?: "Ready",
                state = "Completed"
            )
        )
    }

    fun startUpload(fileName: String, currentPath: String) {
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

    fun updateUploadProgress(fileName: String, percent: Int, currentPath: String) {
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

    fun finishUpload(fileName: String, currentPath: String) {
        uploadProgress = 100
        transferHistory = updateLatestHistory(
            history = transferHistory,
            type = "Upload",
            fileName = fileName,
            location = formatDisplayPath(currentPath),
            state = "Completed",
            detail = "Upload complete"
        )
    }

    fun failUpload(fileName: String, message: String, currentPath: String) {
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
