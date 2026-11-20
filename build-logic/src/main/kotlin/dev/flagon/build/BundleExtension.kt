package dev.flagon.build

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

interface BundleExtension {
    val bundleName: Property<String>

    val mainClass: Property<String>

    val catalogClass: Property<String>

    val resourcesFrom: DirectoryProperty

    val reflectClasses: Property<String>

    val reflectProxies: Property<String>

    val reflectConverted: Property<String>

    val compilerMemory: Property<Int>

    /** release wasm-opt on the coroutine build; binaryen crashes on the app's */
    val shrinkCoroutineWasm: Property<Boolean>
}
