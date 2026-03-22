package com.retheviper.file.transporter.config

import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.inputStream

private const val DEFAULT_CONFIG_PATH = "config/application.yaml"
private const val DEFAULT_ROOT_DIRECTORY = "."
private const val DEFAULT_MAX_UPLOAD_FILE_SIZE_BYTES = 1024L * 1024L * 1024L

data class AppSettings(
    val rootDirectory: Path,
    val maxUploadFileSizeBytes: Long,
)

object AppConfig {

    fun settings(): AppSettings {
        val configValues = loadYaml()
        val configuredRootDirectory = System.getProperty("file.transporter.root")
            ?: configValues.string("storage.rootDirectory")
            ?: DEFAULT_ROOT_DIRECTORY
        val configuredMaxUploadFileSizeBytes = System.getProperty("file.transporter.upload.maxFileSizeBytes")
            ?.toLongOrNull()
            ?: configValues.long("upload.maxFileSizeBytes")
            ?: DEFAULT_MAX_UPLOAD_FILE_SIZE_BYTES

        require(configuredMaxUploadFileSizeBytes > 0) {
            "upload.maxFileSizeBytes must be greater than 0"
        }

        return AppSettings(
            rootDirectory = Path.of(configuredRootDirectory).toAbsolutePath().normalize(),
            maxUploadFileSizeBytes = configuredMaxUploadFileSizeBytes
        )
    }

    private fun loadYaml(): Map<String, Any?> {
        val configPath = Path.of(System.getProperty("file.transporter.config", DEFAULT_CONFIG_PATH))
        if (Files.notExists(configPath)) {
            return emptyMap()
        }

        val yaml = Yaml()
        configPath.inputStream().use { input ->
            @Suppress("UNCHECKED_CAST")
            return yaml.load(input) as? Map<String, Any?> ?: emptyMap()
        }
    }
}

private fun Map<String, Any?>.string(path: String): String? = value(path) as? String

private fun Map<String, Any?>.long(path: String): Long? {
    val value = value(path) ?: return null
    return when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
}

private fun Map<String, Any?>.value(path: String): Any? {
    var current: Any? = this
    for (segment in path.split('.')) {
        current = (current as? Map<*, *>)?.get(segment) ?: return null
    }
    return current
}
