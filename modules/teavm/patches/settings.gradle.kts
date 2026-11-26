rootProject.name = "teavm-patches"

pluginManagement {
    includeBuild("../../../build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../../../gradle/libs.versions.toml"))
        }
    }
}

include("teavm-core-patched")
project(":teavm-core-patched").projectDir = file("../core")

include("teavm-jso-impl-patched")
project(":teavm-jso-impl-patched").projectDir = file("../jso")
