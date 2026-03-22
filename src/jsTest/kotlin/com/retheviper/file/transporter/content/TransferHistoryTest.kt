package com.retheviper.file.transporter.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TransferHistoryTest {

    @Test
    fun prependHistoryCapsListAtEightEntries() {
        val history = (1..8).map { index ->
            TransferHistoryEntry(
                type = "Upload",
                fileName = "file-$index.txt",
                location = "/uploads",
                detail = "detail-$index",
                state = "Completed",
                timestamp = "10:00:0$index"
            )
        }

        val result = prependHistory(
            history,
            TransferHistoryEntry(
                type = "Download",
                fileName = "latest.txt",
                location = "/downloads",
                detail = "Ready",
                state = "Completed",
                timestamp = "11:11:11"
            )
        )

        assertEquals(8, result.size)
        assertEquals("latest.txt", result.first().fileName)
        assertEquals("file-1.txt", result[1].fileName)
        assertEquals("file-7.txt", result.last().fileName)
    }

    @Test
    fun updateLatestHistoryUpdatesFirstMatchingEntryInPlace() {
        val original = listOf(
            TransferHistoryEntry(
                type = "Upload",
                fileName = "match.txt",
                location = "/old",
                detail = "Preparing transfer",
                state = "Running",
                timestamp = "10:00:00"
            ),
            TransferHistoryEntry(
                type = "Upload",
                fileName = "other.txt",
                location = "/uploads",
                detail = "Upload complete",
                state = "Completed",
                timestamp = "10:05:00"
            )
        )

        val result = updateLatestHistory(
            history = original,
            type = "Upload",
            fileName = "match.txt",
            location = "/new",
            state = "Completed",
            detail = "Upload complete"
        )

        assertEquals(2, result.size)
        assertEquals("/new", result.first().location)
        assertEquals("Completed", result.first().state)
        assertEquals("Upload complete", result.first().detail)
        assertNotEquals("10:00:00", result.first().timestamp)
        assertEquals(original[1], result[1])
    }

    @Test
    fun updateLatestHistoryPrependsEntryWhenNoMatchExists() {
        val history = listOf(
            TransferHistoryEntry(
                type = "Download",
                fileName = "existing.txt",
                location = "/downloads",
                detail = "Ready",
                state = "Completed",
                timestamp = "09:00:00"
            )
        )

        val result = updateLatestHistory(
            history = history,
            type = "Upload",
            fileName = "new.txt",
            location = "/uploads",
            state = "Running",
            detail = "25% uploaded"
        )

        assertEquals(2, result.size)
        assertEquals("new.txt", result.first().fileName)
        assertEquals("Running", result.first().state)
        assertEquals("25% uploaded", result.first().detail)
        assertEquals("existing.txt", result[1].fileName)
    }
}
