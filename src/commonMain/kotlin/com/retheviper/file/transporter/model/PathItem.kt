package com.retheviper.file.transporter.model

import com.retheviper.file.transporter.constant.FileType
import kotlinx.serialization.Serializable

@Serializable
data class PathItem(
    val name: String,
    val type: FileType,
    val path: String,
    val mimeType: String? = null,
    val size: Long? = null
) {
    var isDirectory: Boolean = type == FileType.DIRECTORY
}