plugins {
    id("flagon.bundle")
}

dependencies {
    implementation(project(":runtime"))
    implementation(project(":platform:browser"))

    implementation(project(":lwjgl:lwjgl2"))
    implementation(project(":lwjgl:lwjgl3"))
    implementation(project(":example-app"))
    implementation(project(":thirdparty-app"))
}

bundle {
    mainClass.set("dev.flagon.example.ExampleMain")
    catalogClass.set("dev.flagon.example.ExampleCatalog")
}
