plugins {
    id("flagon.published")
}

dependencies {
    api(project(":platform-api"))
    compileOnly("dev.flagon:teavm-core-patched:${project.version}")
}
