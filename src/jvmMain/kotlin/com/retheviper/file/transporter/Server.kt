package com.retheviper.file.transporter

import com.retheviper.file.transporter.config.AppSettings
import com.retheviper.file.transporter.di.applicationModule
import com.retheviper.file.transporter.plugins.configureContent
import com.retheviper.file.transporter.plugins.configureLogging
import com.retheviper.file.transporter.plugins.configureRouting
import com.retheviper.file.transporter.plugins.configureSerialization
import com.retheviper.file.transporter.plugins.configureStatusPages
import com.retheviper.file.transporter.service.FileStorageService
import io.ktor.server.application.Application
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(
        factory = Netty,
        environment = applicationEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
        },
        configure = {
            configureServerConnectors()
        },
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    stopKoin()
    val koin = startKoin {
        modules(applicationModule())
    }.koin
    val settings = koin.get<AppSettings>()
    val fileStorageService = koin.get<FileStorageService>()

    configureLogging()
    configureSerialization()
    configureStatusPages()
    configureRouting(
        fileStorageService = fileStorageService,
        maxUploadFileSizeBytes = settings.maxUploadFileSizeBytes
    )
    configureContent()
}
