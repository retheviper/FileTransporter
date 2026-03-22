package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.API_URL
import com.retheviper.file.transporter.client.listPathItem
import com.retheviper.file.transporter.constant.ENDPOINT_DOWNLOAD
import com.retheviper.file.transporter.constant.ENPOINT_UPLOAD
import com.retheviper.file.transporter.constant.SLASH
import com.retheviper.file.transporter.model.PathItem
import com.retheviper.file.transporter.style.pointerCursor
import com.retheviper.file.transporter.util.FileInfoUtil
import io.ktor.http.encodeURLParameter
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.ref
import org.jetbrains.compose.web.attributes.value
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.xhr.FormData

@Composable
fun FileBrowser(scope: CoroutineScope) {
    var currentPath by remember { mutableStateOf("") }
    var selectedPathItems by remember { mutableStateOf(emptyList<PathItem>()) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPath) {
        selectedPathItems = listPathItem(currentPath)
    }

    Div {
        Text("Current: $currentPath")
    }

    FileUploadForm(
        currentPath = currentPath,
        scope = scope,
        onUploaded = {
            uploadError = null
            selectedPathItems = listPathItem(currentPath)
        },
        onError = { message ->
            uploadError = message
        }
    )

    if (uploadError != null) {
        Div {
            Text(uploadError!!)
        }
    }

    if (currentPath.isBlank()) {
        Br()
    } else {
        BackButton {
            scope.launch {
                currentPath = previousPath(currentPath)
                selectedPathItems = listPathItem(currentPath)
            }
        }
    }

    selectedPathItems.forEach { pathItem ->
        FileItem(pathItem) {
            val targetPath = "${pathItem.path}/${pathItem.name}"
            if (pathItem.isDirectory) {
                currentPath = targetPath
            } else {
                window.open(
                    url = "$API_URL$ENDPOINT_DOWNLOAD?filepath=${targetPath.encodeURLParameter()}",
                    target = ATarget.Blank.targetStr
                )
            }
        }
    }
}

@Composable
private fun FileUploadForm(
    currentPath: String,
    scope: CoroutineScope,
    onUploaded: suspend () -> Unit,
    onError: (String) -> Unit
) {
    var fileInput by remember { mutableStateOf<HTMLInputElement?>(null) }

    Div {
        Input(InputType.File) {
            name("file")
            ref { element ->
                fileInput = element
                onDispose {
                    if (fileInput == element) {
                        fileInput = null
                    }
                }
            }
        }
        Input(InputType.Button) {
            value("Upload")
            onClick {
                val selectedFile = fileInput?.files?.item(0)
                if (selectedFile == null) {
                    onError("Select a file to upload.")
                    return@onClick
                }
                scope.launch {
                    try {
                        uploadFile(selectedFile, currentPath)
                        fileInput?.value = ""
                        onUploaded()
                    } catch (e: Throwable) {
                        onError(e.message ?: "File upload failed.")
                    }
                }
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Div(
        {
            style { pointerCursor() }
            onClick { onClick() }
        }
    ) {
        Text("◀️ Return")
    }
}

@Composable
private fun FileItem(pathItem: PathItem, onClick: () -> Unit) {
    Div(
        {
            style { pointerCursor() }
            onClick { onClick() }
        }
    ) {
        if (pathItem.isDirectory) {
            Text("📁 ${pathItem.name}")
        } else {
            val icon = FileInfoUtil.getIconByMimeType(pathItem.mimeType)
            val size = FileInfoUtil.formatFileSizeWithUnit(pathItem.size ?: 0)
            Text("$icon ${pathItem.name} ($size)")
        }
    }
}

private fun previousPath(path: String): String {
    return path.substringBeforeLast(SLASH).substringBeforeLast("\\")
}

private suspend fun uploadFile(file: File, currentPath: String) {
    val formData = FormData()
    formData.append("target", currentPath)
    formData.append("file", file, file.name)

    val response = window.fetch(
        input = "$API_URL$ENPOINT_UPLOAD",
        init = js("{ method: 'POST', body: formData }")
    ).await()

    check(response.ok) { "Upload failed with status ${response.status.toInt()}." }
}
