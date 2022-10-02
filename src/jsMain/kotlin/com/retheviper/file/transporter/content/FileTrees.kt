package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.API_URL
import com.retheviper.file.transporter.client.getFileTree
import com.retheviper.file.transporter.model.FileTree
import com.retheviper.file.transporter.style.pointerCursor
import io.ktor.http.encodeURLParameter
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput

@Composable
fun FileTrees(scope: CoroutineScope) {
    var targetPath by remember { mutableStateOf("") }
    var selectedFileTree by remember { mutableStateOf(emptyList<FileTree>()) }

    TextInput {
        value(targetPath)
        onInput { event ->
            targetPath = event.value
            if (event.inputType == "enterKey") {
                scope.launch {
                    selectedFileTree = getFileTree(targetPath)
                }
            }
        }
    }

    Button(
        attrs = {
            onClick {
                scope.launch {
                    selectedFileTree = getFileTree(targetPath)
                }
            }
        }) {
        Text("Get list")
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
                    targetPath = previousPath(targetPath)
                    selectedFileTree = getFileTree(targetPath)
                }
            }
            if (targetPath.isBlank()) hidden()
        }
    ) {
        Text("◀️ " + previousPath(targetPath))
    }

    selectedFileTree.forEach { fileTree ->
        Div(
            attrs = {
                style {
                    pointerCursor()
                }
                onClick {
                    val path = "${fileTree.path}/${fileTree.name}"
                    if (fileTree.isDirectory) {
                        scope.launch {
                            selectedFileTree = getFileTree(path)
                            targetPath = path
                        }
                    } else {
                        window.open(
                            "$API_URL/download?filepath=${path.encodeURLParameter()}",
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