plugins {
    id("flagon.published")
}

dependencies {
    testImplementation(libs.teavm.jso)
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.add("-parameters")
}
