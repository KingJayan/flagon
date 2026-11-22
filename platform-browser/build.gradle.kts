plugins {
    id("flagon.published")
}

dependencies {
    api(project(":platform-api"))
    api(libs.teavm.jso.apis)
    api(libs.teavm.platform)
    compileOnly("dev.flagon:teavm-core-patched:${project.version}")
}

// ApiReferenceTest reads the site's api reference and the sources it covers
tasks.test {
    val root = rootProject.layout.projectDirectory
    inputs.file(root.file("web/docs/api.md")).withPathSensitivity(PathSensitivity.RELATIVE)
    listOf("runtime", "platform-api", "platform-browser", "toolchain/reflect-policy").forEach {
        inputs.dir(root.dir("$it/src/main/java")).withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
