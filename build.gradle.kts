import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.20"
    kotlin("plugin.serialization") version "1.5.20"
    application
}

group = "net.vogman"
version = "3.2"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
    dependencies {
        implementation("com.sksamuel.hoplite:hoplite-core:1.4.3")
        implementation("com.sksamuel.hoplite:hoplite-toml:1.4.3")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
        implementation("io.ktor:ktor-client-core:1.6.1")
        implementation("io.ktor:ktor-client-java:1.6.1")
    }
}
