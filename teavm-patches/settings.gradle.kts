rootProject.name = "teavm-patches"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

// the patched compiler sits in its own build, because gradle reads it before the main build exists
include("teavm-core-patched")
project(":teavm-core-patched").projectDir = file("../teavm")

include("teavm-jso-impl-patched")
project(":teavm-jso-impl-patched").projectDir = file("../teavm-jso")
