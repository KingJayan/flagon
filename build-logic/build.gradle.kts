plugins {
    `kotlin-dsl`
}

group = "dev.flagon.build"
version = "0"

dependencies {
    compileOnly("org.teavm:teavm-gradle-plugin:${libs.versions.teavm.get()}")

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
