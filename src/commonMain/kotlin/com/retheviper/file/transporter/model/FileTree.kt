package com.retheviper.file.transporter.model

import kotlinx.serialization.Serializable

@Serializable
data class FileTree(
    val name: String,
    val type: String,
    val path: String,
    val size: Long? = null
) {
    var isDirectory: Boolean = type == "directory"
}