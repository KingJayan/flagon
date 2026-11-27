plugins {
    id("flagon.published")
}

// the real openal classes, which the surface test reads and never loads
val realLwjgl2 = configurations.create("realLwjgl2") { isTransitive = false }
val realLwjgl3 = configurations.create("realLwjgl3") { isTransitive = false }

dependencies {
    api(project(":lwjgl:core"))
    testImplementation(project(path = ":lwjgl:core", configuration = "testFixtures"))
    realLwjgl2(libs.lwjgl2)
    realLwjgl3(libs.lwjgl3.openal)
}

tasks.named<Test>("test") {
    val lwjgl2 = files(realLwjgl2)
    val lwjgl3 = files(realLwjgl3)
    inputs.files(lwjgl2, lwjgl3).withPropertyName("realLwjgl").withNormalizer(ClasspathNormalizer::class)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dflagon.openal.lwjgl2=${lwjgl2.singleFile}", "-Dflagon.openal.lwjgl3=${lwjgl3.singleFile}")
    })
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "lwjgl2-compat"
}
