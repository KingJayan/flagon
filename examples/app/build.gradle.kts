plugins {
    id("flagon.java")
}

dependencies {
    compileOnly(libs.lwjgl2)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Main-Class" to "org.example.spinner.SpinnerApp")
    }
}
