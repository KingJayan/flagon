import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("flagon.toolchain")
    checkstyle
}

val libs = the<LibrariesForLibs>()

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all,-serial,-processing")
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = rootProject.file("toolchain/checkstyle/checkstyle.xml")
}

tasks.withType<Checkstyle>().configureEach {
    val generated = layout.buildDirectory.get().asFile
    setSource(source.filter { !it.startsWith(generated) })
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

configurations.named("testCompileOnly") { extendsFrom(configurations["compileOnly"]) }
configurations.named("testRuntimeOnly") { extendsFrom(configurations["compileOnly"]) }
