package com.retheviper.file.transporter

import com.retheviper.file.transporter.content.FileTrees
import kotlinx.coroutines.MainScope
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.renderComposable

private val scope = MainScope()

fun main() {

    renderComposable(rootElementId = "root") {
        Div({ style { padding(25.px) } }) {
//            var checked by remember { mutableStateOf(false) }
//            var file by remember { mutableStateOf<File?>(null) }
//
//            Form(
//                attrs = {
//                    onSubmit {
//                        if (file != null) {
//                            scope.launch {
//                                test(file!!)
//                            }
//                        }
//                        it.preventDefault()
//                    }
//                }
//            ) {
//                FileInput {
//                    accept("image/*")
//                    onChange { event ->
//                        file = event.target.files?.get(0)
//                        println(file)
//                    }
//                }
//                Input(InputType.Submit) {
//                    if (!checked) {
//                        disabled()
//                    }
//                }
//            }
//
//            repeat(2) {
//                Br()
//            }

            FileTrees(scope)
        }
    }
}