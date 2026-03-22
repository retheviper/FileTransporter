package com.retheviper.file.transporter.util

import com.retheviper.file.transporter.constant.FileSizeUnits
import com.retheviper.file.transporter.constant.ROOT_PATH

object FileInfoUtil {

    fun getIconByMimeType(mimeType: String?): String {
        if (mimeType == null) return "📄"
        return when (mimeType.substringBefore(ROOT_PATH)) {
            "image" -> "🏞"
            "video" -> "🎬"
            "audio" -> "🎵"
            "text" -> "🗓"
            "application" -> "🖥"
            else -> "📄"
        }
    }

    fun formatFileSizeWithUnit(size: Long): String {
        val unit = when {
            size < FileSizeUnits.BASE -> FileSizeUnits.BYTE
            size < FileSizeUnits.BASE * FileSizeUnits.BASE -> FileSizeUnits.KILOBYTE
            size < FileSizeUnits.BASE * FileSizeUnits.BASE * FileSizeUnits.BASE -> FileSizeUnits.MEGABYTE
            else -> FileSizeUnits.GIGABYTE
        }
        val value = when (unit) {
            FileSizeUnits.BYTE -> size
            FileSizeUnits.KILOBYTE -> size / FileSizeUnits.BASE
            FileSizeUnits.MEGABYTE -> size / FileSizeUnits.BASE / FileSizeUnits.BASE
            else -> size / FileSizeUnits.BASE / FileSizeUnits.BASE / FileSizeUnits.BASE
        }
        return "$value $unit"
    }

    fun guessTypeLabel(mimeType: String?): String {
        if (mimeType.isNullOrBlank()) return "Unknown"
        return when (mimeType.substringBefore(ROOT_PATH)) {
            "image" -> "Image"
            "video" -> "Video"
            "audio" -> "Audio"
            "text" -> "Text"
            "application" -> mimeType.substringAfter("/").substringBefore(".").replace("-", " ").replaceFirstChar(Char::uppercase)
            else -> mimeType.replace("/", " ").replace("-", " ").replaceFirstChar(Char::uppercase)
        }
    }
}
