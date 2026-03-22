package com.retheviper.file.transporter.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.window
import org.w3c.dom.events.Event

interface FileBrowserLocationDriver {
    fun readPathFromHash(): String
    fun writePathToHash(path: String)
}

object WindowFileBrowserLocationDriver : FileBrowserLocationDriver {
    override fun readPathFromHash(): String {
        val hash = window.location.hash.removePrefix("#")
        if (hash.isBlank()) return ""
        return decodeUriComponent(hash)
    }

    override fun writePathToHash(path: String) {
        val nextHash = if (path.isBlank()) "" else encodeUriComponent(path)
        val currentHash = window.location.hash.removePrefix("#")
        if (currentHash == nextHash) return
        window.location.hash = nextHash
    }
}

@Composable
fun BindFileBrowserLocation(onLocationChanged: () -> Unit) {
    DisposableEffect(onLocationChanged) {
        val listener: (Event) -> Unit = {
            onLocationChanged()
        }
        window.addEventListener("hashchange", listener)
        onDispose {
            window.removeEventListener("hashchange", listener)
        }
    }
}

fun readPathFromHash(): String {
    return WindowFileBrowserLocationDriver.readPathFromHash()
}

fun writePathToHash(path: String) {
    WindowFileBrowserLocationDriver.writePathToHash(path)
}

private fun encodeUriComponent(value: String): String = js("encodeURIComponent(value)") as String

private fun decodeUriComponent(value: String): String = js("decodeURIComponent(value)") as String
