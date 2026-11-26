plugins {
    id("flagon.published")
}

dependencies {
    api(libs.teavm.extension.apis)
    api(libs.teavm.extension.spi)
    api(libs.teavm.extension.spi.util)
    compileOnly("dev.flagon:teavm-core-patched:${project.version}")
}
