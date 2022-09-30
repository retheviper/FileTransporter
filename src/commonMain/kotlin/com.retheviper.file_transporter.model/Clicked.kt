package com.retheviper.file_transporter.model

import kotlinx.serialization.Serializable


@Serializable
data class Clicked(
    val number: Int = 0
) {
    companion object {
        const val endpoint = "/click"
    }
}