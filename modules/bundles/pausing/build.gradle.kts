plugins {
    id("flagon.bundle")
}

dependencies {
    implementation(project(":runtime"))
    implementation(project(":platform:browser"))
}

bundle {
    mainClass.set("dev.flagon.pausing.PausingMain")
    catalogClass.set("dev.flagon.pausing.PausingCatalog")
}
