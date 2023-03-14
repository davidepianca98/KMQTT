import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.8.10"
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val serializationVersion: String by project

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js(IR) {
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

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlin.ExperimentalUnsignedTypes")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
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
                implementation("com.hivemq:hivemq-mqtt-client:1.3.0")
                implementation("org.eclipse.paho:org.eclipse.paho.mqttv5.client:1.2.5")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.4")
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
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/davidepianca98/KMQTT")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_PACKAGES")
            }
        }
    }
}
