package com.retheviper.file.transporter.content

import com.retheviper.file.transporter.model.PathItem
import com.retheviper.file.transporter.style.AppTheme
import com.retheviper.file.transporter.util.FileInfoUtil
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun FileDetailsModal(
    theme: AppTheme,
    item: PathItem,
    onClose: () -> Unit,
    onDownload: () -> Unit
) {
    val details = listOf(
        "Type" to FileInfoUtil.guessTypeLabel(item.mimeType),
        "Path" to "${item.path}/${item.name}".replace("//", "/"),
        "Size" to item.size?.let(FileInfoUtil::formatFileSizeWithUnit).orEmpty().ifBlank { "Unavailable" },
        "MIME" to item.mimeType.orEmpty().ifBlank { "Unknown" }
    )

    Div({
        style {
            property("position", "fixed")
            property("inset", "0")
            property("display", "grid")
            property("place-items", "center")
            property("padding", "20px")
            property("background", theme.overlayBackground)
            property("backdrop-filter", "blur(12px)")
            property("z-index", "1000")
        }
        onClick { onClose() }
    }) {
        Div({
            style {
                panelStyle(theme)
                property("width", "min(560px, 100%)")
                property("display", "grid")
                property("gap", "16px")
            }
            onClick { it.stopPropagation() }
        }) {
            Div({
                style {
                    property("display", "flex")
                    property("justify-content", "space-between")
                    property("align-items", "start")
                    property("gap", "12px")
                }
            }) {
                Div({
                    style {
                        property("display", "grid")
                        property("gap", "6px")
                    }
                }) {
                    SectionEyebrow(theme, "File Info")
                    Div({
                        style {
                            property("font-size", "20px")
                            property("font-weight", "700")
                            property("color", theme.headingText)
                            property("word-break", "break-word")
                        }
                    }) {
                        Text(item.name)
                    }
                }

                Button(attrs = {
                    style {
                        actionButtonStyle(theme, primary = false)
                        property("padding", "10px 12px")
                    }
                    onClick { onClose() }
                }) {
                    Text("Close")
                }
            }

            Div({
                style {
                    property("display", "grid")
                    property("gap", "10px")
                }
            }) {
                details.forEach { (label, value) ->
                    Div({
                        style {
                            property("display", "grid")
                            property("gap", "4px")
                            property("padding", "12px 14px")
                            property("border-radius", "16px")
                            property("background", theme.softBackground)
                            property("border", "1px solid ${theme.cardBorder}")
                        }
                    }) {
                        Div({
                            style {
                                property("font-size", "11px")
                                property("font-weight", "700")
                                property("letter-spacing", "0.14em")
                                property("text-transform", "uppercase")
                                property("color", theme.subtleText)
                            }
                        }) {
                            Text(label)
                        }
                        Div({
                            style {
                                property("font-size", "14px")
                                property("color", theme.headingText)
                                property("word-break", "break-word")
                            }
                        }) {
                            Text(value)
                        }
                    }
                }
            }

            Button(attrs = {
                style {
                    actionButtonStyle(theme, primary = false)
                }
                onClick { onDownload() }
            }) {
                Text("Download")
            }
        }
    }
}
