
plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js(IR) {
        useCommonJs()
        browser()
    }
    mingwX64 {}
    linuxX64 {}
    linuxArm64 {}
    //macosX64 {}
    //macosArm64 {}
    //iosSimulatorArm64 {}
    //iosX64 {}
    //watchosSimulatorArm64 {}
    //watchosX64 {}
    //watchosArm32 {}
    //watchosArm64 {}
    //tvosSimulatorArm64 {}
    //tvosX64 {}
    //tvosArm64 {}
    //iosArm64 {}

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalUnsignedTypes")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":common"))
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
