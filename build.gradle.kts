plugins {
    kotlin("jvm") version "2.2.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.hamcrest:hamcrest:2.2")
    implementation(platform("dev.forkhandles:forkhandles-bom:2.25.0.0"))
    implementation("dev.forkhandles:result4k")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}