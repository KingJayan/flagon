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

In `bundle-browser/pom.xml`, include your application and Flagon's runtime modules.

> **Important:** Your desktop LWJGL dependencies must be marked as `<scope>provided</scope>` or excluded in `bundle-browser`. Native desktop LWJGL classes will shadow Flagon's compatibility layer and break compilation.

```xml
<dependencies>
    <!-- Flagon Core -->
    <dependency>
        <groupId>dev.flagon</groupId>
        <artifactId>runtime</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.flagon</groupId>
        <artifactId>platform-browser</artifactId>
    </dependency>

    <!-- LWJGL Compat (Choose lwjgl2-compat or lwjgl3-compat) -->
    <dependency>
        <groupId>dev.flagon</groupId>
        <artifactId>lwjgl2-compat</artifactId>
    </dependency>

    <!-- Optional Classpath Extensions -->
    <!-- Add if your app uses java.net.Socket -->
    <dependency>
        <groupId>dev.flagon</groupId>
        <artifactId>socket-classlib</artifactId>
    </dependency>
    <!-- Add if your app uses reflection over its own members -->
    <dependency>
        <groupId>dev.flagon</groupId>
        <artifactId>reflect-policy</artifactId>
    </dependency>

    <!-- Your Desktop Application -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>your-app</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.lwjgl.lwjgl</groupId>
                <artifactId>*</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
</dependencies>

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

If inheriting from `flagon-parent`, declare these properties in `bundle-browser/pom.xml`:

```xml
<properties>
    <flagon.bundle.mainClass>com.example.BrowserMain</flagon.bundle.mainClass>
    <flagon.bundle.name>app</flagon.bundle.name>
    <flagon.bundleOutput>${project.build.directory}/generated-bundle</flagon.bundleOutput>
    <!-- Keep strict enabled to preserve JVM NullPointerException semantics -->
    <flagon.strict>true</flagon.strict>
</properties>

```

*Optional Program Catalog:* To generate an `app.catalog.json` metadata manifest for multi-app pickers prior to runtime boot, point `flagon.bundle.catalogClass` to a class executing `ProgramCatalog.writeTo(registry, args[0])`.

### 4. Compile

Compile the WebAssembly GC bundle (default):

```bash
./mvnw package

```

*Outputs: `app.wasm` and bootstrap loader scripts.*

Compile the JavaScript fallback bundle:

```bash
./mvnw package -Pjs

```

*Outputs: `app.js`.*

Ship both artifacts for complete browser support. Flagon's web shell automatically handles feature detection and runs WASM GC when available.

---

## Artifact Repository

Pre-built releases are hosted on GitHub Packages.

Add the repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>flagon</id>
        <url>https://maven.pkg.github.com/KingJayan/flagon</url>
    </repository>
</repositories>

```

Authenticate in `~/.m2/settings.xml` using a GitHub Personal Access Token with `read:packages` permissions:

```xml
<servers>
    <server>
        <id>flagon</id>
        <username>GITHUB_USERNAME</username>
        <password>GITHUB_TOKEN</password>
    </server>
</servers>

```

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
| `flagon.bundle.*` | Maven POM configuration properties |
| `@Reflectable`, `flagon.reflect.*` | Reflection preservation controls |

All unlisted APIs are internal implementation details subject to change.
