plugins {
    kotlin("jvm") version "2.1.20"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    kotlin {
        ktfmt("0.58").kotlinlangStyle()
    }
}


kotlin {
    jvmToolchain(21)
}