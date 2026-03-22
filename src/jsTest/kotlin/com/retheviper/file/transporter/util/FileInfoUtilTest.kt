package com.retheviper.file.transporter.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FileInfoUtilTest {

    @Test
    fun getIconByMimeTypeMapsKnownTopLevelTypes() {
        assertEquals("🏞", FileInfoUtil.getIconByMimeType("image/png"))
        assertEquals("🎬", FileInfoUtil.getIconByMimeType("video/mp4"))
        assertEquals("🎵", FileInfoUtil.getIconByMimeType("audio/mpeg"))
        assertEquals("🗓", FileInfoUtil.getIconByMimeType("text/plain"))
        assertEquals("🖥", FileInfoUtil.getIconByMimeType("application/pdf"))
        assertEquals("📄", FileInfoUtil.getIconByMimeType("font/woff2"))
        assertEquals("📄", FileInfoUtil.getIconByMimeType(null))
    }

    @Test
    fun formatFileSizeWithUnitUsesExpectedUnitThresholds() {
        assertEquals("512 byte", FileInfoUtil.formatFileSizeWithUnit(512))
        assertEquals("1 kb", FileInfoUtil.formatFileSizeWithUnit(1024))
        assertEquals("2 mb", FileInfoUtil.formatFileSizeWithUnit(2L * 1024 * 1024))
        assertEquals("3 gb", FileInfoUtil.formatFileSizeWithUnit(3L * 1024 * 1024 * 1024))
    }

    @Test
    fun guessTypeLabelFormatsKnownAndFallbackMimeTypes() {
        assertEquals("Image", FileInfoUtil.guessTypeLabel("image/png"))
        assertEquals("Text", FileInfoUtil.guessTypeLabel("text/plain"))
        assertEquals("Pdf", FileInfoUtil.guessTypeLabel("application/pdf"))
        assertEquals("Model vnd object", FileInfoUtil.guessTypeLabel("model/vnd-object"))
        assertEquals("Unknown", FileInfoUtil.guessTypeLabel(null))
        assertEquals("Unknown", FileInfoUtil.guessTypeLabel(""))
    }
}
