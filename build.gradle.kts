group = "io.razza.poiatlas"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.8.0"
    id("java")

    id("xyz.jpenilla.run-paper") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation("org.xerial:sqlite-jdbc:3.40.1.0")
    implementation("net.dv8tion:JDA:5.0.0-beta.3")
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")

    testImplementation(kotlin("test"))
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.19.3")
    }

    withType<Jar> {
        dependsOn(configurations.runtimeClasspath)
    }
}

tasks.test {
    useJUnitPlatform()
}