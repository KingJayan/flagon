plugins {
    id("flagon.published")
}

dependencies {
    api(project(":platform-api"))
    api(libs.teavm.jso.apis)
    api(libs.teavm.platform)
    compileOnly("dev.flagon:teavm-core-patched:${project.version}")
}
