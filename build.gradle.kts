plugins {
    id("java")
}

group = "org.slimecraft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    implementation("net.minestom:minestom:2025.09.13-1.21.8")
    implementation("net.kyori:adventure-text-minimessage:4.24.0")
    implementation("org.tinylog:slf4j-tinylog:2.8.0-M1")
    implementation("org.tinylog:tinylog-impl:2.8.0-M1")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}