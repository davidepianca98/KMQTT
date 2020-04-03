import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("multiplatform") version "1.3.71"
    kotlin("plugin.serialization") version "1.3.71"
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "com.github.davidepianca98"
version = "0.2.0"

val serializationVersion: String by project

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    jvm {
        compilations["main"].kotlinOptions {
            // Setup the Kotlin compiler options for the 'main' compilation:
            jvmTarget = "1.8"
        }
        compilations["test"].kotlinOptions {
            // Setup the Kotlin compiler options for the 'test' compilation:
            jvmTarget = "1.8"
        }
    }
    mingwX64("mingw") {
        compilations.getByName("main") {
            val openssl by cinterops.creating {
                packageName("openssl")
            }
        }
        binaries {
            executable {
                linkerOpts = mutableListOf("-Lsrc/nativeInterop/cinterop/opensslLibs/mingwX64", "-lssl", "-lcrypto")
            }
        }
    }
    linuxX64 {
        binaries {
            executable()
        }
    }
    linuxArm32Hfp {
        binaries {
            executable()
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
            }
        }
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-common:$serializationVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("com.hivemq:hivemq-mqtt-client:1.1.3")
            }
        }
        val mingwMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-mingwx64:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-mingwx64:$serializationVersion")
            }
        }
        val mingwTest by getting {

        }
        val linuxX64Main by getting {
            dependencies {
                implementation(files("src/nativeInterop/openssl-linux-x64.klib"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-linuxx64:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-linuxx64:$serializationVersion")
            }
        }
        val linuxX64Test by getting {

        }
        val linuxArm32HfpMain by getting {
            dependencies {
                implementation(files("src/nativeInterop/openssl-linux-arm32-hfp.klib"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-linuxarm32hfp:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-linuxarm32hfp:$serializationVersion")
            }
        }
        val linuxArm32HfpTest by getting {

        }
    }
}

task("shadowJar", ShadowJar::class) {
    manifest.attributes["Main-Class"] = "MainKt"
    from(kotlin.targets["jvm"].compilations["main"].output)
    val runtimeClasspath =
        (kotlin.targets["jvm"].compilations["main"] as org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles).runtimeDependencyFiles
    configurations = mutableListOf(runtimeClasspath as Configuration)
}

tasks {
    build {
        dependsOn("shadowJar")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/davidepianca98/KMQTT")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_PACKAGES")
            }
        }
    }
}
