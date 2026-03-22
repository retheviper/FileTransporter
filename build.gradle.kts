import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.retheviper"
version = "0.0.1"

kotlin {
    js {
        browser {
            testTask {
                testLogging.showStandardStreams = true
                useKarma {
                    useChromeHeadless()
                    if (!System.getenv("FIREFOX_BIN").isNullOrBlank()) {
                        useFirefox()
                    }
                }
            }
        }
        binaries.executable()
    }
    jvm {
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.compose.html.core)
                implementation(libs.compose.runtime)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(libs.kotlin.test.js)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.bundles.ktor.server)
                implementation(libs.logback.classic)
                implementation(libs.compose.runtime)
                implementation(libs.koin.core)
                implementation(libs.snakeyaml)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.ktor.server.test.host.jvm)
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

tasks {
    named<Copy>("jvmProcessResources") {
        val webpackTask = named("jsBrowserProductionWebpack")
        dependsOn(webpackTask)
        from(layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable")) {
            include("**/*.js", "**/*.js.map")
        }
    }

    named<Jar>("jvmJar") {
        dependsOn(named("jvmProcessResources"))
    }

    register<JavaExec>("run") {
        group = "run"
        description = "Runs the JVM server with bundled JS assets."
        dependsOn(named("jvmJar"))
        classpath(named("jvmJar"), configurations.named("jvmRuntimeClasspath"))
        mainClass.set("com.retheviper.file.transporter.ServerKt")
    }

    register("test") {
        group = "verification"
        description = "Runs both JVM and JS test suites."
        dependsOn(
            named("jvmTest"),
            named("jsTest")
        )
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}
