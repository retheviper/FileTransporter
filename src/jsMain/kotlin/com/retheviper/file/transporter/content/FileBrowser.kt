package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.FormEncType
import org.jetbrains.compose.web.attributes.FormMethod
import org.jetbrains.compose.web.attributes.FormTarget
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.encType
import org.jetbrains.compose.web.attributes.method
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.HiddenInput
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text

@Composable
fun FileBrowser(scope: CoroutineScope) {
    var currentPath by remember { mutableStateOf("") }
    var selectedPathItems by remember { mutableStateOf(emptyList<PathItem>()) }

    if (selectedPathItems.isEmpty()) {
        scope.launch {
            selectedPathItems = listPathItem(currentPath)
        }
    }

    Div {
        Text("Current: $currentPath")
    }

    FileUploadForm(currentPath)

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
                scope.launch {
                    selectedPathItems = listPathItem(targetPath)
                    currentPath = targetPath
                }
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
private fun FileUploadForm(currentPath: String) {
    Div {
        Form(
            action = "$API_URL$ENPOINT_UPLOAD",
            attrs = {
                method(FormMethod.Post)
                encType(FormEncType.MultipartFormData)
                target(FormTarget.Blank)
            }
        ) {
            HiddenInput {
                name("target")
                value(currentPath)
            }
            Input(InputType.File) { name("file") }
            Input(InputType.Submit)
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