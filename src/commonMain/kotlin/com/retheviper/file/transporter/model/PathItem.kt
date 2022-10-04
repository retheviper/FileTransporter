package com.retheviper.file.transporter.model

import kotlinx.serialization.Serializable

@Serializable
data class PathItem(
    val name: String,
    val type: String,
    val path: String,
    val mimeType: String? = null,
    val size: Long? = null
) {
    var isDirectory: Boolean = type == "directory"
}