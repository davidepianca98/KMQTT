
allprojects {
    repositories {
        mavenCentral()
    }
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.23.1")
    }
}

plugins {
    kotlin("multiplatform") version "1.9.22" apply false
    id("com.louiscad.complete-kotlin") version "1.1.0"
    id("com.goncalossilva.resources") version "0.4.0"
}

subprojects {
    group = "io.github.davidepianca98"
    version = "0.4.4"
}
