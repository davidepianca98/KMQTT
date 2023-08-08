import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.0"
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val serializationVersion: String by project
val coroutineVersion: String by project

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js {
        useCommonJs()
        nodejs {
            binaries.executable()
        }
    }
    mingwX64 {
        binaries {
            executable()
        }
    }
    linuxX64 {
        binaries {
            executable()
        }
    }
    linuxArm64 {
        binaries {
            executable()
        }
    }
    macosX64 {
        binaries {
            executable()
        }
    }
    macosArm64 {
        binaries {
            executable()
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":kmqtt-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":kmqtt-client"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutineVersion")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("dnsjava:dnsjava:3.5.2")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin-wrappers:kotlin-node:18.16.12-pre.599")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val posixMain by creating {
            dependsOn(commonMain)
        }
        val mingwX64Main by getting {
            dependsOn(posixMain)
        }
        val linuxX64Main by getting {
            dependsOn(posixMain)
        }
        val linuxArm64Main by getting {
            dependsOn(posixMain)
        }
        val macosX64Main by getting {
            dependsOn(posixMain)
        }
        val macosArm64Main by getting {
            dependsOn(posixMain)
        }
    }
}

task("shadowJar", ShadowJar::class) {
    manifest.attributes["Main-Class"] = "MainKt"
    from(kotlin.targets["jvm"].compilations["main"].output)
    val runtimeClasspath =
        (kotlin.targets["jvm"].compilations["main"] as org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles).runtimeDependencyFiles
    configurations = listOf(runtimeClasspath as Configuration)
}

tasks {
    build {
        dependsOn("shadowJar")
    }
}

publishing {
    repositories {
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/davidepianca98/KMQTT")
            credentials(PasswordCredentials::class)

        }
    }
}