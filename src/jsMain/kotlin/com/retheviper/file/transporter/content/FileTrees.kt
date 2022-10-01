package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.getFileTree
import com.retheviper.file.transporter.constant.API_URL
import com.retheviper.file.transporter.model.FileTree
import io.ktor.http.encodeURLParameter
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput

@Composable
fun FileTrees(scope: CoroutineScope) {
    var target by remember { mutableStateOf("") }
    var selectedFileTree by remember { mutableStateOf(emptyList<FileTree>()) }

    TextInput {
        value(target)
        onInput { event ->
            target = event.value
            if (event.inputType == "enterKey") {
                scope.launch {
                    selectedFileTree = getFileTree(target)
                }
            }
        }
    }

    Button(attrs = {
        onClick {
            scope.launch {
                selectedFileTree = getFileTree(target)
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
                cursor("pointer")
            }
            onClick {
                scope.launch {
                    target = target.substringBeforeLast("/").substringBeforeLast("\\")
                    selectedFileTree = getFileTree(target)
                }
            }
            if (target.isBlank()) hidden()
        }
    ) {
        Text("..")
    }

    selectedFileTree.forEach { fileTree ->
        Div(
            attrs = {
                style {
                    cursor("pointer")
                }
                onClick {
                    val path = "${fileTree.path}/${fileTree.name}"
                    if (fileTree.isDirectory) {
                        scope.launch {
                            selectedFileTree = getFileTree(path)
                            target = path
                        }
                    } else {
                        window.open("${window.location.origin}$API_URL/download?filepath=${path.encodeURLParameter()}", "_blank")
                    }
                }
            }
        ) {
            if (fileTree.isDirectory) {
                Text("📁 ")
            } else {
                Text("📄 ")
            }
            Text(fileTree.name)
            if (!fileTree.isDirectory) {
                Text(" (${fileTree.size?.div(1024)?.div(1024)} mb)")
            }
        }
    }
}