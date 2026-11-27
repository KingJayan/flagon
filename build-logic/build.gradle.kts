plugins {
    `kotlin-dsl`
}

group = "dev.flagon.build"
version = "0"

dependencies {
    implementation(project(":bundle-plugin"))

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
