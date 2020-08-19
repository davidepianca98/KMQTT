import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("multiplatform") version "1.4.0"
    kotlin("plugin.serialization") version "1.4.0"
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "com.github.davidepianca98"
version = "0.2.1"

val serializationVersion: String by project

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
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
    mingwX64("mingwX64") {
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
                useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
            }
        }
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
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
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("com.hivemq:hivemq-mqtt-client:1.2.1")
            }
        }
        val posixMain by creating {
            dependsOn(commonMain.get())
        }
        val mingwX64Main by getting {
            dependsOn(posixMain)
        }
        val linuxX64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-linux-x64.klib"))
            }
        }
        val linuxArm32HfpMain by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-linux-arm32-hfp.klib"))
            }
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
