package dev.flagon.build

val WASM_FEATURES = listOf(
    "--enable-gc",
    "--enable-reference-types",
    "--enable-exception-handling",
    "--enable-multivalue",
    "--enable-bulk-memory",
    "--enable-sign-ext",
    "--enable-nontrapping-float-to-int",
    "--enable-mutable-globals",
)
