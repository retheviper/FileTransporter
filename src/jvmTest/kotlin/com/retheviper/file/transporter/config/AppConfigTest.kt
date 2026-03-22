package com.retheviper.file.transporter.config

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppConfigTest {

    @AfterTest
    fun clearProperties() {
        System.clearProperty("file.transporter.config")
        System.clearProperty("file.transporter.root")
        System.clearProperty("file.transporter.upload.maxFileSizeBytes")
    }

    @Test
    fun settingsReadValuesFromConfiguredYamlFile() {
        val configFile = Files.createTempFile("file-transporter-config", ".yaml")
        val uploadsDirectory = Files.createTempDirectory("file-transporter-config-uploads")
        try {
            configFile.writeText(
                """
                storage:
                  rootDirectory: ${uploadsDirectory.toAbsolutePath()}
                upload:
                  maxFileSizeBytes: 2048
                """.trimIndent()
            )
            System.setProperty("file.transporter.config", configFile.toString())

            val settings = AppConfig.settings()

            assertEquals(uploadsDirectory.toAbsolutePath().normalize(), settings.rootDirectory)
            assertEquals(2048L, settings.maxUploadFileSizeBytes)
        } finally {
            configFile.toFile().delete()
            uploadsDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun systemPropertiesOverrideYamlValues() {
        val configFile = Files.createTempFile("file-transporter-config-override", ".yaml")
        val uploadsDirectory = Files.createTempDirectory("file-transporter-config-override-uploads")
        try {
            configFile.writeText(
                """
                storage:
                  rootDirectory: ${uploadsDirectory.toAbsolutePath()}
                upload:
                  maxFileSizeBytes: 2048
                """.trimIndent()
            )
            System.setProperty("file.transporter.config", configFile.toString())
            System.setProperty("file.transporter.root", "/tmp/override-root")
            System.setProperty("file.transporter.upload.maxFileSizeBytes", "4096")

            val settings = AppConfig.settings()

            assertEquals(java.nio.file.Path.of("/tmp/override-root").toAbsolutePath().normalize(), settings.rootDirectory)
            assertEquals(4096L, settings.maxUploadFileSizeBytes)
        } finally {
            configFile.toFile().delete()
            uploadsDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun settingsRejectNonPositiveUploadLimit() {
        System.setProperty("file.transporter.upload.maxFileSizeBytes", "0")

        val error = assertFailsWith<IllegalArgumentException> {
            AppConfig.settings()
        }

        assertEquals("upload.maxFileSizeBytes must be greater than 0", error.message)
    }
}
