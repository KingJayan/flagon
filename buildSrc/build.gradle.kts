plugins {
    `kotlin-dsl`
}

dependencies {
    // the patched jars carry the whole of teavm-core and teavm-jso-impl, and shadow them here
    implementation("org.teavm:teavm-gradle-plugin:${libs.versions.teavm.get()}") {
        exclude(group = "org.teavm", module = "teavm-core")
        exclude(group = "org.teavm", module = "teavm-jso-impl")
    }
    implementation("dev.flagon:teavm-core-patched")
    implementation("dev.flagon:teavm-jso-impl-patched")

    // the version catalog's generated accessors, which a precompiled script plugin cannot see
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
