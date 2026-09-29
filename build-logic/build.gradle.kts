// conventions here read no version catalog
plugins {
    `kotlin-dsl`
}

group = "dev.flagon.build"
version = "0"

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
