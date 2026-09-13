# Compatibility Reference

General reference guide for Flagon.

## Targets & Build Flags

| Target | Build Flag | Execution |
| --- | --- | --- |
| **WASM GC** | *(Default)* | Preferred in supported browsers. |
| **JavaScript** | `-Pjs` | Fallback bundle when WASM GC is unsupported. |

CI executes all self-tests across both backends in Chromium and Firefox.

---

## Platform Services

| Area | Status | Underlying Web API / Implementation |
| --- | --- | --- |
| **Wall Clock** | Supported | `Date.now()` |
| **Monotonic Clock** | Clamped | `performance.now()` (monotonic, clamped resolution) |
| **Console & Logging** | Supported | Direct mapping to browser `console.*` |
| **`System.out` / `System.err**` | Supported | Line-buffered and routed to `console.log` / `console.error` |
| **Host Lifecycle** | Supported | Communicates via `window.__flagonHost` |
| **Filesystem** | Persistent | `java.io` backed by Origin Private File System (OPFS) |
| **Threading** | Cooperative | Single-core green threads (TeaVM async suspension) |
| **Sockets & HTTP** | Proxied | WebSocket tunnel via companion proxy server |
| **Input** | Supported | DOM Events + Pointer Lock API |
| **Audio** | Supported | OpenAL over Web Audio API |
| **Graphics** | Core subset | Desktop GL 1.1–3.2 mapped onto WebGL 2.0 |

---

## Java Runtime & Classpath

### Unsupported Runtime Behaviors

* **Dynamic Bytecode Modification:** Refused. No JVM bytecode exists at runtime; `ClassLoader.defineClass` is unavailable (blocks runtime Forge/Fabric mod loaders).
* **Process Management:** `Runtime.exec` and native `System.loadLibrary` throw `UnsupportedOperationException`.
* **Outbound/Inbound Sockets:** Direct raw TCP/UDP cannot run in-browser. `ServerSocket` is explicitly refused.

### Reflection Rules

Ahead-of-time (AOT) tree-shaking strips unreferenced class members by default. `Class.forName`, `isInstance`, and hierarchy introspection remain functional across all retained classes.

* **Default:** Field/method/constructor reflection returns empty arrays instead of throwing.
* **Opt-in via Annotation:** Annotate with `@Reflectable` (from `toolchain/reflect-policy`).
* **Opt-in via Build Configuration:**

```xml
<!-- Retain members for third-party classes -->
<flagon.reflect.classes>com.example.Settings, com.example.model.*</flagon.reflect.classes>

<!-- Retain interface proxies for Proxy.newProxyInstance -->
<flagon.reflect.proxies>com.example.Listener*</flagon.reflect.proxies>

<!-- Retain constructors for unannotated/obfuscated jars -->
<flagon.reflect.constructed>target/app-classes.txt</flagon.reflect.constructed>

<!-- Retain valueOf(String) and the (String) constructor, which option parsers call -->
<flagon.reflect.converted>java.lang.Integer, java.io.File</flagon.reflect.converted>

```

### ClassLoader & Resource Lookup

* `getResourceAsStream`: Reads the resource pack. The build packs every file a jar carries beside its classes into `<bundle>.pack`, and the page fetches that pack before the bundle runs. `Class.getResourceAsStream` resolves a relative name beside the class, the way the JDK does.
* `ClassLoader.getResource`, `Class.getResource`: Return a `flagon:` URL for a file in the pack, and `null` for any other name. `openStream` on that URL reads the packed bytes.
* `ClassLoader.getResources`: Returns the one URL the pack holds for that name, or an empty enumeration. A bundle is one artifact, so no name ever has two copies, and a multi-jar scan such as `META-INF/services` still finds only one.

### JDK Shims Reference

| API | Behavior / Return Value |
| --- | --- |
| `Runtime.maxMemory()` | Fixed at `512MB` (standard browser tab budget). |
| `System.exit()`, `Runtime.exit()` | Invokes registered shutdown hooks sequentially. |
| `Runtime.halt()` | Immediately halts execution without running hooks. |
| `Shutdown Hooks` | Cooperative execution during standard exit. **Ignored on tab close/reload.** |
| `Class.getGenericSuperclass()` | Raw class only (generics erased at compilation). |
| `Class.isAnonymousClass()` | `true` if the compiled class name ends with a digit. |
| `Array.newInstance(Class, int...)` | Dynamically constructs nested arrays. |
| `java.sql.Date`, `Time`, `Timestamp` | Type-mapping stubs provided by `toolchain/jdk-classlib`. |
| `ManagementFactory` | Reports process uptime and memory usage derived from runtime start. |
| `java.util.UUID` | Standard pseudo-random UUIDs supported; `(long, long)` constructor is absent. |

---

## Storage & Filesystem (`java.io`)

* **Backing Store:** Origin Private File System (OPFS). Falls back to an ephemeral in-memory filesystem if OPFS is unavailable.
* **Concurrency:** Asynchronous main-thread OPFS access suspends the calling green thread; application-level `java.io` remains synchronous and blocking.
* **Caching:** Files are fully loaded into memory when opened.
* **Flushing:** Streams flush to OPFS on `flush()` and `close()`.
* **`RandomAccessFile`:** Configured to write through to OPFS on every write operation to prevent data loss.

---

## Networking (`java.net`)

All raw socket access routes through a mandatory proxy over WebSockets using `socket-classlib`.

```
Browser (Green Thread) <──WebSocket──> flagon proxy <──TCP/TLS──> Target Host

```

### Supported Socket Options

* `Socket`: Supported via the proxy tunnel.
* `HttpURLConnection` / `URL`: Routed through the proxy via an internal `URLStreamHandlerFactory`.
* `Socket.setSoTimeout`: Stored and queryable; socket reads continue to block until data arrives.
* `HttpsURLConnection`: Direct TLS negotiation is offloaded to the proxy. Direct casting to `javax.net.ssl.HttpsURLConnection` or custom certificate pinning is unsupported.

### Unsupported Networking APIs

* `ServerSocket`: Refused (browsers cannot open listening ports).
* `DatagramSocket` / `MulticastSocket`: Refused (the WebSocket transport is stream-only).

### Proxy Server CLI

```bash
# Production proxy with origin binding and restricted destination host
bun run proxy -- --root dist --host 0.0.0.0 --port 8080 \
  --token "$FLAGON_PROXY_TOKEN" \
  --allow game.example.com:25565

```

---

## Threading & Concurrency

Threading is backed by cooperative green threads via `toolchain/concurrent-classlib`. **Concurrency provides non-blocking async execution, not multi-core parallelism.**

| Component | Behavior |
| --- | --- |
| `Thread.sleep`, `Object.wait/notify` | Suspends current green thread; yields execution to event loop. |
| `Executors.newFixedThreadPool(n)` | Allocates `n` green worker threads over an internal task queue. |
| `Executors.newCachedThreadPool()` | Thread pool upper bound clamped to `availableProcessors()`. |
| `CompletableFuture` | Supported across standard chains (`supplyAsync`, `thenApply`, etc.). |
| `ReentrantLock`, `CountDownLatch` | Suspends calling green thread on block; maintains UI loop. |
| `ReentrantReadWriteLock` | Upgrading to a write lock while holding a read lock throws an error. |
| `ForkJoinPool` | Unsupported. Use standard `ExecutorService` patterns instead. |
| Long-running non-suspending tasks | **Blocks the entire page.** Work must yield or await periodically. |

---

## LWJGL & OpenGL Pipeline

Both LWJGL 2 (`lwjgl2-compat`) and LWJGL 3 (`lwjgl3-compat`) APIs are supported.

### Display & Lifecycle

* **`Display.update()` / `glfwSwapBuffers()`:** Suspends the calling thread and waits for browser `requestAnimationFrame()`.
* **`Display.sync(fps)`:** Throttles execution by presenting redundant frames via `requestAnimationFrame()`.
* **`Display.setIcon()` / `glfwSetWindowPos()`:** No-op. The host page controls tab icons and canvas positioning.

### OpenGL Support Matrix

| Feature | Support | Details |
| --- | --- | --- |
| **Core Targets** | Desktop GL 1.1–3.2 | Backed by WebGL 2.0. |
| **Immediate Mode** | Supported | `glBegin`/`glEnd` batches emulated via dynamic VBOs. |
| **`GL_QUADS`** | Supported (Unindexed) | Converted into indexed triangle pairs on upload. |
| **`GL_QUADS` (Indexed)** | Refused | Rewriting caller-owned index buffers is unsupported. |
| **Client Arrays** | Supported | Staged into internal buffer objects prior to draw calls. |
| **Display Lists** | Supported | `glGenLists`, `glNewList`, `glCallList`, `glDeleteLists`. |
| **Lighting** | Supported | 8 fixed-function vertex lights calculated in eye space. |
| **Shading Model** | `GL_SMOOTH` only | `GL_FLAT` is unsupported (no provoking vertex in WebGL). |
| **Polygon Modes** | `GL_FILL` only | Wireframe and point polygon rasterization are unsupported. |
| **Texture Types** | Core subsets | Byte uploads converted to RGBA internally. No 16-bit integer channels. |
| **Mipmaps** | Supported | Emulated via `glGenerateMipmap`; supports all standard minification filters. |
| **Framebuffer Objects** | Single Attachment | Multi-render-target (`GL_COLOR_ATTACHMENT1+`) is unsupported. |
| **GLSL (Desktop 1.10–3.30)** | Auto-transpiled | Rewritten to GLSL ES 1.00 or 3.00 at link time. |
| **GLSL (> 3.30)** | Unsupported | Throws `UnsupportedOperationException`. |

### Fixed-Function Shader Rewriting

Desktop shaders that depend on built-in fixed-function variables are transpiled to GLSL ES 3.00:

* Matrices (`gl_ModelViewMatrix`, `gl_ProjectionMatrix`, etc.) are mapped to internal uniforms.
* Vertex inputs (`gl_Vertex`, `gl_Color`, `gl_Normal`, `gl_MultiTexCoord*`) map to dedicated VBO attributes.
* Missing shader stages in legacy programs are dynamically generated to satisfy WebGL linking rules.

---

## Audio (OpenAL)

* **Backend:** Web Audio API.
* **Activation:** Browsers suspend audio until the first interaction. Flagon auto-resumes the context on the first user click or keypress. Source states advance during suspension to avoid logic hangs.
* **Distance Models:** Inverse, linear, and exponential attenuation map to Web Audio `PannerNode` algorithms. Distances are clamped.
* **Doppler:** Pitch-adjusted along the relative source-listener vector (Web Audio's removed Doppler is handled manually).
* **Streaming:** Supported via `alSourceQueueBuffers` and `alSourceUnqueueBuffers`.
* **Unsupported:** Audio capture (`ALC11`), sound cone outer gains, and unclamped distance models.

---

## Input & Windowing

* **Coordinate System:** DOM mouse coordinates are inverted on the Y-axis to match OpenGL's bottom-left origin.
* **Button Indices:** Reordered from DOM format (`0=L, 1=M, 2=R`) to LWJGL format (`0=L, 1=R, 2=M`).
* **Wheel:** Pixel deltas are accumulated into discrete 120-unit ticks.
* **Pointer Lock:** Bound to `Mouse.setGrabbed(true)` and `glfwSetInputMode(..., GLFW_CURSOR_DISABLED)`. Requires a prior user gesture.
* **`Mouse.setCursorPosition`:** No-op (browsers forbid programmatic cursor warping).
* **Touch Events:** The primary touch point behaves as a left mouse button. Multi-touch and virtual keyboards are unhandled.

---

## Known Edge Cases

### WASM GC Out-of-Bounds Trapping

Under WASM GC, out-of-bounds array access where the index is a compiler constant may fold directly into a WebAssembly `array.get` instruction. This causes a browser engine trap (`RuntimeError`) rather than an interceptable `ArrayIndexOutOfBoundsException`. Variable-indexed bounds checks work as expected.

### Null Pointer Exceptions

TeaVM requires `strict` mode to insert null checks on field and method dereferences under JavaScript. If disabled, dereferencing null surfaces as an uncatchable JavaScript `TypeError` instead of a `NullPointerException`.

### Main Entry Point Signature

TeaVM synthesizes `main(String[])` signatures using `Object[]` arrays internally. `BrowserPlatform.copyOf()` sanitizes array types before execution.
