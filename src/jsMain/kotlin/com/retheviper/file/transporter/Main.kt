package com.retheviper.file.transporter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.client.sendClicked
import com.retheviper.file.transporter.constant.API_URL
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.FormEncType
import org.jetbrains.compose.web.attributes.FormMethod
import org.jetbrains.compose.web.attributes.accept
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.encType
import org.jetbrains.compose.web.attributes.method
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.FileInput
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.SubmitInput
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

            repeat(2) {
                Br()
            }

            var checked by remember { mutableStateOf(false) }
            var file by remember { mutableStateOf("") }

            Form(
                action = "${window.location.origin}$API_URL/upload",
                attrs = {
                    method(FormMethod.Post)
                    encType(FormEncType.MultipartFormData)
                    attr("filename", file)
                }
            ) {
                FileInput {
                    accept("image/*")
                    onChange {
                        checked = true
                        file = it.value
                    }
                }
                SubmitInput {
                    if (!checked) {
                        disabled()
                    }
                }
            }
        }
    }
}