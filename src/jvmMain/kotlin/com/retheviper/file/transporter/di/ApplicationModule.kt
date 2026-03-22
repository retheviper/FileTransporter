package com.retheviper.file.transporter.di

import com.retheviper.file.transporter.config.AppConfig
import com.retheviper.file.transporter.config.AppSettings
import com.retheviper.file.transporter.service.FileStorageService
import com.retheviper.file.transporter.service.LocalFileStorageService
import org.koin.dsl.module

fun applicationModule() = module {
    single<AppSettings> { AppConfig.settings() }
    single<FileStorageService> { LocalFileStorageService(get<AppSettings>().rootDirectory) }
}
