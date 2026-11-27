plugins {
    id("flagon.published")
}

description = "JSO bindings for WebCodecs, WebTransport, WebRTC, Web Bluetooth, WebUSB and IndexedDB"

dependencies {
    api(libs.teavm.jso)
    api(libs.teavm.jso.apis)
    api(libs.teavm.interop)
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "platform-web"
}
