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

// JdkFormsTableTest reads the table from jdk-classlib's source
tasks.test {
    inputs.file(rootProject.layout.projectDirectory.file("modules/toolchain/jdk-classlib/src/main/java/java/io/JdkForms.java"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
