plugins {
    id("flagon.published")
}

dependencies {
    api(project(":lwjgl-core"))
    testImplementation(project(path = ":lwjgl-core", configuration = "testFixtures"))
}
