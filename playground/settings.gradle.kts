rootProject.name = "flagon-playground"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.teavm") version "0.17.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("javac", "compiler")
