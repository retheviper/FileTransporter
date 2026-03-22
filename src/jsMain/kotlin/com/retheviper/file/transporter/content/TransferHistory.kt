package com.retheviper.file.transporter.content

import com.retheviper.file.transporter.style.AppTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

data class TransferHistoryEntry(
    val type: String,
    val fileName: String,
    val location: String,
    val detail: String,
    val state: String,
    val timestamp: String = nowLabel()
)

fun prependHistory(
    history: List<TransferHistoryEntry>,
    entry: TransferHistoryEntry
): List<TransferHistoryEntry> = listOf(entry) + history.take(7)

fun updateLatestHistory(
    history: List<TransferHistoryEntry>,
    type: String,
    fileName: String,
    location: String,
    state: String,
    detail: String
): List<TransferHistoryEntry> {
    val index = history.indexOfFirst { it.type == type && it.fileName == fileName }
    if (index == -1) {
        return prependHistory(
            history,
            TransferHistoryEntry(
                type = type,
                fileName = fileName,
                location = location,
                detail = detail,
                state = state
            )
        )
    }

    return history.mapIndexed { currentIndex, entry ->
        if (currentIndex == index) {
            entry.copy(
                location = location,
                detail = detail,
                state = state,
                timestamp = nowLabel()
            )
        } else {
            entry
        }
    }
}

@Composable
internal fun TransferHistoryPanel(theme: AppTheme, history: List<TransferHistoryEntry>) {
    Div({
        style {
            panelStyle(theme)
            property("display", "grid")
            property("gap", "14px")
        }
    }) {
        Div({
            style {
                property("display", "grid")
                property("gap", "6px")
            }
        }) {
            SectionEyebrow(theme, "Recent Activity")
        }

        if (history.isEmpty()) {
            Div({
                style {
                    property("padding", "18px")
                    property("border-radius", "20px")
                    property("background", theme.softBackground)
                    property("color", theme.subtleText)
                    property("font-size", "14px")
                }
            }) {
                Text("Recent transfer activity will appear here.")
            }
        } else {
            Div({
                style {
                    property("display", "grid")
                    property("gap", "10px")
                }
            }) {
                history.forEach { entry ->
                    TransferHistoryItem(theme = theme, entry = entry)
                }
            }
        }
    }
}

@Composable
private fun TransferHistoryItem(theme: AppTheme, entry: TransferHistoryEntry) {
    val stateColor = when (entry.state) {
        "Failed" -> if (theme.isDark) "#fca5a5" else "#dc2626"
        "Running" -> if (theme.isDark) "#67e8f9" else "#0284c7"
        else -> if (theme.isDark) "#86efac" else "#15803d"
    }

    Div({
        style {
            property("display", "grid")
            property("gap", "8px")
            property("padding", "16px 18px")
            property("border-radius", "20px")
            property("background", theme.cardBackground)
            property("border", "1px solid ${theme.cardBorder}")
        }
    }) {
        Div({
            style {
                property("display", "flex")
                property("justify-content", "space-between")
                property("gap", "12px")
                property("align-items", "center")
                property("flex-wrap", "wrap")
            }
        }) {
            Div({
                style {
                    property("font-size", "15px")
                    property("font-weight", "600")
                    property("color", theme.headingText)
                }
            }) {
                Text("${entry.type}: ${entry.fileName}")
            }
            Div({
                style {
                    property("font-size", "11px")
                    property("font-weight", "700")
                    property("letter-spacing", "0.14em")
                    property("text-transform", "uppercase")
                    property("color", stateColor)
                }
            }) {
                Text(entry.state)
            }
        }
        Div({
            style {
                property("font-size", "13px")
                property("color", theme.subtleText)
            }
        }) {
            Text("${entry.location} • ${entry.detail}")
        }
        Div({
            style {
                property("font-size", "12px")
                property("color", theme.mutedText)
            }
        }) {
            Text(entry.timestamp)
        }
    }
}

private fun nowLabel(): String = js(
    "new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })"
) as String
