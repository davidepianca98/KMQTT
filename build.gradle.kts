
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}

plugins {
    kotlin("multiplatform") version "1.8.10" apply false
}

subprojects {
    group = "com.github.davidepianca98"
    version = "0.3.3"
}
