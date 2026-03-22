package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.API_URL
import com.retheviper.file.transporter.constant.ApiRoutes
import com.retheviper.file.transporter.model.PathItem
import com.retheviper.file.transporter.style.AppTheme
import org.jetbrains.compose.web.dom.Div

@Composable
fun FileBrowser(
    theme: AppTheme,
    onThemeToggle: () -> Unit
) {
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
            HeaderSection(
                theme = theme,
                onThemeToggle = onThemeToggle
            )

            BreadcrumbSection(
                theme = theme,
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
                    theme = theme,
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
                        theme = theme,
                        currentPath = browserState.currentPath,
                        uploadProgress = browserState.uploadProgress,
                        onUploadStarted = browserState::startUpload,
                        onUploadProgress = browserState::updateUploadProgress,
                        onUploadFinished = browserState::finishUpload,
                        onUploadFailed = browserState::failUpload
                    )

                    browserState.uploadError?.let { message ->
                        StatusMessage(
                            theme = theme,
                            message = message,
                            tone = "error"
                        )
                    }

                    TransferHistoryPanel(
                        theme = theme,
                        history = browserState.transferHistory
                    )
                }
            }
        }
    }

    selectedFile?.let { item ->
        FileDetailsModal(
            theme = theme,
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
