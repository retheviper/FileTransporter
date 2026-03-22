package com.retheviper.file.transporter.content

import kotlinx.browser.window
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileBrowserLocationTest {

    @AfterTest
    fun resetHash() {
        window.location.hash = ""
    }

    @Test
    fun readPathFromHashReturnsEmptyWhenHashIsBlank() {
        window.location.hash = ""

        assertEquals("", readPathFromHash())
    }

    @Test
    fun writeAndReadPathRoundTripEncodedCharacters() {
        val path = "/공유 폴더/reports & logs"

        writePathToHash(path)

        assertEquals("#%2F%EA%B3%B5%EC%9C%A0%20%ED%8F%B4%EB%8D%94%2Freports%20%26%20logs", window.location.hash)
        assertEquals(path, readPathFromHash())
    }

    @Test
    fun writePathToHashDoesNotChangeExistingEncodedHash() {
        window.location.hash = "#%2Fuploads%2Fhello%20world"

        writePathToHash("/uploads/hello world")

        assertEquals("#%2Fuploads%2Fhello%20world", window.location.hash)
    }
}
