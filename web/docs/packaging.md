# Packaging

A browser cannot load a JAR at run time. The build links the application, its libraries and Flagon into one bundle ahead of time.

There are two ways to make a bundle:

- **`vendor/`**: put the JARs in a folder and run `make app`. No build files.
- **A bundle module**: a Gradle module in this repository that applies the `flagon.bundle` plugin. Use it for several programs, your own entry point or custom settings.

Both ways need this repository. The patched TeaVM and the `flagon.bundle` plugin are not published yet ([#570](https://github.com/KingJayan/flagon/issues/570)).

## From vendor/

1. Put the application JAR and its runtime dependencies in `vendor/`. Leave out the desktop LWJGL JARs and natives: Flagon supplies `org.lwjgl.*`.
2. Run `make app`.
3. Run `make dev` and open `/play/`.

The build:

1. Merges the JARs in file name order. For a duplicate class, the first JAR wins.
2. Finds the entry point from the `Main-Class` manifest header, or from a class with a `main` method.
3. Writes a direct entry point, so the call graph survives tree shaking.
4. Compiles the `app` bundle for every backend.

### Files beside the JARs

Folders in `vendor/` ship with the bundle. The program reads their files by path from the working directory `/`. They are read-only. See [Shipped files](compatibility.md#shipped-files).

### vendor/preserve.txt and vendor/reflect.txt

Some libraries find their own classes by scanning the classpath. Log4j 2 finds its pattern converters this way. List the packages such a library scans in `vendor/preserve.txt`.

A library that calls methods or reads fields by name needs its classes in `vendor/reflect.txt`.

Both files take one glob per line. `*` stops at a package boundary and `**` does not. A line that starts with `#` is a comment.

```text
# vendor/reflect.txt
com.example.config.*
com.example.model.**
```

### Constructors

`make app` keeps the constructors of every class in the JARs, so `Class.newInstance` and `Constructor.newInstance` work. It logs the count. It warns past 10,000 constructors and stops past 50,000. Narrow it to your packages:

```bash
./gradlew bundles "-Pflagon.app.reflect=com.example.**"
```

### Audio

`make app` re-encodes Ogg Vorbis files at quality 0 through `ffmpeg`. It keeps the original where the new file saves less than 15%. Files of 256 KiB or more, such as music, load after the program starts.

### Minecraft

`make minecraft` fills `vendor/` from a local launcher install: the game, the libraries that run in a browser, the assets in the launcher's layout, and both glob files.

```bash
make minecraft MINECRAFT=1.8.9 LAUNCHER=~/.minecraft
make app
```

## Bundle modules

A bundle module is a Gradle module that applies `flagon.bundle`. `examples/bundle` is a complete example.

### 1. Add the module

Add the module to `settings.gradle.kts`, and add its path to `bundleProjects` in the root `build.gradle.kts` so `make bundles` copies its output into the site.

```kotlin
// your-bundle/build.gradle.kts
plugins {
    id("flagon.bundle")
}

dependencies {
    implementation(project(":runtime"))
    implementation(project(":platform-browser"))
    implementation(project(":lwjgl2-compat")) // or :lwjgl3-compat, or both

    implementation("com.example:your-app:1.0.0") {
        exclude(group = "org.lwjgl")
    }
}

bundle {
    mainClass.set("com.example.BrowserMain")
    catalogClass.set("com.example.BrowserCatalog")
}
```

The desktop LWJGL JARs must not be on the bundle's classpath. They hide Flagon's `org.lwjgl.*` classes and break the build.

Add the class libraries the application needs:

| Module | Adds |
| --- | --- |
| `:toolchain:jdk-shims` | Reroutes JDK calls that TeaVM lacks. Add it to every application bundle. |
| `:toolchain:jdk-classlib` | Java serialization, `java.lang.management`, `javax.xml.parsers`, `java.sql` dates and small stubs. |
| `:toolchain:concurrent-classlib` | `java.util.concurrent` on green threads. |
| `:toolchain:socket-classlib` | `java.net.Socket` and `InetAddress` through the socket proxy. |
| `:toolchain:nio-classlib` | `FileChannel`, `Selector` and the NIO channel types. |
| `:toolchain:awt-classlib` | `BufferedImage`, `ImageIO` (PNG) and the clipboard. |
| `:toolchain:crypto-classlib` | `MessageDigest`, `Cipher`, `Signature` and RSA keys. |
| `:toolchain:sound-classlib` | `javax.sound` types that report no device. |
| `:toolchain:reflect-policy` | `@Reflectable` and the reflection policy. |

### 2. Write the entry point

The entry point registers each program and boots the runtime. Use method references, so the compiler sees the call graph.

```java
package com.example;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.MainProgram;
import dev.flagon.runtime.ProgramRegistry;

public final class BrowserMain {
    public static void main(String[] arguments) {
        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry());
    }

    public static ProgramRegistry registry() {
        return new ProgramRegistry()
                .register(new MainProgram("game", "the game", Game::main))
                .register(new MainProgram("editor", "the level editor", Editor::main));
    }
}
```

The page picks a program with `?program=`. With no program, the runtime runs the first one. The rest of the arguments go to the program.

### 3. Write the catalog

The catalog lists the bundle's programs for the page. The build runs the catalog class on the JVM, never in the page.

```java
public final class BrowserCatalog {
    public static void main(String[] arguments) throws IOException {
        ProgramCatalog.writeTo(BrowserMain.registry(), arguments[0]);
    }
}
```

### Settings

| Setting | Default | Use |
| --- | --- | --- |
| `bundleName` | the module name without `bundle-` | The output file names. |
| `mainClass` | none | The entry point. Required. |
| `catalogClass` | none | Writes `<name>.catalog.json`. |
| `resourcesFrom` | the module's resources | The folder the resource pack is made from. |
| `reflectClasses` | empty | Globs of classes that keep their members. Same as `flagon.reflect.classes`. |
| `reflectProxies` | empty | Globs of interfaces that `Proxy.newProxyInstance` implements. |
| `reflectConverted` | empty | Classes that keep `valueOf(String)` and their `(String)` constructor, for option parsers. |
| `compilerMemory` | 1024 | The TeaVM heap in MB. |
| `vorbisQuality` | unset | Re-encodes Ogg Vorbis at this quality. Unset keeps the files. |
| `apartFrom` | 0 | Resources this size in bytes or larger load beside the pack. 0 keeps all of them in it. |
| `shrinkCoroutineWasm` | true | Runs `wasm-opt` on the coroutine WebAssembly in release builds. |

### Gradle properties

| Property | Use |
| --- | --- |
| `-Pflagon.release` | Release build: optimized, obfuscated, shrunk with `wasm-opt`. |
| `-Pflagon.preserve=globs` | Classes that only a classpath scan names. Defaults to `vendor/preserve.txt`. |
| `-Pflagon.app.reflect=globs` | Narrows the constructors `make app` keeps. |

## Output

Each bundle writes these files to `build/flagon/`. `make bundles` copies them to `web/public/flagon/`.

| File | Contents |
| --- | --- |
| `wasm/<name>.wasm` | WebAssembly GC. Pauses with a coroutine rewrite. |
| `jspi/<name>.wasm` | WebAssembly GC. Pauses with JSPI, so it is smaller and faster. |
| `js/<name>.js` | JavaScript, for browsers without WebAssembly GC. |
| `<name>.pack` | The resource pack: every non-class file, gzipped. |
| `<name>.files/` | Resources of `apartFrom` size or larger, named by SHA-256. |
| `<name>.catalog.json` | The program list, when `catalogClass` is set. |

Serve all of them. The page picks the backend at load time.

## Deploy

The output is static. Any static host serves it. A program that opens sockets also needs the socket proxy.

### Socket proxy

A page cannot open TCP connections. Flagon sends each socket over a WebSocket to `/__flagon/tcp` on the page's origin. The proxy opens the TCP or TLS connection.

`make dev` includes a proxy that reaches only loopback addresses. For production, serve the site and the proxy together:

```bash
make build-site
cd web && bun run proxy -- --root dist --host 0.0.0.0 --port 8080 \
  --token "$FLAGON_PROXY_TOKEN" \
  --allow game.example.com:25565
```

Open the page with `?proxytoken=<token>`.

| Option | Default | Use |
| --- | --- | --- |
| `--root` | `dist` | The folder to serve. |
| `--host` | `127.0.0.1` | The bind address. A non-loopback address needs `--token`. |
| `--port` | `8080` | The port. |
| `--token` | none | The token each connection must send. |
| `--allow` | `localhost:*`, `127.0.0.1:*`, `[::1]:*` | Destinations as `host:port`. `*` matches any port. Repeat or separate with commas. |
| `--origin` | the page's own origin | Extra origins that may connect. |
| `--allowed-host` | the bind address | Extra `Host` header values to accept. |
| `--tls-ca` | none | Extra trusted CA certificates (PEM) for TLS destinations. |

### Embedding

A host page loads a bundle and sets `window.__flagonHost` to receive its events. See [Host bridge](architecture.md#host-bridge).

## Stable API

These are stable for downstream use. Everything else may change.

| Surface | Use |
| --- | --- |
| `org.lwjgl.*` | The LWJGL 2 and LWJGL 3 APIs Flagon supports. |
| `FlagonRuntime.boot` | Starts the runtime. |
| `Program`, `ProgramRegistry`, `MainProgram`, `ProgramCatalog` | Registers programs. |
| `BrowserPlatform.create` | Makes the browser platform. |
| `@Reflectable`, `flagon.reflect.*` | Controls what reflection keeps. |
| `window.__flagonHost` | The host page bridge. |
| `dev.flagon:*` | Published module coordinates. |
