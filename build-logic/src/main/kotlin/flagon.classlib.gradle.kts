import dev.flagon.build.ClasslibExtension
import dev.flagon.build.UntrackedArguments

plugins {
    id("flagon.java")
}

val classlib = extensions.create<ClasslibExtension>("classlib")
classlib.module.convention("java.base")

tasks.withType<JavaCompile>().configureEach {
    options.release.set(null as Int?)
    sourceCompatibility = "25"
    targetCompatibility = "25"
    val patched = layout.projectDirectory.dir("src/main/java").asFile.absolutePath
    options.compilerArgumentProviders.add(objects.newInstance<UntrackedArguments>().apply {
        arguments.set(classlib.module.map {
            listOf("--add-reads", "$it=ALL-UNNAMED", "--patch-module", "$it=$patched")
        })
    })
}
