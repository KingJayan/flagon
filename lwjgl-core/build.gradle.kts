plugins {
    id("flagon.published")
}

dependencies {
    api(project(":platform-api"))
}

val testFixtures = configurations.register("testFixtures") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val testJar = tasks.register<Jar>("testJar") {
    archiveClassifier.set("tests")
    from(sourceSets["test"].output)
}

artifacts {
    add(testFixtures.name, testJar)
}

publishing.publications.named<MavenPublication>("maven") {
    artifact(testJar)
}
