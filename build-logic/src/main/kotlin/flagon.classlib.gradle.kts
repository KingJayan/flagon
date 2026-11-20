import dev.flagon.build.UntrackedArguments

plugins {
    id("flagon.java")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(null as Int?)
    sourceCompatibility = "25"
    targetCompatibility = "25"
    options.compilerArgs.addAll(listOf("--add-reads", "java.base=ALL-UNNAMED"))
    val patched = layout.projectDirectory.dir("src/main/java").asFile.absolutePath
    options.compilerArgumentProviders.add(objects.newInstance<UntrackedArguments>().apply {
        arguments.set(listOf("--patch-module", "java.base=$patched"))
    })
}
