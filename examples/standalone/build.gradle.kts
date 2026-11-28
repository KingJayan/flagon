import dev.flagon.build.FlagonVersions

plugins {
    id("dev.flagon.bundle")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation("dev.flagon:runtime:${FlagonVersions.flagon}")
    implementation("dev.flagon:platform-browser:${FlagonVersions.flagon}")
}

bundle {
    mainClass.set("org.example.standalone.StandaloneMain")
    catalogClass.set("org.example.standalone.StandaloneCatalog")
}
