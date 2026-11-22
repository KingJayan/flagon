plugins {
    java
    id("org.teavm")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val teavmVersion = "0.17.0"

val classlibInput = configurations.create("classlibInput") { isTransitive = false }
val bootstrapCompiler = configurations.create("bootstrapCompiler") { isTransitive = false }

val classlibEmu = sourceSets.create("classlibEmu")


dependencies {
    implementation(project(":javac"))
    compileOnly("org.teavm:teavm-core:$teavmVersion")
    implementation("org.teavm:teavm-tooling:$teavmVersion")
    implementation("org.teavm:teavm-bootstrap-compiler:$teavmVersion")
    implementation("org.ow2.asm:asm:9.8")
    classlibInput("org.teavm:teavm-classlib:$teavmVersion")
    classlibInput("org.teavm:teavm-jso:$teavmVersion")
    classlibInput("org.teavm:teavm-jso-apis:$teavmVersion")
    bootstrapCompiler("org.teavm:teavm-bootstrap-compiler:$teavmVersion")
}


tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("--limit-modules", "java.base"))
}

teavm {
    all {
        mainClass = "org.teavm.javac.CompilerLib"
    }
    wasmGC {
        outOfProcess = true
        processMemory = 4096
        modularRuntime = true
    }
}

val classlib = layout.buildDirectory.dir("classlib")

val generateClassLib = tasks.register<JavaExec>("generateClassLib") {
    val output = classlib.map { it.file("compile-classlib-teavm.bin") }
    inputs.files(classlibInput, classlibEmu.output)
    outputs.file(output)
    classpath(sourceSets.main.get().output, sourceSets.main.get().runtimeClasspath)
    mainClass = "org.teavm.javac.StdlibConverter"
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(output.get().asFile.absolutePath) +
            classlibInput.files.map { it.absolutePath } +
            classlibEmu.output.classesDirs.files.map { it.absolutePath }
    })
}

val extractRuntimeClassLib = tasks.register<Sync>("extractRuntimeClassLib") {
    from({ zipTree(bootstrapCompiler.singleFile) }) {
        include("org/teavm/compiler/bootstrap/runtime-classlib-teavm.bin")
        eachFile { path = name }
    }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("runtime-classlib"))
}

tasks.register<Sync>("dist") {
    from(tasks.named("generateWasmGC")) {
        include("compiler.wasm")
    }
    from(tasks.named("copyWasmGCRuntime"))
    from(generateClassLib, extractRuntimeClassLib)
    into(layout.buildDirectory.dir("dist"))
}
