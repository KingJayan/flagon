package dev.flagon.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.security.MessageDigest

/** the wasmtime release whose preview 1 adapter turns a core module into a preview 2 component */
const val WASI_ADAPTER_URL = "https://github.com/bytecodealliance/wasmtime/releases/download/" +
    "v51.0.0/wasi_snapshot_preview1.command.wasm"
const val WASI_ADAPTER_SHA256 = "4e0172e3d210cd5080c08cdd1708cf272a5ea731b1121329c3e56705f5c6f91c"

/** fetches a file once and refuses it unless its sha-256 is the one expected */
@CacheableTask
abstract class DownloadFile : DefaultTask() {

    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val sha256: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun download() {
        val bytes = URI(url.get()).toURL().openStream().use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        check(digest == sha256.get()) {
            "${url.get()} has sha-256 $digest, not the ${sha256.get()} this build trusts"
        }
        output.get().asFile.writeBytes(bytes)
    }
}

/** @return wasm-tools from -Pflagon.wasmTools or the PATH, or an error saying how to get it */
fun wasmTools(configured: String?): String =
    tool("wasm-tools", configured, "flagon.wasmTools", "https://github.com/bytecodealliance/wasm-tools")

/** @return wasmtime from -Pflagon.wasmtime or the PATH, or an error saying how to get it */
fun wasmtime(configured: String?): String =
    tool("wasmtime", configured, "flagon.wasmtime", "https://wasmtime.dev")

private fun tool(name: String, configured: String?, property: String, home: String): String {
    if (configured != null) {
        return configured
    }
    val found = System.getenv("PATH").orEmpty().split(File.pathSeparator)
        .map { File(it, name) }.firstOrNull { it.canExecute() }
    return checkNotNull(found) {
        "a wasi component needs $name; install it from $home or pass -P$property=<path>"
    }.absolutePath
}
