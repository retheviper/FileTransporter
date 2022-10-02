package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.API_URL
import com.retheviper.file.transporter.client.listFileTree
import com.retheviper.file.transporter.model.FileTree
import com.retheviper.file.transporter.style.pointerCursor
import io.ktor.http.encodeURLParameter
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
fun FileTrees(scope: CoroutineScope) {
    var currentPath by remember { mutableStateOf("") }
    var selectedFileTree by remember { mutableStateOf(emptyList<FileTree>()) }

    scope.launch {
        selectedFileTree = listFileTree(currentPath)
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
        Text("◀️ " + previousPath(currentPath))
    }

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
                Text("📄 ${fileTree.name} (${calculateFileSize(fileTree.size)})")
            }
        }
    }
}


private fun previousPath(path: String): String {
    return path.substringBeforeLast("/").substringBeforeLast("\\")
}


private fun calculateFileSize(origin: Long?): String {
    val size = origin ?: 0
    return if (size < 1024) {
        "$size b"
    } else {
        val kb = size / 1024
        if (kb < 1024) {
            "$kb kb"
        } else {
            val mb = kb / 1024
            if (mb < 1024) {
                "$mb mb"
            } else {
                val gb = mb / 1024
                "$gb gb"
            }
        }
    }
}