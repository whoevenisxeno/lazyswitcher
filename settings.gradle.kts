pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    }
}

plugins {
    // Check the latest version on https://stonecutter.kikugie.dev/blog/changes/0.9
    id("dev.kikugie.stonecutter") version "0.9.8"

    // Lets Gradle auto-provision the JDK 1.20.1 needs (17) even if only 21 is installed locally
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        versions("1.20.1", "1.20.6", "1.21.1", "1.21.11")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "lazyswitcher"
