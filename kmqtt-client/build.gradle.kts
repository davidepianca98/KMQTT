
plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("kotlinx-atomicfu")
}

kotlin {
    explicitApi()

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
    iosX64 {}
    iosArm64 {}
    iosSimulatorArm64 {}
    macosX64 {}
    macosArm64 {}
    //tvos {}
    //watchos {}

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalUnsignedTypes")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":kmqtt-common"))
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
        val jsMain by getting {}
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
        val iosX64Main by getting {
            dependsOn(posixMain)
        }
        val iosArm64Main by getting {
            dependsOn(posixMain)
        }
        val iosSimulatorArm64Main by getting {
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
