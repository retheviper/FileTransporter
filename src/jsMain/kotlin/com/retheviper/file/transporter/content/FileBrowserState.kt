package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.listPathItem
import com.retheviper.file.transporter.model.PathItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.retheviper.file.transporter.content.readPathFromHash as browserLocationReadPathFromHash
import com.retheviper.file.transporter.content.writePathToHash as browserLocationWritePathToHash

@Composable
fun rememberFileBrowserState(): FileBrowserState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { FileBrowserState(scope) }

    BindFileBrowserLocation(onLocationChanged = state::syncFromLocation)

    return state
}

interface FileBrowserStateDependencies {
    suspend fun listPathItems(path: String): List<PathItem>
    fun readPathFromHash(): String
    fun writePathToHash(path: String)
    fun openDownload(url: String)
}

object DefaultFileBrowserStateDependencies : FileBrowserStateDependencies {
    override suspend fun listPathItems(path: String): List<PathItem> = listPathItem(path)

    override fun readPathFromHash(): String = browserLocationReadPathFromHash()

    override fun writePathToHash(path: String) = browserLocationWritePathToHash(path)

    override fun openDownload(url: String) {
        kotlinx.browser.window.open(url, "_blank")
    }
}

class FileBrowserState(
    private val scope: CoroutineScope,
    private val dependencies: FileBrowserStateDependencies = DefaultFileBrowserStateDependencies,
    private val transferState: FileTransferState = FileTransferState()
) {
    var currentPath by mutableStateOf(dependencies.readPathFromHash())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var pathItems by mutableStateOf(emptyList<PathItem>())
        private set

    val uploadError: String?
        get() = transferState.uploadError

    var browserError by mutableStateOf<String?>(null)
        private set

    val uploadProgress: Int?
        get() = transferState.uploadProgress

    val transferHistory: List<TransferHistoryEntry>
        get() = transferState.transferHistory

    suspend fun refresh() {
        isLoading = true
        browserError = null
        runCatching {
            dependencies.listPathItems(currentPath).sortedWith(
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
        dependencies.writePathToHash(path)
    }

    fun syncFromLocation() {
        currentPath = dependencies.readPathFromHash()
    }

    fun download(item: PathItem, downloadUrl: String) {
        transferState.recordDownload(item, currentPath)
        dependencies.openDownload(downloadUrl)
    }

    fun startUpload(fileName: String) {
        transferState.startUpload(fileName, currentPath)
    }

    fun updateUploadProgress(fileName: String, percent: Int) {
        transferState.updateUploadProgress(fileName, percent, currentPath)
    }

    fun finishUpload(fileName: String) {
        browserError = null
        transferState.finishUpload(fileName, currentPath)
        scope.launch {
            refresh()
        }
    }

    fun failUpload(fileName: String, message: String) {
        transferState.failUpload(fileName, message, currentPath)
    }
}
