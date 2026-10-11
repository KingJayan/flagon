package dev.flagon.build

import org.gradle.api.file.FileTree

/** a patch replaces a stock file, and every class nested in it */
fun replaces(path: String, java: FileTree, resources: FileTree): Boolean {
    var hit = false
    java.visit {
        val name = relativePath.pathString.removeSuffix(".java")
        hit = hit || path == "$name.class" || path.startsWith("$name$")
    }
    resources.visit { hit = hit || relativePath.pathString == path }
    return hit
}
