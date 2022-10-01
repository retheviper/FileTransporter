package com.retheviper.file.transporter.model

import kotlinx.serialization.Serializable

@Serializable
data class FileTree(
    val name: String,
    val type: String,
    val size: Long? = null,
    val children: List<FileTree>? = null
)