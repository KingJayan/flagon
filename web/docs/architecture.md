# Architecture

How Flagon compiles a program and runs it in a page.

## Layers

Each layer uses only the layer under it.

```text
application JAR, unmodified
        │  compiled ahead of time
lwjgl2-compat · lwjgl3-compat     org.lwjgl.* APIs
        │
lwjgl-core                        GL and AL state, fixed-function pipeline, GLSL translator
        │
platform-api                      host contracts: graphics, audio, files, input, network, clock
        │
platform-browser                  WebGL 2, Web Audio, OPFS, DOM events, WebSocket tunnel
```

### Rules

1. **Host isolation.** Only `platform-browser` uses browser APIs. No other module names `window`, `document` or WebGL.
2. **JVM tests.** `lwjgl-core` and the compat layers use only `platform-api`. Their tests run on a desktop JVM with recording and in-memory devices: `RecordingGraphicsDevice`, `SilentAudioDevice`, `MemoryFileStore` and `MemoryNetwork`.
3. **Static access.** LWJGL calls are static, such as `Display.create()` and `GL11.glClear()`. The runtime installs the active `Platform` in `Platforms` at boot, and static code reads it from there.

## Modules

### Runtime

| Module | Role |
| --- | --- |
| `platform-api` | Host contracts: `Platform`, `GraphicsDevice`, `DisplaySurface`, `AudioDevice`, `FileStore`, `InputSource`, `Network`, `Clock`, `RuntimeHost`. Also the JDK reroute targets. |
| `platform-browser` | Implements `platform-api` in the browser. |
| `runtime` | Boots a platform, picks a program, runs it, and reports to the host. Installs `System.out`, URL handlers and shutdown hooks. |
| `lwjgl-core` | GL and AL state machines, immediate mode, display lists, the fixed-function shader generator and the GLSL translator. No `org.lwjgl` classes. |
| `lwjgl2-compat` | The LWJGL 2 API. |
| `lwjgl3-compat` | The LWJGL 3 API. |

### Class libraries

Each adds JDK classes that TeaVM lacks.

| Module | Adds |
| --- | --- |
| `toolchain/jdk-classlib` | Java serialization, `RecordComponent`, `Parameter`, `java.lang.management`, `javax.xml.parsers` with a DOM, `java.sql` dates, `javax.naming` and small stubs. |
| `toolchain/concurrent-classlib` | `java.util.concurrent` executors, futures, locks and queues on green threads. |
| `toolchain/socket-classlib` | `java.net` sockets and addresses over the socket proxy. |
| `toolchain/http-classlib` | `java.net.http` over the page's `fetch`. |
| `toolchain/nio-classlib` | `FileChannel`, `Selector` and the channel types. |
| `toolchain/awt-classlib` | `Graphics2D` on an `OffscreenCanvas`, `java.awt.geom`, `BufferedImage`, `ImageIO` and an in-page clipboard. |
| `toolchain/crypto-classlib` | Digests, HMAC, PBKDF2, AES, RSA, EC keys, ECDSA and ECDH. |
| `toolchain/sound-classlib` | `javax.sound` types with no devices. |

### Build tools

| Module | Role |
| --- | --- |
| `toolchain/jdk-shims` | TeaVM class transformers. They reroute JDK calls that TeaVM lacks to `platform-api`, and write the class-file facts TeaVM drops (generic signatures, record components, parameter names, default `serialVersionUID`) into each reflectable class. |
| `toolchain/reflect-policy` | `@Reflectable` and the compiler plugin that decides which classes keep their members. |
| `toolchain/app-entry` | Reads the JARs in `vendor/` and writes the `app` bundle's entry point. |
| `toolchain/classfile` | A constant-pool reader. It tells whether a classpath has every class and method a class names. |
| `toolchain/resource-pack` | Writes `<bundle>.pack` and `<bundle>.files/`, and re-encodes Vorbis. |
| `teavm` | `teavm-core` 0.15.0 rebuilt with compiler and class library fixes. |
| `teavm-jso` | `teavm-jso-impl` 0.15.0 rebuilt so a simple `@JSBody` compiles to backend operations, not text. |
| `teavm-patches` | Builds `teavm` and `teavm-jso` before the main build. The TeaVM plugin loads them in place of the stock JARs. |
| `build-logic` | The Gradle convention plugins, including `flagon.bundle`. |

### Bundles and examples

| Module | Contents |
| --- | --- |
| `bundle-sandbox` | The self-tests the browser tests run, and the LWJGL demos. |
| `bundle-pausing` | A program that pauses during class setup, for the coroutine backend. |
| `examples/app`, `examples/thirdparty` | Sample LWJGL 2 programs and LWJGL's own LWJGL 3 sample, built against the real LWJGL. |
| `examples/bundle` | The `example` bundle of those programs. |
| `bundle-app` | The `app` bundle from `vendor/`. Present when `vendor/` has JARs. |
| `playground` | A separate build. Compiles javac and TeaVM 0.17 to WebAssembly for the playground. |
| `web` | The site, the bundle loader, the socket proxy and the Playwright tests. |

## Build pipeline

TeaVM compiles the bytecode ahead of time. The page has no JVM, class loader or JIT.

```text
bytecode (.class, .jar)
  │
  ├─ jdk-shims         reroute JDK calls, write reflection data
  ├─ reflect-policy    choose what reflection keeps
  ▼
TeaVM (patched 0.15.0)
  ├─ reachability      remove code nothing calls
  ├─ async transform   let threads pause
  │
  ├─► wasm/<name>.wasm   WebAssembly GC, coroutine pauses
  ├─► jspi/<name>.wasm   WebAssembly GC, JSPI pauses
  └─► js/<name>.js       JavaScript
resources ─► <name>.pack, <name>.files/
```

### Backends

- **WebAssembly GC.** Java objects and arrays live in the GC heap. `JsArrays` copies data out when it crosses into browser APIs, such as a WebGL upload.
- **JSPI.** The same program without the coroutine rewrite. `WebAssembly.Suspending` parks the whole stack, so the code is smaller and faster.
- **JavaScript.** Java arrays are typed arrays and cross into browser APIs without a copy.

### Pausing

Browser APIs are asynchronous, and Java code expects blocking calls. TeaVM finds every method that can reach an asynchronous call and rewrites it to save and restore its frame. A virtual call is asynchronous when any override can pause, so one pausing override makes every caller of that method pausable. That makes the coroutine bundle bigger. The JSPI backend skips the rewrite.

## Subsystems

### Graphics

- `GLState` tracks the whole GL state and applies only the differences before each draw.
- Programs switch between fixed-function shaders and application shaders at `glUseProgram`.
- Immediate mode, client arrays and display lists go into stream buffers.
- `FixedFunctionPipeline` generates a shader for each combination of lighting, texturing, fog and alpha test in use.
- `GlslTranslator` parses desktop GLSL 1.10 to 3.30 and writes GLSL ES 1.00 or 3.00.

### Threads

`concurrent-classlib` runs threads as green threads on the page's thread. A thread pauses at I/O, `Thread.sleep`, `Object.wait`, a held monitor or lock, and at the end of each frame. Code that never pauses holds the page until it returns.

### Files

`OpfsFileStore` holds the files in OPFS. `StoreFileSystem` puts `java.io` on any `FileStore`. A file loads into memory when it opens and writes back on `flush` and `close`. `RandomAccessFile` writes through in `rwd` and `rws` modes.

### Audio

`WebAudioDevice` maps OpenAL buffers and sources to Web Audio nodes. `ALState` holds the OpenAL state. Each source has a `PannerNode`. Doppler sets the playback rate, because Web Audio removed its own.

### Network

`ProxyNetwork` opens one WebSocket to the socket proxy per connection. `NetworkSocket` gives `java.net.Socket` its stream. The proxy makes TLS connections for `https` URLs, through a `URLStreamHandlerFactory` the runtime installs.

### Input

DOM events go into bounded queues. `Frames.awaitNextFrame()` drains them once per frame. Keys map from `KeyboardEvent.code`. Mouse Y is flipped to GL's origin.

## Reflection policy

The compiler removes members nothing calls by name. These settings keep them:

| Need | Setting | Keeps |
| --- | --- | --- |
| Class identity | none | `getClass`, `isInstance`, `cast`, `forName` for kept classes. |
| Members of a class | `@Reflectable`, `flagon.reflect.classes` | Fields, methods, constructors and their metadata. |
| Members in `vendor/` | `vendor/reflect.txt` | Adds its globs to `flagon.reflect.classes`. |
| Proxies | `flagon.reflect.proxies` | A proxy class made at compile time. |
| Constructors in `vendor/` | `META-INF/flagon/constructed.txt` | Constructors. `-Pflagon.app.reflect` narrows them. |
| Classpath scans | `flagon.preserve`, `vendor/preserve.txt` | Classes that only a scan names. |
| Serialization | `Serializable` | Fields, the serial methods and a constructor that skips the serializable ones. |

There is no bytecode in the page. Transform the JARs before the build.

## Host bridge

The page sets `window.__flagonHost` before it starts a bundle. The runtime reports to it:

```ts
interface HostBridge {
  log(level: string, text: string): void;
  catalog(ids: string[], descriptions: string[]): void;
  status(phase: string, detail: string): void;
  ready(): void;
  exited(code: number): void;
  fatal(phase: string, message: string, stack: string): void;
}
```

| Call | When |
| --- | --- |
| `log` | Each console line. `level` is `DEBUG`, `INFO`, `WARN` or `ERROR`. |
| `catalog` | At boot, with the bundle's programs. |
| `status` | At each phase: `platform`, `select`, `program`. |
| `ready` | Just before the program's `main`. |
| `exited` | When the program ends or calls `System.exit`. |
| `fatal` | When a phase throws. |

Without `window.__flagonHost`, the runtime writes to `console`.
