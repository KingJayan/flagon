import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    `java-library`
    checkstyle
}

val libs = the<LibrariesForLibs>()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.add("-Xlint:all,-serial,-processing")
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = rootProject.file("toolchain/checkstyle/checkstyle.xml")
}

// generated code is not written by hand, so style rules do not apply to it
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

// maven put a provided dependency on the test classpath too, and the tests still expect it there
configurations.named("testCompileOnly") { extendsFrom(configurations["compileOnly"]) }
configurations.named("testRuntimeOnly") { extendsFrom(configurations["compileOnly"]) }
