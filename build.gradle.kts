import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val kotlin_version: String by project
val ktor_version: String by project
val logback_version: String by project
val serialization_version: String by project
val compose_version: String by project
val koin_version = "3.5.6"

plugins {
    kotlin("plugin.compose")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

group = "com.retheviper"
version = "0.0.1"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    js {
        browser {
            testTask {
                testLogging.showStandardStreams = true
                useKarma {
                    useChromeHeadless()
                    useFirefox()
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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.html:html-core:$compose_version")
                implementation("org.jetbrains.compose.runtime:runtime:$compose_version")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-partial-content-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktor_version")
                implementation("io.ktor:ktor-network-tls-certificates-jvm:$ktor_version")
                implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
                implementation("ch.qos.logback:logback-classic:$logback_version")
                implementation("org.jetbrains.compose.runtime:runtime:$compose_version")
                implementation("io.insert-koin:koin-core:$koin_version")
                implementation("org.yaml:snakeyaml:2.4")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

tasks.named<Copy>("jvmProcessResources") {
    val webpackTask = tasks.named("jsBrowserProductionWebpack")
    dependsOn(webpackTask)
    from(layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable")) {
        include("**/*.js", "**/*.js.map")
    }
}

tasks.named<Jar>("jvmJar") {
    dependsOn(tasks.named("jvmProcessResources"))
}

tasks.register<JavaExec>("run") {
    group = "run"
    description = "Runs the JVM server with bundled JS assets."
    dependsOn(tasks.named("jvmJar"))
    classpath(tasks.named("jvmJar"), configurations.getByName("jvmRuntimeClasspath"))
    mainClass.set("com.retheviper.file.transporter.ServerKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}
