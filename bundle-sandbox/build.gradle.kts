import dev.flagon.build.RunMain
plugins {
    id("flagon.bundle")
}

val absent = sourceSets.create("absent") {
    java.setSrcDirs(listOf("src/main/java"))
    java.include("org/example/absent/**")
}

sourceSets["main"].java.exclude("org/example/absent/**")

// keeps parameter names, which reflection reads back
tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-parameters")
}

dependencies {
    implementation(project(":runtime"))
    implementation(project(":platform-browser"))
    implementation(project(":lwjgl2-compat"))
    implementation(project(":lwjgl3-compat"))
    implementation(project(":toolchain:socket-classlib"))
    implementation(project(":toolchain:nio-classlib"))
    implementation(project(":toolchain:jdk-classlib"))
    implementation(project(":toolchain:awt-classlib"))
    implementation(project(":toolchain:crypto-classlib"))
    implementation(project(":toolchain:concurrent-classlib"))
    implementation(project(":toolchain:reflect-policy"))
    implementation(project(":toolchain:jdk-shims"))
    compileOnly(absent.output)
}

bundle {
    mainClass.set("dev.flagon.sandbox.SandboxMain")
    catalogClass.set("dev.flagon.sandbox.SandboxCatalog")
    reflectClasses.set("dev.flagon.sandbox.ReflectedBy*")
    reflectProxies.set("dev.flagon.sandbox.Proxied")
    reflectConverted.set("java.lang.Integer")
    apartFrom.set(256)
}

tasks.named<Test>("test") {
    systemProperty("flagon.serial.update",
        providers.systemProperty("flagon.serial.update").getOrElse("false"))
}

tasks.named<RunMain>("writeCatalog") {
    runtimeClasspath.from(absent.output)
}
