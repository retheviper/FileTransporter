package com.retheviper.file.transporter.plugins

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoutingTest {

    @Test
    fun multipartLimitErrorIsDetectedFromCurrentKtorMessage() {
        val error = IOException("Multipart content length exceeds limit 16 > 10; limit is defined using 'formFieldLimit' argument")

        assertTrue(error.isMultipartLimitError())
    }

    @Test
    fun multipartLimitErrorIsDetectedFromNestedCause() {
        val error = IllegalStateException(
            "wrapper",
            IOException("Length exceeded while searching for multipart boundary")
        )

        assertTrue(error.isMultipartLimitError())
    }

    @Test
    fun unrelatedErrorsAreIgnored() {
        val error = IOException("Broken pipe")

        assertFalse(error.isMultipartLimitError())
    }
}
