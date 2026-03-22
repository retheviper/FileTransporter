package com.retheviper.file.transporter.content

import com.retheviper.file.transporter.constant.FileType
import com.retheviper.file.transporter.model.PathItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FileBrowserStateTest {

    private var scopeToCancel: CoroutineScope? = null

    @AfterTest
    fun cleanup() {
        scopeToCancel?.cancel()
        scopeToCancel = null
    }

    @Test
    fun refreshSortsDirectoriesBeforeFilesAndClearsErrors() = runTest {
        val dependencies = FakeFileBrowserStateDependencies(
            currentHashPath = "/downloads",
            listedItems = listOf(
                PathItem(name = "zeta.txt", type = FileType.FILE, path = "/downloads"),
                PathItem(name = "beta", type = FileType.DIRECTORY, path = "/downloads"),
                PathItem(name = "Alpha.txt", type = FileType.FILE, path = "/downloads"),
                PathItem(name = "omega", type = FileType.DIRECTORY, path = "/downloads")
            )
        )
        val state = createState(dependencies)

        state.refresh()

        assertEquals(listOf("beta", "omega", "Alpha.txt", "zeta.txt"), state.pathItems.map { it.name })
        assertNull(state.browserError)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun refreshStoresFailureMessageAndClearsItems() = runTest {
        val dependencies = FakeFileBrowserStateDependencies(
            currentHashPath = "/broken",
            failure = IllegalStateException("boom")
        )
        val state = createState(dependencies)

        state.refresh()

        assertEquals(emptyList(), state.pathItems)
        assertEquals("boom", state.browserError)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun navigateSyncDownloadAndUploadFlowsUpdateState() = runTest {
        val dependencies = FakeFileBrowserStateDependencies(currentHashPath = "")
        val state = createState(dependencies)

        state.navigateTo("/uploads")
        assertEquals("/uploads", state.currentPath)
        assertEquals("/uploads", dependencies.writtenPaths.single())

        dependencies.currentHashPath = "/synced"
        state.syncFromLocation()
        assertEquals("/synced", state.currentPath)

        state.download(
            item = PathItem(
                name = "archive",
                type = FileType.DIRECTORY,
                path = "/synced"
            ),
            downloadUrl = "/api/v1/download?filepath=/synced/archive"
        )
        assertEquals("/api/v1/download?filepath=/synced/archive", dependencies.openedDownloads.single())
        assertEquals("Download", state.transferHistory.first().type)
        assertEquals("ZIP archive", state.transferHistory.first().detail)

        state.startUpload("hello.txt")
        assertEquals(0, state.uploadProgress)
        assertEquals("Preparing transfer", state.transferHistory.first().detail)

        state.updateUploadProgress("hello.txt", 40)
        assertEquals(40, state.uploadProgress)
        assertEquals("40% uploaded", state.transferHistory.first().detail)

        state.failUpload("hello.txt", "Network error")
        assertNull(state.uploadProgress)
        assertEquals("Network error", state.uploadError)
        assertEquals("Failed", state.transferHistory.first().state)
    }

    @Test
    fun finishUploadTriggersRefreshAndMarksHistoryCompleted() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dependencies = FakeFileBrowserStateDependencies(
            currentHashPath = "/uploads",
            listedItems = listOf(
                PathItem(name = "fresh.txt", type = FileType.FILE, path = "/uploads", size = 5)
            )
        )
        val scope = CoroutineScope(dispatcher)
        scopeToCancel = scope
        val state = FileBrowserState(scope, dependencies)

        state.startUpload("fresh.txt")

        state.finishUpload("fresh.txt")
        advanceUntilIdle()

        assertEquals(100, state.uploadProgress)
        assertEquals("Completed", state.transferHistory.first().state)
        assertEquals("Upload complete", state.transferHistory.first().detail)
        assertEquals(listOf("fresh.txt"), state.pathItems.map { it.name })
        assertEquals(1, dependencies.listRequests)
        assertNull(state.browserError)
    }

    private fun createState(
        dependencies: FakeFileBrowserStateDependencies
    ): FileBrowserState {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scopeToCancel = scope
        return FileBrowserState(scope, dependencies)
    }
}

private class FakeFileBrowserStateDependencies(
    var currentHashPath: String,
    var listedItems: List<PathItem> = emptyList(),
    var failure: Throwable? = null
) : FileBrowserStateDependencies {
    val writtenPaths = mutableListOf<String>()
    val openedDownloads = mutableListOf<String>()
    var listRequests = 0

    override suspend fun listPathItems(path: String): List<PathItem> {
        listRequests += 1
        failure?.let { throw it }
        return listedItems
    }

    override fun readPathFromHash(): String = currentHashPath

    override fun writePathToHash(path: String) {
        writtenPaths += path
        currentHashPath = path
    }

    override fun openDownload(url: String) {
        openedDownloads += url
    }
}
