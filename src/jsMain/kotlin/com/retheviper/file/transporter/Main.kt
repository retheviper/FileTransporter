package com.retheviper.file.transporter

import com.retheviper.file.transporter.content.FileBrowser
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        Div({
            style {
                property("min-height", "100vh")
                property("background", "radial-gradient(circle at top left, rgba(20, 184, 166, 0.18), transparent 32%), radial-gradient(circle at top right, rgba(59, 130, 246, 0.16), transparent 28%), linear-gradient(180deg, #09111f 0%, #050811 100%)")
                property("color", "#f4f7fb")
                property("font-family", "'Space Grotesk', 'Inter', 'Segoe UI', sans-serif")
            }
        }) {
            FileBrowser()
        }
    }
}
