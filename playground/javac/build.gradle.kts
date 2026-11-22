plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val revision = "6c48f4ed707bf0b15f9b6098de30db8aae6fa40f"
val jdk = layout.buildDirectory.dir("jdk").get().asFile
val src = jdk.resolve("src")
val generated = layout.buildDirectory.dir("generated/java").get().asFile

// teavm 0.17.0 emits invalid wasm for a continue out of a try in a loop; the patch avoids that shape
val patch = file("modules.patch")

val fetchJdk = tasks.register<Exec>("fetchJdk") {
    inputs.property("revision", revision)
    inputs.file(patch)
    outputs.dir(jdk)
    workingDir(layout.buildDirectory)
    commandLine(
        "sh", "-ec",
        """
        rm -rf jdk && git init -q jdk && cd jdk
        git remote add origin https://github.com/openjdk/jdk25u.git
        git sparse-checkout set --no-cone /make/langtools/tools /src/jdk.compiler/share/classes \
            /src/java.compiler/share/classes /src/jdk.internal.opt/share/classes \
            /src/java.base/share/classes/jdk/internal/jmod
        git fetch -q --depth 1 --filter=blob:none origin $revision
        git checkout -q FETCH_HEAD
        git apply ${patch.absolutePath}
        find src -name module-info.java -delete
        """.trimIndent(),
    )
}

val compileTools = tasks.register<JavaCompile>("compileTools") {
    dependsOn(fetchJdk)
    source(fileTree(jdk.resolve("make/langtools/tools")) {
        exclude("anttasks/**", "genstubs/**", "crules/**")
    })
    destinationDirectory = layout.buildDirectory.dir("buildTools")
    classpath = files()
}

val parsed = listOf("compiler", "launcher")
val generateProperties = tasks.register<JavaExec>("generateProperties") {
    dependsOn(fetchJdk)
    classpath(compileTools, jdk.resolve("make/langtools/tools"))
    mainClass = "propertiesparser.PropertiesParser"
    val resources = "jdk.compiler/share/classes/com/sun/tools/javac/resources"
    for (name in parsed) {
        args("-compile", src.resolve("$resources/$name.properties"), generated.resolve(resources))
    }
    outputs.dir(generated.resolve(resources))
    doFirst { generated.resolve(resources).mkdirs() }
}

val bundled = listOf("compiler", "launcher", "javac")
val compileProperties = tasks.register<JavaExec>("compileProperties") {
    dependsOn(fetchJdk)
    classpath(compileTools, jdk.resolve("make/langtools/tools"))
    mainClass = "compileproperties.CompileProperties"
    val classes = "jdk.compiler/share/classes"
    workingDir(src.resolve(classes))
    for (name in bundled) {
        val path = "com/sun/tools/javac/resources/$name"
        args("-compile", "./$path.properties", generated.resolve("$classes/$path.java"), "java.util.ListResourceBundle")
    }
    outputs.dir(generated.resolve("$classes/com/sun/tools/javac/resources"))
    doFirst { generated.resolve("$classes/com/sun/tools/javac/resources").mkdirs() }
}

sourceSets.main {
    java {
        srcDir(src.resolve("jdk.compiler/share/classes"))
        srcDir(generated.resolve("jdk.compiler/share/classes"))
        srcDir(src.resolve("java.compiler/share/classes"))
        srcDir(src.resolve("jdk.internal.opt/share/classes"))
        srcDir(src.resolve("java.base/share/classes"))
        exclude("**/module-info.java")
    }
}

tasks.compileJava {
    dependsOn(fetchJdk, generateProperties, compileProperties)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(
        listOf(
            "--limit-modules", "java.base",
            "--add-exports", "java.base/jdk.internal.javac=ALL-UNNAMED",
            "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED",
            "--add-exports", "java.base/jdk.internal.module=ALL-UNNAMED",
            "--add-exports", "java.base/sun.reflect.annotation=ALL-UNNAMED",
            "-nowarn", "-Xlint:none",
        ),
    )
}
