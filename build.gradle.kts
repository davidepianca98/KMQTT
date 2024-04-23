
allprojects {
    repositories {
        mavenCentral()
    }
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.24.0")
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.complete.kotlin)
    alias(libs.plugins.goncalossilva.resources)
}

subprojects {
    group = "io.github.davidepianca98"
    version = "0.4.7"
}
