package com.retheviper.file_transporter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retheviper.file_transporter.client.sendClicked
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

private val scope = MainScope()

fun main() {
    var count: Int by mutableStateOf(0)


    renderComposable(rootElementId = "root") {
        Div({ style { padding(25.px) } }) {
            Button(attrs = {
                onClick {
                    count -= 1
                    scope.launch {
                        sendClicked(count)
                    }
                }
            }) {
                Text("-")
            }

            Span({ style { padding(15.px) } }) {
                Text("$count")
            }

            Button(attrs = {
                onClick {
                    count += 1
                    scope.launch {
                        sendClicked(count)
                    }
                }
            }) {
                Text("+")
            }
        }
    }
}