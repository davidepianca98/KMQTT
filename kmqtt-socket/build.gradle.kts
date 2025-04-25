
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("convention.publication")
}

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js {
        nodejs()
    }
    mingwX64 {}
    linuxX64 {}
    linuxArm64 {}
    iosX64 {}
    iosArm64 {}
    iosSimulatorArm64 {}
    macosX64 {}
    macosArm64 {}
    tvosX64 {}
    tvosSimulatorArm64 {}
    tvosArm64 {}
    watchosArm32 {}
    watchosArm64 {}
    watchosSimulatorArm64 {}
    watchosX64 {}

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {}
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.kotlin.node)
                implementation(kotlin("test-js"))
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
            dependencies {
                implementation(files("src/nativeInterop/openssl-mingw-x64.klib"))
            }
        }
        val linuxX64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-linux-x64.klib"))
            }
        }
        val linuxArm64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-linux-arm64.klib"))
            }
        }
        val iosX64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-ios-x64.klib"))
            }
        }
        val iosArm64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-ios-arm64.klib"))
            }
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-ios-simulator-arm64.klib"))
            }
        }
        val macosX64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-macos-x64.klib"))
            }
        }
        val macosArm64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-macos-arm64.klib"))
            }
        }
        val tvosX64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-tvos-x64.klib"))
            }
        }
        val tvosArm64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-tvos-arm64.klib"))
            }
        }
        val tvosSimulatorArm64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-tvos-simulator-arm64.klib"))
            }
        }
        val watchosX64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-watchos-x64.klib"))
            }
        }
        val watchosArm32Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-watchos-arm32.klib"))
            }
        }
        val watchosArm64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-watchos-arm64.klib"))
            }
        }
        val watchosSimulatorArm64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(files("src/nativeInterop/openssl-watchos-simulator-arm64.klib"))
            }
        }
    }
}

// Fix Gradle warning about signing tasks using publishing task outputs without explicit dependencies
// https://github.com/gradle/gradle/issues/26091
tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
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
