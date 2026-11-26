plugins {
    id("flagon.classlib")
    id("flagon.published")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("--limit-modules", "java.base"))
}

dependencies {
    api(project(":platform-api"))
}
