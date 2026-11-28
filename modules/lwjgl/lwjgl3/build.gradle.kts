plugins {
    id("flagon.published")
}

// the real gl es classes, which the surface test reads and never loads
val realGles = configurations.create("realGles") { isTransitive = false }

dependencies {
    api(project(":lwjgl:core"))
    testImplementation(project(path = ":lwjgl:core", configuration = "testFixtures"))
    realGles(libs.lwjgl3.opengles)
}

tasks.named<Test>("test") {
    val gles = files(realGles)
    inputs.files(gles).withPropertyName("realGles").withNormalizer(ClasspathNormalizer::class)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dflagon.opengles=${gles.singleFile}")
    })
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "lwjgl3-compat"
}
