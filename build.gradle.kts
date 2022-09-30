val kotlin_version: String by project
val ktor_version: String by project
val logback_version: String by project
val serialization_version: String by project

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    application
    id("io.ktor.plugin") version "2.1.2"
    kotlin("plugin.serialization") version "1.7.10"
}

group = "com.retheviper"
version = "0.0.1"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    js(IR) {
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
        withJava()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
                implementation("io.ktor:ktor-client-core:$ktor_version")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
                implementation(compose.runtime)
                implementation("io.ktor:ktor-client-js:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
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
                implementation("ch.qos.logback:logback-classic:$logback_version")
                implementation(compose.runtime)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
                implementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
            }
        }
    }
}

application {
    mainClass.set("com.retheviper.file.transporter.ServerKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

// include JS artifacts in any JAR we generate
tasks.getByName<Jar>("jvmJar") {
    val webpackTask = tasks.getByName("jsBrowserProductionWebpack")
    dependsOn(webpackTask) // make sure JS gets compiled first
    from(
        File("build/distributions", "file-transporter.js"),
        File("build/distributions", "file-transporter.js.map")
    ) // bring output file along into the JAR
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}

distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}

tasks.getByName<JavaExec>("run") {
    classpath(tasks.getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
}