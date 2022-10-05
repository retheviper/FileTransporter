package com.retheviper.file.transporter.util

import com.retheviper.file.transporter.constant.CONTENT_SIZE_UNIT_BYTE
import com.retheviper.file.transporter.constant.CONTENT_SIZE_UNIT_GIGABYTE
import com.retheviper.file.transporter.constant.CONTENT_SIZE_UNIT_KILOBYTE
import com.retheviper.file.transporter.constant.CONTENT_SIZE_UNIT_MEGABYTE
import com.retheviper.file.transporter.constant.CONTENT_SIZE_UNIT_VALUE
import com.retheviper.file.transporter.constant.SLASH
import io.ktor.http.ContentType

object FileInfoUtil {

    fun getIconByMimeType(mimeType: String?): String {
        if (mimeType == null) return "📄"
        return when (mimeType.substringBefore(SLASH)) {
            ContentType.Image.Any.contentType -> "🏞"
            ContentType.Video.Any.contentType -> "🎬"
            ContentType.Audio.Any.contentType -> "🎵"
            ContentType.Text.Any.contentType -> "🗓"
            ContentType.Application.Any.contentType -> "🖥"
            else -> "📄"
        }
    }

    fun formatFileSizeWithUnit(size: Long): String {
        val unit = when {
            size < CONTENT_SIZE_UNIT_VALUE -> CONTENT_SIZE_UNIT_BYTE
            size < CONTENT_SIZE_UNIT_VALUE * CONTENT_SIZE_UNIT_VALUE -> CONTENT_SIZE_UNIT_KILOBYTE
            size < CONTENT_SIZE_UNIT_VALUE * CONTENT_SIZE_UNIT_VALUE * CONTENT_SIZE_UNIT_VALUE -> CONTENT_SIZE_UNIT_MEGABYTE
            else -> CONTENT_SIZE_UNIT_GIGABYTE
        }
        val value = when (unit) {
            CONTENT_SIZE_UNIT_BYTE -> size
            CONTENT_SIZE_UNIT_KILOBYTE -> size / CONTENT_SIZE_UNIT_VALUE
            CONTENT_SIZE_UNIT_MEGABYTE -> size / CONTENT_SIZE_UNIT_VALUE / CONTENT_SIZE_UNIT_VALUE
            else -> size / CONTENT_SIZE_UNIT_VALUE / CONTENT_SIZE_UNIT_VALUE / CONTENT_SIZE_UNIT_VALUE
        }
        return "$value $unit"
    }
}