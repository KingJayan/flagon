// a build of its own, as a project outside this repository would have. make standalone installs
// the modules it builds against in the local maven repository first
pluginManagement {
    // the version this repository builds; a project of your own names a release here
    val flagon = java.util.Properties().apply {
        settingsDir.resolve("../../gradle.properties").reader().use { load(it) }
    }.getProperty("version")

    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    plugins {
        id("dev.flagon.bundle") version flagon
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "standalone"
