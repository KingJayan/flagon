description = "teavm-core 0.15.0, rebuilt with the compiler fixes Flagon needs"

dependencies {
    "stock"(libs.teavm.core)
    "compileOnly"(libs.teavm.core)
    "compileOnly"(libs.teavm.classlib)
    "api"(libs.teavm.interop)
    "api"(libs.teavm.metaprogramming.api)
    "api"(libs.teavm.extension.apis)
    "api"(libs.teavm.hppc)

    "api"(libs.teavm.extension.spi)
    "api"(libs.teavm.asm)
    "api"(libs.teavm.asm.tree)
    "api"(libs.teavm.asm.analysis)
    "api"(libs.teavm.asm.commons)
    "api"(libs.teavm.asm.util)
    "api"(libs.teavm.rhino)
    "testImplementation"(platform(libs.junit.bom))
    "testImplementation"(libs.junit.jupiter)
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
