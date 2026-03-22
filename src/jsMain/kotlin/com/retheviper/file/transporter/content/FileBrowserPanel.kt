package com.retheviper.file.transporter.content

import com.retheviper.file.transporter.model.PathItem
import com.retheviper.file.transporter.style.AppTheme
import com.retheviper.file.transporter.style.pointerCursor
import com.retheviper.file.transporter.util.FileInfoUtil
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun BrowserPanel(
    theme: AppTheme,
    selectedPathItems: List<PathItem>,
    isLoading: Boolean,
    browserError: String?,
    onItemSelected: (PathItem) -> Unit,
    onItemDownload: (PathItem) -> Unit
) {
    Div({
        style {
            panelStyle(theme)
            property("display", "grid")
            property("gap", "18px")
            property("align-content", "start")
        }
    }) {
        Div({
            style {
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
                    property("gap", "0")
                }
            }) {
                SectionEyebrow(theme, "Browser")
            }

            Div({
                style {
                    property("font-size", "13px")
                    property("color", theme.subtleText)
                }
            }) {
                val itemCount = selectedPathItems.size
                Text("$itemCount item" + if (itemCount == 1) "" else "s")
            }
        }

        browserError?.let {
            StatusMessage(
                theme = theme,
                message = it,
                tone = "error"
            )
        }

        when {
            isLoading -> {
                StatusMessage(
                    theme = theme,
                    message = "Loading current directory...",
                    tone = "neutral"
                )
            }

            selectedPathItems.isEmpty() -> {
                Div({
                    style {
                        property("padding", "42px 20px")
                        property("border", "1px dashed ${theme.emptyStateBorder}")
                        property("border-radius", "24px")
                        property("text-align", "center")
                        property("color", theme.subtleText)
                    }
                }) {
                    Text("This directory is empty.")
                }
            }

            else -> {
                Div({
                    style {
                        property("display", "grid")
                        property("gap", "12px")
                    }
                }) {
                    selectedPathItems.forEach { pathItem ->
                        FileItem(
                            theme = theme,
                            pathItem = pathItem,
                            onClick = { onItemSelected(pathItem) },
                            onDownloadClick = { onItemDownload(pathItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(
    theme: AppTheme,
    pathItem: PathItem,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val icon = if (pathItem.isDirectory) "📁" else FileInfoUtil.getIconByMimeType(pathItem.mimeType)
    val meta = if (pathItem.isDirectory) "Folder" else FileInfoUtil.formatFileSizeWithUnit(pathItem.size ?: 0)

    Div({
        style {
            pointerCursor()
            property("display", "grid")
            property("grid-template-columns", "auto minmax(0, 1fr) auto")
            property("gap", "16px")
            property("align-items", "center")
            property("padding", "16px 18px")
            property("border-radius", "20px")
            property("border", "1px solid ${theme.cardBorder}")
            property("background", theme.cardBackground)
        }
        onClick { onClick() }
    }) {
        Div({
            style {
                property("display", "grid")
                property("place-items", "center")
                property("width", "46px")
                property("height", "46px")
                property("border-radius", "16px")
                property("background", if (pathItem.isDirectory) "rgba(20, 184, 166, 0.16)" else "rgba(59, 130, 246, 0.16)")
                property("font-size", "20px")
            }
        }) {
            Text(icon)
        }

        Div({
            style {
                property("min-width", "0")
                property("display", "grid")
                property("gap", "4px")
            }
        }) {
            Div({
                style {
                    property("font-size", "16px")
                    property("font-weight", "600")
                    property("color", theme.headingText)
                    property("overflow", "hidden")
                    property("text-overflow", "ellipsis")
                    property("white-space", "nowrap")
                }
            }) {
                Text(pathItem.name)
            }
            Div({
                style {
                    property("font-size", "13px")
                    property("color", theme.subtleText)
                }
            }) {
                Text(meta)
            }
        }

        Button(attrs = {
            style {
                actionButtonStyle(theme, primary = false)
                property("padding", "10px 14px")
                property("font-size", "12px")
            }
            onClick {
                it.stopPropagation()
                onDownloadClick()
            }
        }) {
            Text("Download")
        }
    }
}

@Composable
internal fun StatusMessage(theme: AppTheme, message: String, tone: String) {
    val palette = when (tone) {
        "error" -> Triple(theme.statusErrorBackground, theme.statusErrorBorder, theme.statusErrorText)
        else -> Triple(theme.statusNeutralBackground, theme.statusNeutralBorder, theme.statusNeutralText)
    }

    Div({
        style {
            property("padding", "14px 16px")
            property("border-radius", "18px")
            property("background", palette.first)
            property("border", "1px solid ${palette.second}")
            property("color", palette.third)
            property("font-size", "14px")
        }
    }) {
        Text(message)
    }
}

@Composable
internal fun SectionEyebrow(theme: AppTheme, text: String) {
    Div({
        style {
            property("font-size", "11px")
            property("font-weight", "700")
            property("letter-spacing", "0.2em")
            property("text-transform", "uppercase")
            property("color", theme.accentText)
        }
    }) {
        Text(text)
    }
}
