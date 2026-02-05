plugins {
    kotlin("jvm") version "2.3.0"
}

group = "gg.aquatic.votifier"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-handler:4.2.10.Final")
    implementation("io.netty:netty-transport-native-epoll:4.2.9.Final:linux-x86_64")
    implementation("com.google.code.gson:gson:2.13.2")
}


kotlin {
    jvmToolchain(21)
}