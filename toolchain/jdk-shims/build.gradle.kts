plugins {
    id("flagon.published")
}

dependencies {
    api(project(":toolchain:classfile"))
    api(libs.teavm.extension.apis)
    api(libs.teavm.extension.spi)
    api(libs.teavm.extension.spi.util)
    compileOnly("dev.flagon:teavm-core-patched:${project.version}")
}
