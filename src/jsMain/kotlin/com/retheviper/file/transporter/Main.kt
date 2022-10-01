package com.retheviper.file.transporter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.sendClicked
import com.retheviper.file.transporter.client.test
import com.retheviper.file.transporter.content.FileTrees
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.accept
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.FileInput
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.files.File
import org.w3c.files.get

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

            repeat(2) {
                Br()
            }

            var checked by remember { mutableStateOf(false) }
            var file by remember { mutableStateOf<File?>(null) }

            Form(
                attrs = {
                    onSubmit {
                        if (file != null) {
                            scope.launch {
                                test(file!!)
                            }
                        }
                        it.preventDefault()
                    }
                }
            ) {
                FileInput {
                    accept("image/*")
                    onChange { event ->
                        file = event.target.files?.get(0)
                        println(file)
                    }
                }
                Input(InputType.Submit) {
//                    if (!checked) {
//                        disabled()
//                    }
                }
            }

            repeat(2) {
                Br()
            }

            FileTrees(scope)
        }
    }
}