plugins {
    id("flagon.java")
}

dependencies {
    compileOnly(libs.lwjgl3)
    compileOnly(libs.lwjgl3.glfw)
    compileOnly(libs.lwjgl3.opengl)
}

tasks.withType<Checkstyle>().configureEach {
    enabled = false
}
