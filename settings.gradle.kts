rootProject.name = "file-transporter"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin_version"] as String)
        id("org.jetbrains.compose").version(extra["compose_version"] as String)
    }
}