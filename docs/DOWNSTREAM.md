# Downstream Applications

Flagon compiles existing desktop Java and LWJGL applications into static browser bundles using ahead-of-time (AOT) compilation via TeaVM.

Because browsers cannot dynamically load JARs at runtime, your application code, dependencies, and Flagon's compatibility layer must be linked ahead of time into a single bundle.

---

## Quickstart (no setup)

To test an existing JAR immediately without configuring a build module:

1. Drop your application JAR (and any runtime dependencies) into the `vendor/` directory of this repo.
2. Run:
```bash
make app

```


3. Open `/play/` in your browser

```bash
cd web
bun dev # local vite on localhost:5174
```

`make app` re-encodes Ogg Vorbis audio through `ffmpeg`, so a JAR with audio needs ffmpeg installed.

Some libraries scan the classpath for their own parts. Log4j 2 finds its pattern converters this way. List the packages such a library scans in `vendor/preserve.txt`, one glob per line (`*` stops at a package boundary, `**` does not), and the build keeps them. A library that calls methods or reads fields by name needs its classes in `vendor/reflect.txt`, in the same form.

For Minecraft, `make minecraft` fills `vendor/` from a local launcher install: the game, the libraries that run in a browser, its assets and both lists. `make minecraft MINECRAFT=1.8.9 LAUNCHER=path` picks another version or install.

The build resolves collisions by sorting filenames alphabetically (the first occurrence of a duplicate class takes priority), identifies the entry point via the JAR's `Main-Class` manifest header (or classes with a `main` method), generates a direct bootstrap entry point, and compiles both `app.wasm` and `app.js`.

---

## Production Integration

For custom integration, multi-program launchers, or production deployment, structure your repository into two submodules:

```
your-project/
├── app/                 # Your unmodified desktop Java/LWJGL application
└── bundle-browser/      # Browser entry point and TeaVM compilation target

```

### 1. Configure Dependencies

In `bundle-browser/build.gradle.kts`, include your application and Flagon's runtime modules.

> **Important:** Your desktop LWJGL dependencies must be `compileOnly` or excluded in `bundle-browser`. Native desktop LWJGL classes will shadow Flagon's compatibility layer and break compilation.

```kotlin
dependencies {
    // Flagon Core
    implementation("dev.flagon:runtime:0.9.3")
    implementation("dev.flagon:platform-browser:0.9.3")

    // LWJGL Compat (Choose lwjgl2-compat or lwjgl3-compat)
    implementation("dev.flagon:lwjgl2-compat:0.9.3")

    // Optional Classpath Extensions
    // Add if your app uses java.net.Socket
    implementation("dev.flagon:socket-classlib:0.9.3")
    // Add if your app uses reflection over its own members
    implementation("dev.flagon:reflect-policy:0.9.3")

    // Your Desktop Application
    implementation("com.example:your-app:1.0.0") {
        exclude(group = "org.lwjgl.lwjgl")
    }
}

```

### 2. Define the Entry Point

Create a browser entry point in `bundle-browser`. Explicit method references ensure your application’s entry point and call graph survive AOT tree-shaking.

```java
package com.example;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.MainProgram;
import dev.flagon.runtime.ProgramRegistry;

public final class BrowserMain {
    public static void main(String[] args) {
        ProgramRegistry registry = new ProgramRegistry()
                .register(new MainProgram(
                        "app",
                        "Your Application",
                        YourApplication::main));

        FlagonRuntime.boot(
                BrowserPlatform.create(args),
                registry);
    }
}

```

### 3. Build Configuration

Apply the TeaVM Gradle plugin and point it at your entry point:

```kotlin
plugins {
    java
    id("org.teavm") version "0.15.0"
}

teavm {
    all {
        mainClass.set("com.example.BrowserMain")
        outputDir.set(layout.buildDirectory.dir("generated-bundle"))
    }
    js {
        targetFileName.set("app.js")
        moduleType.set(org.teavm.gradle.api.JSModuleType.ES2015)
        // Keep strict enabled to preserve JVM NullPointerException semantics
        strict.set(true)
    }
    wasmGC {
        targetFileName.set("app.wasm")
        strict.set(true)
    }
}

```

*Optional Program Catalog:* To generate an `app.catalog.json` metadata manifest for multi-app pickers prior to runtime boot, run a class executing `ProgramCatalog.writeTo(registry, args[0])` from a `JavaExec` task.

### 4. Compile

Compile the bundles:

```bash
./gradlew generateWasmGC generateJavaScript

```

*Outputs: `app.wasm`, `app.js` and bootstrap loader scripts. One build emits both backends.*

Ship both artifacts for complete browser support. Flagon's web shell automatically handles feature detection and runs WASM GC when available.

---

## Artifact Repository

Pre-built releases are hosted on GitHub Packages.

Add the repository to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/KingJayan/flagon")
        credentials {
            username = providers.gradleProperty("githubUser").get()
            password = providers.gradleProperty("githubToken").get()
        }
    }
}

```

The token is a GitHub Personal Access Token with `read:packages` permissions. Keep both values in
`~/.gradle/gradle.properties`, never in the repository.

---

## Stable API Surface

The following APIs and properties are stable for downstream consumers:

| Surface | Description |
| --- | --- |
| `org.lwjgl.*` | Supported LWJGL 2 and LWJGL 3 emulation APIs |
| `FlagonRuntime.boot` | Runtime initialization hook |
| `Program`, `ProgramRegistry`, `MainProgram` | Application lifecycle registration |
| `BrowserPlatform.create` | Platform binding factory |
| `window.__flagonHost` | Browser host coordination interface |
| `dev.flagon:*` | Published module coordinates |
| `@Reflectable`, `flagon.reflect.*` | Reflection preservation controls |

All unlisted APIs are internal implementation details subject to change.
