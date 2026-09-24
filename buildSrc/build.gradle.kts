plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.teavm:teavm-gradle-plugin:${libs.versions.teavm.get()}") {
        exclude(group = "org.teavm", module = "teavm-core")
        exclude(group = "org.teavm", module = "teavm-jso-impl")
    }
    implementation("dev.flagon:teavm-core-patched")
    implementation("dev.flagon:teavm-jso-impl-patched")

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
