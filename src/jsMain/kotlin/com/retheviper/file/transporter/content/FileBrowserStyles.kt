package com.retheviper.file.transporter.content

import com.retheviper.file.transporter.style.AppTheme
import com.retheviper.file.transporter.style.pointerCursor
import org.jetbrains.compose.web.css.StyleScope

internal fun formatDisplayPath(path: String): String = if (path.isBlank()) "Root /" else path

internal fun encodeURIComponent(value: String): String = js("encodeURIComponent(value)") as String

internal fun StyleScope.panelStyle(theme: AppTheme) {
    property("padding", "26px")
    property("border-radius", "28px")
    property("border", "1px solid ${theme.panelBorder}")
    property("background", theme.panelBackground)
    property("box-shadow", theme.panelShadow)
    property("backdrop-filter", "blur(18px)")
}

internal fun StyleScope.actionButtonStyle(theme: AppTheme, primary: Boolean) {
    pointerCursor()
    property("display", "inline-flex")
    property("align-items", "center")
    property("gap", "10px")
    property("padding", "13px 18px")
    property("border-radius", "999px")
    property("border", if (primary) "1px solid ${theme.buttonPrimaryBorder}" else "1px solid ${theme.buttonSecondaryBorder}")
    property("background", if (primary) theme.buttonPrimaryBackground else theme.buttonSecondaryBackground)
    property("color", if (primary) theme.buttonText else theme.headingText)
    property("font-size", "13px")
    property("font-weight", "700")
    property("letter-spacing", "0.08em")
    property("text-transform", "uppercase")
}
