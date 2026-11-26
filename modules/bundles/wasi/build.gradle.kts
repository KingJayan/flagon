import dev.flagon.build.wasmtime

plugins {
    id("flagon.bundle")
}

dependencies {
    implementation(project(":runtime"))
    implementation(project(":platform:wasi"))
    implementation(project(":toolchain:jdk-shims"))
}

bundle {
    mainClass.set("dev.flagon.wasi.WasiMain")
}

val component = layout.buildDirectory.file("wasi/wasi.wasm")

// runs the component under wasmtime, so it needs wasm-tools and wasmtime where test does not
testing {
    suites {
        register<JvmTestSuite>("wasiTest") {
            dependencies {
                implementation(platform(libs.junit.bom))
                implementation(libs.junit.jupiter)
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
            targets.all {
                testTask.configure {
                    description = "run the wasi component's programs under wasmtime"
                    dependsOn("wasi")
                    inputs.file(component).withPropertyName("component")
                        .withPathSensitivity(PathSensitivity.NONE)
                    val configured = providers.gradleProperty("flagon.wasmtime")
                    val path = component.map { it.asFile.absolutePath }
                    doFirst {
                        systemProperty("flagon.wasi.component", path.get())
                        systemProperty("flagon.wasmtime", wasmtime(configured.orNull))
                    }
                }
            }
        }
    }
}
