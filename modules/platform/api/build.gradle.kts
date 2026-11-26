plugins {
    id("flagon.published")
}

dependencies {
    compileOnly("dev.flagon:teavm-core-patched:${project.version}")
    testImplementation("dev.flagon:teavm-core-patched:${project.version}")
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "platform-api"
}
