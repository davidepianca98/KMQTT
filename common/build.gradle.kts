
plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js(IR) {
        useCommonJs()
        nodejs()
    }
    mingwX64 {}
    linuxX64 {}
    linuxArm64 {}
    iosX64 {
        compilations.getByName("main") {
            val interop by cinterops.creating {
                defFile("src/nativeInterop/ios/bindings.def")
            }
        }
    }
    iosArm64 {
        compilations.getByName("main") {
            val interop by cinterops.creating {
                defFile("src/nativeInterop/ios/bindings.def")
            }
        }
    }
    //tvos {}
    //watchos {}
    //macosX64 {}
    //macosArm64 {}

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalUnsignedTypes")
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
                implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
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
            // TODO add openssl dependency
        }
        val iosArm64Main by getting {
            dependsOn(posixMain)
            // TODO add openssl dependency
        }
    }
}
