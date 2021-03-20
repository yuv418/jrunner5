import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
    application
}

group = "me.nonuser"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}


dependencies {
    implementation("org.jooq:joor:0.9.13")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.0.1")
    implementation("org.apache.commons:commons-lang3:3.11")
    implementation("com.github.adelnizamutdinov:kotlin-either:3.0.0")
    implementation("io.ktor:ktor-client-websockets:1.5.1")
    implementation("io.ktor:ktor-client-cio:1.5.1")
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
}


tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}


