package dev.flagon.build

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

interface BundleExtension {
    val bundleName: Property<String>

    val mainClass: Property<String>

    /** the main class of the wasi component the wasi task builds; mainClass when unset */
    val wasiMainClass: Property<String>

    val catalogClass: Property<String>

    val resourcesFrom: DirectoryProperty

    val reflectClasses: Property<String>

    val reflectProxies: Property<String>

    val reflectConverted: Property<String>

    val compilerMemory: Property<Int>

    /** re-encodes the pack's ogg vorbis at this quality through ffmpeg; unset keeps it */
    val vorbisQuality: Property<Int>

    /** resources this many bytes or larger load from beside the pack when read; 0 keeps all in */
    val apartFrom: Property<Int>

    /** release wasm-opt on the coroutine build; binaryen crashes on the app's */
    val shrinkCoroutineWasm: Property<Boolean>
}
