plugins {
    id("flagon.published")
}

dependencies {
    api(project(":platform-api"))
}

val testFixtures by configurations.registering {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val testJar by tasks.registering(Jar::class) {
    archiveClassifier.set("tests")
    from(sourceSets["test"].output)
}

artifacts {
    add(testFixtures.name, testJar)
}

publishing.publications.named<MavenPublication>("maven") {
    artifact(testJar)
}
