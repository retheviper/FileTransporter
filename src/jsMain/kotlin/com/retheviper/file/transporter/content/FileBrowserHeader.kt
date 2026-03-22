package com.retheviper.file.transporter.content

import com.retheviper.file.transporter.style.AppTheme
import com.retheviper.file.transporter.style.pointerCursor
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun HeaderSection(
    theme: AppTheme,
    onThemeToggle: () -> Unit
) {
    Div({
        style {
            panelStyle(theme)
            property("display", "flex")
            property("justify-content", "space-between")
            property("align-items", "center")
            property("gap", "16px")
            property("flex-wrap", "wrap")
        }
    }) {
        Div({
            style {
                property("display", "grid")
                property("gap", "10px")
            }
        }) {
            SectionEyebrow(theme, "File Transporter")
            Div({
                style {
                    property("font-size", "clamp(28px, 4vw, 42px)")
                    property("font-weight", "700")
                    property("line-height", "1.05")
                    property("color", theme.headingText)
                }
            }) {
                Text("Faster browsing in a lighter workspace")
            }
        }

        Button(attrs = {
            style {
                actionButtonStyle(theme, primary = false)
                property("padding", "12px 16px")
                property("gap", "8px")
            }
            onClick { onThemeToggle() }
        }) {
            Text(if (theme.isDark) "☀" else "☾")
            Text("Dark mode")
        }
    }
}

@Composable
internal fun BreadcrumbSection(
    theme: AppTheme,
    currentPath: String,
    onNavigate: (String) -> Unit
) {
    Div({
        style {
            panelStyle(theme)
            property("display", "grid")
            property("gap", "14px")
        }
    }) {
        SectionEyebrow(theme, "Path")
        Div({
            style {
                property("display", "flex")
                property("gap", "10px")
                property("align-items", "center")
                property("flex-wrap", "wrap")
            }
        }) {
            breadcrumbItems(currentPath).forEachIndexed { index, item ->
                if (index > 0) {
                    Div({
                        style {
                            property("color", theme.subtleText)
                            property("font-size", "14px")
                        }
                    }) {
                        Text("/")
                    }
                }

                Button(attrs = {
                    style {
                        pointerCursor()
                        property("padding", "10px 14px")
                        property("border-radius", "999px")
                        property("border", "1px solid ${theme.cardBorder}")
                        property("background", if (item.path == currentPath) theme.breadcrumbActiveBackground else theme.breadcrumbInactiveBackground)
                        property("color", if (item.path == currentPath) theme.breadcrumbActiveText else theme.breadcrumbInactiveText)
                        property("font-size", "14px")
                        property("font-weight", "600")
                    }
                    onClick { onNavigate(item.path) }
                }) {
                    Text(item.label)
                }
            }
        }
    }
}

private data class BreadcrumbItem(
    val label: String,
    val path: String
)

private fun breadcrumbItems(path: String): List<BreadcrumbItem> {
    if (path.isBlank()) return listOf(BreadcrumbItem("Root", ""))

    val normalized = path.trim('/').split('/').filter { it.isNotBlank() }
    val items = mutableListOf(BreadcrumbItem("Root", ""))
    var current = ""
    normalized.forEach { segment ->
        current = "$current/$segment"
        items += BreadcrumbItem(segment, current)
    }
    return items
}
