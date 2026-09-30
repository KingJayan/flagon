plugins {
    id("flagon.java")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(null as Int?)
    sourceCompatibility = "25"
    targetCompatibility = "25"
    options.compilerArgs.addAll(
        listOf(
            "--patch-module", "java.base=${project.projectDir}/src/main/java",
            "--add-reads", "java.base=ALL-UNNAMED",
        ),
    )
}
