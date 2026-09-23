plugins {
    id("flagon.bundle")
}

dependencies {
    implementation(project(":runtime"))
    implementation(project(":platform-browser"))

    implementation(project(":lwjgl2-compat"))
    implementation(project(":lwjgl3-compat"))
    implementation(project(":example-app"))
    implementation(project(":thirdparty-app"))
}

bundle {
    mainClass.set("dev.flagon.example.ExampleMain")
    catalogClass.set("dev.flagon.example.ExampleCatalog")
}
