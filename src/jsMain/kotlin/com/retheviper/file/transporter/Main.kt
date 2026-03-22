package com.retheviper.file.transporter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.retheviper.file.transporter.content.FileBrowser
import com.retheviper.file.transporter.style.darkAppTheme
import com.retheviper.file.transporter.style.lightAppTheme
import kotlinx.browser.localStorage
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        var isDarkMode by remember {
            mutableStateOf(localStorage.getItem(THEME_STORAGE_KEY) == "dark")
        }
        val theme = if (isDarkMode) darkAppTheme() else lightAppTheme()

        Div({
            style {
                property("min-height", "100vh")
                property("background", theme.rootBackground)
                property("color", theme.rootColor)
                property("font-family", "'Space Grotesk', 'Inter', 'Segoe UI', sans-serif")
                property("transition", "background 220ms ease, color 220ms ease")
            }
        }) {
            FileBrowser(
                theme = theme,
                onThemeToggle = {
                    val nextValue = !isDarkMode
                    isDarkMode = nextValue
                    localStorage.setItem(THEME_STORAGE_KEY, if (nextValue) "dark" else "light")
                }
            )
        }
    }
}

private const val THEME_STORAGE_KEY = "file-transporter-theme"
