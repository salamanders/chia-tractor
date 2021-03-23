import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
    application
}

group = "info.benjamin.chiatractor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        useIR = true
    }
}

application {
    mainClassName = "TractorKt"
}


dependencies {
    implementation(kotlin("script-runtime"))
}