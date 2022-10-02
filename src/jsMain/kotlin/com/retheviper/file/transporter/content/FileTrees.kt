package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.API_URL
import com.retheviper.file.transporter.client.listFileTree
import com.retheviper.file.transporter.constant.CONTENT_SIZE_UNIT
import com.retheviper.file.transporter.model.FileTree
import com.retheviper.file.transporter.style.pointerCursor
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
fun FileTrees(scope: CoroutineScope) {
    var currentPath by remember { mutableStateOf("") }
    var selectedFileTree by remember { mutableStateOf(emptyList<FileTree>()) }

    scope.launch {
        selectedFileTree = listFileTree(currentPath)
    }

    Div {
        Text("Current: $currentPath")
    }

    repeat(2) {
        Br()
    }

    Div(
        attrs = {
            style {
                pointerCursor()
            }
            onClick {
                scope.launch {
                    currentPath = previousPath(currentPath)
                    selectedFileTree = listFileTree(currentPath)
                }
            }
            if (currentPath.isBlank()) hidden()
        }
    ) {
        Text("◀️ Return")
    }

    Br()

    selectedFileTree.forEach { fileTree ->
        Div(
            attrs = {
                style {
                    pointerCursor()
                }
                onClick {
                    val targetPath = "${fileTree.path}/${fileTree.name}"
                    if (fileTree.isDirectory) {
                        scope.launch {
                            selectedFileTree = listFileTree(targetPath)
                            currentPath = targetPath
                        }
                    } else {
                        window.open(
                            "$API_URL/download?filepath=${targetPath.encodeURLParameter()}",
                            "_parent"
                        )
                    }
                }
            }
        ) {
            if (fileTree.isDirectory) {
                Text("📁 ${fileTree.name}")
            } else {
                val icon = getIconByMimeType(fileTree.mimeType)
                val size = calculateFileSize(fileTree.size)
                Text("$icon ${fileTree.name} ($size)")
            }
        }
    }
}

fun getIconByMimeType(mimeType: String?): String {
    if (mimeType == null) return "📄"
    return when (ContentType.parse(mimeType).contentType) {
        "image" -> "🏞"
        "video" -> "🎬"
        "audio" -> "🎵"
        "text" -> "🗓"
        "application" -> "🖥"
        else -> "📄"
    }
}

private fun previousPath(path: String): String {
    return path.substringBeforeLast("/").substringBeforeLast("\\")
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