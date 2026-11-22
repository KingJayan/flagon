# ArchitectureDesign

This document details Flagon's compilation pipeline, layered architecture, module functinoalities, and system contracts.

---

## Hierarchy

The codebase is organized into strict layers. Higher layers depend solely on immediate lower abstractions.

```
┌────────────────────────────────────────────────────────┐
│              Application JAR (Unmodified)              │
└───────────────────────────┬────────────────────────────┘
                            │ Ahead-of-Time Compilation
┌───────────────────────────▼────────────────────────────┐
│         lwjgl2-compat   │   lwjgl3-compat              │  Source-compatible org.lwjgl.* APIs
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                      lwjgl-core                        │  Shared state machines, pipeline emulators, GLSL translation
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                     platform-api                       │  Abstract host contracts (Graphics, Audio, FS, Input, Net)
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                   platform-browser                     │  Browser implementation (WebGL2, Web Audio, OPFS, DOM)
└────────────────────────────────────────────────────────┘

```

### Invariant Rules

1. **Host Isolation:** No module above `platform-browser` may import, mention, or reference Web APIs or browser globals (`window`, `document`, WebGL, etc.).
2. **Deterministic JVM Testability:** `lwjgl-core` and all compat layers target `platform-api` exclusively. They execute identically on a headless desktop JVM using mock or recording devices (`RecordingGraphicsDevice`, `SilentAudioDevice`, `MemoryFileStore`, `MemoryNetwork`).
3. **Static Access Compatibility:** LWJGL exposes static endpoints (e.g., `Display.create()`, `GL11.glClear()`). To avoid breaking source compatibility, the active `Platform` instance is registered as a static singleton (`Platforms`) during boot.

---

## Index

| Module | Role & Responsibility |
| --- | --- |
| `platform-api` | Defines host contracts: `Platform`, `GraphicsDevice`, `DisplaySurface`, `AudioDevice`, `FileStore`, `InputSource`, `Network`, and `Clock`. |
| `platform-browser` | Implements `platform-api` using DOM events, WebGL2, Web Audio, OPFS, and WebSocket tunnels. |
| `runtime` | Application lifecycle, entry-point registration (`ProgramRegistry`), argument passing, and shutdown hook coordination. |
| `lwjgl-core` | Implementation of OpenGL/OpenAL state tracking, fixed-function shaders, immediate mode buffering, and GLSL AST translation. Contains no `org.lwjgl.*` classes. |
| `lwjgl2-compat` | Emulates LWJGL 2 desktop APIs (`org.lwjgl.opengl.Display`, `GL11`–`GL20`, `Keyboard`, `Mouse`, `OpenAL`). |
| `lwjgl3-compat` | Emulates LWJGL 3 core-profile APIs (`org.lwjgl.glfw.GLFW`, `GL11C`–`GL33C`, `MemoryUtil`, `MemoryStack`). |
| `teavm` | Rebuilds `teavm-core` 0.15.0 with compiler fixes, some from later TeaVM commits and some that TeaVM does not have yet. It builds in `teavm-patches`, ahead of the main build, and the TeaVM plugin loads it in place of the stock jar. |
| `teavm-jso` | Rebuilds `teavm-jso-impl` 0.15.0 so a simple `@JSBody` compiles to backend operations, not to text. |
| `toolchain/reflect-policy` | Compiler plugin discovering classes that retain members and constructors during tree-shaking, including every serializable class. |
| `toolchain/socket-classlib` | `--patch-module` library supplying missing `java.net.Socket` and `InetAddress` types for TeaVM. |
| `toolchain/jdk-classlib` | Supplies JDK classes TeaVM lacks: java serialization (`ObjectOutputStream`, `ObjectInputStream`), `RecordComponent`, `Parameter`, and small stubs. |
| `toolchain/concurrent-classlib` | Supplies missing `java.util.concurrent` classes (executors, latches, locks) mapped to green threads. |
| `toolchain/jdk-shims` | Bytecode-rewriting shims rerouting unsupported JDK calls (`ManagementFactory`, `Runtime.maxMemory`) to runtime equivalents. Also writes the class-file facts TeaVM drops (generic signatures, record components, default `serialVersionUID`) into each reflectable class. |
| `toolchain/app-entry` | Pre-compilation analyzer generating explicit programmatic entry points from scanned `vendor/*.jar` files. |
| `toolchain/classfile` | Constant-pool reader deciding whether a classpath carries every class and method a class names. |

---

## AOT Pipeline

Flagon uses TeaVM to compile Java bytecode ahead of time into a static bundle. There is no JVM, classloader, or JIT at runtime.

```
Bytecode (.class / .jar)
  │
  ├── [toolchain/jdk-shims]        Rewrite unsupported JDK intrinsics
  ├── [toolchain/reflect-policy]  Apply member retention rules
  │
  ▼
TeaVM AOT Compiler                [teavm] 0.15.0 plus compiler fixes
  │
  ├── Reachability Graph Construction (Tree-shaking dead code)
  ├── Async / Continuation Suspension Transform (Green threading)
  │
  ├──► WebAssembly GC (.wasm)
  └──► Optimized JavaScript (.js)

```

### Backend Targets

* **WASM GC:** High-performance execution. Java arrays are in the internal Wasm GC heap. Copy-out barriers (`JsArrays`) stage data when crossing into browser-accessible linear memory or typed array buffers.
* **JavaScript:** Universal fallback. Java arrays map directly to JavaScript typed arrays.
* **Strict Checks (`flagon.strict`):** Compiler flag inserting explicit null dereference and array bounds verification. Ensures standard JVM `NullPointerException` and `ArrayIndexOutOfBoundsException` catch semantics work reliably.

---

## Subsystems

### 1. Graphics (`lwjgl-core`)

Because WebGL2 supports only modern programmable pipelines, legacy desktop OpenGL is emulated in `lwjgl-core`:

* **Pipeline Switcher:** `glUseProgram` toggles between the user shader pipeline and the internal `FixedFunctionPipeline`.
* **State Management (`GLState`):** Mirrors matrices, lighting configurations, polygon offsets, client array bindings, and viewport states in host-independent memory.
* **Immediate Mode Emulation:** `glBegin`/`glEnd` batches accumulate vertices, colors, and texture coordinates into a scratch memory staging buffer, uploading them as dynamic vertex buffer objects (VBOs) on draw completion.
* **GLSL Translation (`GlslTranslator`):** Translates desktop GLSL (1.10 through 3.30) to GLSL ES 1.00 or 3.00 at link time. Replaces fixed-function built-ins (`gl_ModelViewProjectionMatrix`, `gl_Vertex`, `ftransform()`) with internal uniforms and generic vertex attributes.

### 2. Cooperative Concurrency (`toolchain/concurrent-classlib`)

* **Execution Model:** Applications run on cooperative **green threads** on the browser main thread. Concurrency allows non-blocking asynchronous execution, not multi-core parallelism.
* **Suspension Semantics:** I/O operations, `Thread.sleep()`, `Object.wait()`, and lock acquisitions suspend the active green thread via TeaVM’s continuation transformation, allowing other green tasks and the browser frame loop to progress.
* **Page Blocking:** Compute-bound tasks that do not yield or suspend will monopolize the execution thread and drop display frames.

### 3. Filesystem (`platform-browser`)

* **Storage Provider:** `OpfsFileStore` integrates with the browser's Origin Private File System (OPFS).
* **Bridging:** Replaces TeaVM's in-memory storage with `StoreFileSystem`. Synchronous `java.io` calls suspend the calling green thread while waiting for asynchronous browser OPFS promises to settle.
* **Write Semantics:** Standard file streams buffer modifications in memory and commit on `flush()` or `close()`. `RandomAccessFile` operations force an immediate write-through.

### 4. Audio (`platform-browser`)

* **Engine:** `AudioDevice` maps OpenAL concepts onto the Web Audio API.
* **Context State:** `ALState` retains context-wide volume, listener orientation, and velocity values.
* **Doppler & Panning:** Panning and distance attenuation map to `PannerNode`. Doppler shifts are calculated explicitly along the listener-source velocity vector to adjust the source's `playbackRate`.
* **User Gesture Handling:** The browser `AudioContext` initializes suspended and resumes automatically on the first user mouse click or keystroke.

### 5. Sockets & Proxied Transport

* **WebSocket Tunnel:** `ProxyNetwork` implements `platform-api:Network`. Outbound TCP socket connections are tunneled through an external Node/Bun proxy over WebSockets.
* **Stream Contract:** `NetworkSocket` wraps raw WebSocket frames into a blocking Java `InputStream`/`OutputStream` pair. Reads suspend the calling green thread until packets arrive.
* **HTTP / TLS:** Proxies terminate external TLS connections for HTTPS/WSS endpoints. Unmodified `java.net.URL` and `HttpURLConnection` calls route through this tunnel via an installed `URLStreamHandlerFactory`.

### 6. Inputs

* **Polling Architecture:** Inbound DOM input events (`keydown`, `keyup`, `mousemove`, `mousedown`) are buffered in thread-safe, bounded queues rather than dispatched via callbacks.
* **Synchronization:** Input queues are consumed synchronously during the game loop inside `Frames.awaitNextFrame()`, eliminating concurrency races between DOM events and game logic.
* **Layout Normalization:** Scancodes are mapped to desktop layout codes via `KeyboardEvent.code`. Y coordinates are inverted to match OpenGL window space conventions.

---

## Reflection Policy

Because TeaVM relies on whole-program reachability analysis, unreferenced members are stripped by default:

```
Reflection Target            Retention Mechanism                       Metadata / Cost
Class Identity / Types       Automatic (always retained)               Preserves getClass(), isInstance(), cast()
Class Members                @Reflectable or flagon.reflect.classes     Retains fields, methods, constructors
vendor/ Members              vendor/reflect.txt                        Adds its globs to flagon.reflect.classes
Dynamic Proxy Interfaces     flagon.reflect.proxies                    Synthesizes proxy class at compile time
vendor/ Constructors         META-INF/flagon/constructed.txt           Retains constructors; -Pflagon.app.reflect narrows
Classpath Scan Targets       flagon.preserve (vendor/preserve.txt)     Retains classes that only a scan names

```

* **Dynamic Bytecode Refusal:** Bytecode rewriting frameworks (`defineClass`, runtime ASM/Mixins, runtime Forge/Fabric loaders) cannot function because no JVM bytecode representation exists in the browser. Transforms must be applied to target JARs prior to compilation.

---

## Browser Host Policy (`window.__flagonHost`)

The browser shell coordinates with running bundles via a minimalist JavaScript bridge on `window.__flagonHost`:

```typescript
interface FlagonHost {
  log(level: string, message: string): void;
  catalog(programsJson: string): void;
  status(phase: string, message: string): void;
  ready(): void;
  exited(exitCode: number): void;
  fatal(error: string, stackTrace?: string): void;
}

```

If a bundle is embedded in an environment lacking `window.__flagonHost`, platform services automatically fall back to standard `console.*` output without breaking execution.
