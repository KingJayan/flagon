# Compatibility Reference

General reference guide for Flagon.

## Targets & Build Flags

| Target | Execution |
| --- | --- |
| **WASM GC** | Preferred in supported browsers. |
| **JavaScript** | Fallback bundle when WASM GC is unsupported. |

Every build emits both.

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

<!-- Retain valueOf(String) and the (String) constructor, which option parsers call -->
<flagon.reflect.converted>java.lang.Integer, java.io.File</flagon.reflect.converted>

```

* **vendor/ constructors:** `make app` keeps constructors for every class in the jar and logs the count. Past 10,000 it warns; past 50,000 it stops. Narrow it with `-Pflagon.app.reflect=your.game.**`.

### ClassLoader & Resource Lookup

* `getResourceAsStream`: Reads the resource pack. The build packs every file a jar carries beside its classes into `<bundle>.pack`, gzipped whole. The page fetches the pack alongside the bundle and unzips it as it downloads, and a resource is copied out of it only when it is read. A bundle that sets `apartFrom` keeps each file of that size or larger in `<bundle>.files/`, named by its SHA-256. The program starts without them, and the page fetches them in the background. A read never waits, since it may hold a lock: before its file arrives, it throws `IOException`. The page keeps the pack and those files in Cache Storage, and revalidates the pack on each load.
* **Audio in the pack:** `make app` re-encodes Ogg Vorbis at quality 0 through ffmpeg. It keeps the original where that saves under 15%. Files of 256 KiB or more, such as music, load after the program starts. `Class.getResourceAsStream` resolves a relative name beside the class, the way the JDK does.
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

* **Backing Store:** Origin Private File System (OPFS). Falls back to an ephemeral in-memory filesystem if OPFS is unavailable or refuses access, as in a private window.
* **Concurrency:** Asynchronous main-thread OPFS access suspends the calling green thread; application-level `java.io` remains synchronous and blocking.
* **Caching:** Files are fully loaded into memory when opened.
* **Flushing:** Streams flush to OPFS on `flush()` and `close()`.
* **`RandomAccessFile`:** Holds its writes in memory and writes them to OPFS on `close()`, or on every write in `rwd` and `rws` modes.
* **Shipped files:** Files in folders beside the jars, such as `vendor/assets/`, are read-only files under `/`. A read of one never touches OPFS. A program can add files to their folders, but not replace a shipped one. A file the page loads after start opens at once, and its first read throws `IOException` until it arrives.

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
* `SocketChannel` / `ServerSocketChannel`: Refused (the proxy carries a stream, not a selectable channel). `Selector.open()` works and holds no keys: `select` waits for its timeout or `wakeup()`, so a Netty NIO event loop runs tasks and local channels.

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
| `synchronized` | Suspends calling green thread while another thread holds the monitor. |
| `ReentrantLock`, `CountDownLatch` | Suspends calling green thread on block; maintains UI loop. |
| `ReentrantReadWriteLock` | Upgrading to a write lock while holding a read lock throws an error. |
| `ForkJoinPool` | Unsupported. Use standard `ExecutorService` patterns instead. |
| Long-running non-suspending tasks | **Blocks the entire page.** Work must yield or await periodically. |

---

## LWJGL & OpenGL Pipeline

Both LWJGL 2 (`lwjgl2-compat`) and LWJGL 3 (`lwjgl3-compat`) APIs are supported.

### Display & Lifecycle

* **`Display.update()` / `glfwSwapBuffers()`:** Copies the frame to the canvas, then suspends the calling thread and waits for browser `requestAnimationFrame()`. Drawing goes to a back buffer, so a frame that pauses half drawn never shows.
* **`Display.sync(fps)`:** Throttles execution by presenting redundant frames via `requestAnimationFrame()`.
* **`Display.setIcon()` / `glfwSetWindowPos()`:** No-op. The host page controls tab icons and canvas positioning.

### OpenGL Support Matrix

| Feature | Support | Details |
| --- | --- | --- |
| **Core Targets** | Desktop GL 1.1–3.2 | Backed by WebGL 2.0. |
| **Immediate Mode** | Supported | Each `glBegin`/`glEnd` block is written after the last one in a stream buffer, so a draw never waits on the one before. |
| **`GL_QUADS`** | Supported (Unindexed) | Converted into indexed triangle pairs on upload. |
| **`GL_QUADS` (Indexed)** | Refused | Rewriting caller-owned index buffers is unsupported. |
| **Client Arrays** | Supported | Staged into internal buffer objects prior to draw calls. Arrays interleaved in one client buffer share one upload. |
| **Display Lists** | Supported | `glGenLists`, `glNewList`, `glCallList`, `glDeleteLists`. A list's `glBegin`/`glEnd` blocks share one buffer per vertex layout. |
| **Lighting** | Supported | 8 fixed-function vertex lights calculated in eye space. One-sided: back faces take the front material, so a `GL_BACK` material is unseen. `glColorMaterial` tracks any one material. |
| **Matrix Modes** | Supported | `GL_MODELVIEW`, `GL_PROJECTION`, and a `GL_TEXTURE` stack per unit. |
| **Texture Environment** | Supported | `GL_MODULATE`, `GL_REPLACE`, `GL_ADD`, `GL_DECAL`, `GL_BLEND`, and `GL_COMBINE` without the `GL_DOT3` functions. |
| **Logic Ops** | Drawn as `GL_COPY` | WebGL has no logic op stage; `glLogicOp` and `GL_COLOR_LOGIC_OP` are kept but change nothing. |
| **Texture Coordinate Generation** | Supported | `glTexGen` makes S, T, R and Q in the vertex shader: object linear, eye linear and sphere map. `GL_NORMAL_MAP` and `GL_REFLECTION_MAP` throw `UnsupportedOperationException`. |
| **Shading Model** | Supported | `GL_FLAT` takes the color of the last vertex, as GL does. |
| **Polygon Modes** | `GL_FILL` only | Wireframe and point polygon rasterization are unsupported. |
| **Texture Types** | Core subsets | Byte uploads converted to RGBA internally. No 16-bit integer channels. |
| **Mipmaps** | Supported | Emulated via `glGenerateMipmap`; supports all standard minification filters. |
| **Framebuffer Objects** | Single Attachment | Multi-render-target (`GL_COLOR_ATTACHMENT1+`) is unsupported. |
| **GLSL (Desktop 1.10–3.30)** | Auto-transpiled | A shader with a `#version` becomes GLSL ES 3.00, and one without becomes GLSL ES 1.00. Names ES 3.00 reserves, such as `sample`, get a `flagon_` prefix. A constant uniform initializer is set after link. Integer literals in float math still fail (#527). |
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

### Null Pointer Exceptions

TeaVM requires `strict` mode to insert null checks on field and method dereferences under JavaScript. If disabled, dereferencing null surfaces as an uncatchable JavaScript `TypeError` instead of a `NullPointerException`.

### Main Entry Point Signature

TeaVM synthesizes `main(String[])` signatures using `Object[]` arrays internally. `BrowserPlatform.copyOf()` sanitizes array types before execution.
