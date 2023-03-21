
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.20.0")
    }
}

plugins {
    kotlin("multiplatform") version "1.8.10" apply false
    id("com.louiscad.complete-kotlin") version "1.1.0"
}

subprojects {
    group = "com.github.davidepianca98"
    version = "0.4.0"
}
