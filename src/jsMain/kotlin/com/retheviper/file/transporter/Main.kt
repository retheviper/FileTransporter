package com.retheviper.file.transporter

import com.retheviper.file.transporter.content.FileBrowser
import kotlinx.coroutines.MainScope
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.renderComposable

private val scope = MainScope()

fun main() {

    renderComposable(rootElementId = "root") {
        Div({ style { padding(25.px) } }) {
            FileBrowser(scope)
        }
    }
}