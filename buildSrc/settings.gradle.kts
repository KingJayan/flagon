// the patched compiler must reach the buildscript classpath, so it is built before this one
includeBuild("../teavm-patches")

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
