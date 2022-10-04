package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.API_URL
import com.retheviper.file.transporter.client.listPathItem
import com.retheviper.file.transporter.constant.CONTENT_SIZE_UNIT
import com.retheviper.file.transporter.constant.ENDPOINT_DOWNLOAD
import com.retheviper.file.transporter.constant.ENPOINT_UPLOAD
import com.retheviper.file.transporter.constant.SLASH
import com.retheviper.file.transporter.model.PathItem
import com.retheviper.file.transporter.style.pointerCursor
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.FormEncType
import org.jetbrains.compose.web.attributes.FormMethod
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.encType
import org.jetbrains.compose.web.attributes.method
import org.jetbrains.compose.web.css.selectors.CSSSelector.PseudoClass.scope
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Form
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

    Br()

    // Problem: Upload multipart data sends empty file
    // https://stackoverflow.com/questions/73450329/compose-for-web-uploading-a-file-submitted-via-a-multipart-form

    Form(
        action = "$API_URL$ENPOINT_UPLOAD",
        attrs = {
            method(FormMethod.Post)
            encType(FormEncType.MultipartFormData)
        }
    ) {
        Input(InputType.File)
        Input(InputType.Submit)
    }

    Br()

    if (currentPath.isBlank()) {
        Br()
    } else {
        Div(
            {
                style {
                    pointerCursor()
                }
                onClick {
                    scope.launch {
                        currentPath = previousPath(currentPath)
                        selectedPathItems = listPathItem(currentPath)
                    }
                }
            }
        ) {
            Text("◀️ Return")
        }
    }


    selectedPathItems.forEach { pathItem ->
        Div(
            {
                style {
                    pointerCursor()
                }
                onClick {
                    val targetPath = "${pathItem.path}/${pathItem.name}"
                    if (pathItem.isDirectory) {
                        scope.launch {
                            selectedPathItems = listPathItem(targetPath)
                            currentPath = targetPath
                        }
                    } else {
                        window.open(
                            "$API_URL$ENDPOINT_DOWNLOAD?filepath=${targetPath.encodeURLParameter()}",
                            "_parent"
                        )
                    }
                }
            }
        ) {
            if (pathItem.isDirectory) {
                Text("📁 ${pathItem.name}")
            } else {
                val icon = getIconByMimeType(pathItem.mimeType)
                val size = calculateFileSize(pathItem.size)
                Text("$icon ${pathItem.name} ($size)")
            }
        }
    }
}

fun getIconByMimeType(mimeType: String?): String {
    if (mimeType == null) return "📄"
    return when (mimeType.substringBefore(SLASH)) {
        ContentType.Image.Any.contentType -> "🏞"
        ContentType.Video.Any.contentType -> "🎬"
        ContentType.Audio.Any.contentType -> "🎵"
        ContentType.Text.Any.contentType -> "🗓"
        ContentType.Application.Any.contentType -> "🖥"
        else -> "📄"
    }
}

private fun previousPath(path: String): String {
    return path.substringBeforeLast(SLASH).substringBeforeLast("\\")
}

private fun calculateFileSize(size: Long?): String {
    val byte = size ?: 0
    return if (byte < CONTENT_SIZE_UNIT) {
        "$byte byte"
    } else {
        val kb = byte / CONTENT_SIZE_UNIT
        if (kb < CONTENT_SIZE_UNIT) {
            "$kb kb"
        } else {
            val mb = kb / CONTENT_SIZE_UNIT
            if (mb < CONTENT_SIZE_UNIT) {
                "$mb mb"
            } else {
                val gb = mb / CONTENT_SIZE_UNIT
                "$gb gb"
            }
        }
    }
}