plugins {
    id("flagon.classlib")
    id("flagon.published")
}

dependencies {
    compileOnly("dev.flagon:teavm-core-patched:${project.version}")
    compileOnly(libs.teavm.jso)
}
