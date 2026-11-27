# Compatibility

What desktop Java and LWJGL code does in a browser. CI runs the self-tests on every backend in Chromium and Firefox.

## Backends

Every build makes three bundles. The page picks one at load time.

| Backend | Browsers | Notes |
| --- | --- | --- |
| WebAssembly GC with JSPI | Chrome 137+ | Pauses the WebAssembly stack. The smallest and fastest. |
| WebAssembly GC | Chrome 119+, Firefox 120+, Safari 18.2+ | Pauses through a coroutine rewrite. |
| JavaScript | All others | Java arrays are typed arrays. |

All bundles compile with TeaVM's strict mode, so a null dereference throws `NullPointerException` and a bad index throws `ArrayIndexOutOfBoundsException`.

## Platform services

| Area | Browser API | Behavior |
| --- | --- | --- |
| Wall clock | `Date.now()` | Millisecond resolution. |
| Monotonic clock | `performance.now()` | The browser clamps the resolution. |
| `System.out`, `System.err` | `console` | Line-buffered. Also sent to the host page. |
| Files | Origin Private File System | Persistent per origin. See [Files](#files). |
| Threads | none | Green threads on one core. See [Threads](#threads). |
| Bundle workers | Web Workers | `dev.flagon.platform.workers` runs a catalog program on another core and passes it messages. See [Threads](#threads). |
| Sockets, HTTP | WebSocket | Through the socket proxy. See [Networking](#networking). |
| `java.net.http` | `fetch` | The page's CORS rules apply. See [Networking](#networking). |
| Graphics | WebGL 2 | Desktop GL 1.1 to 3.3. See [OpenGL](#opengl). |
| Audio | Web Audio | OpenAL 1.1. See [Audio](#audio). |
| Input | DOM events, Pointer Lock | Keyboard, mouse, wheel and one touch point. |
| Web APIs | WebCodecs, WebTransport, WebRTC, Web Bluetooth, WebUSB, IndexedDB | Bindings in `platform/web`. Firefox has no Web Bluetooth or WebUSB, so `get()` returns null there. See the [API reference](api.md#web-api-bindings). |

## Java runtime

### Not supported

- **Class loading at run time.** There is no bytecode in the page, so `ClassLoader.defineClass` and runtime ASM, Mixin, Forge or Fabric loaders cannot work. Transform the JARs before the build.
- **Processes and native code.** `Runtime.exec`, `System.loadLibrary` and `System.load` throw `UnsupportedOperationException`.
- **Listening sockets and UDP.** `ServerSocket`, `DatagramSocket` and `MulticastSocket` throw.
- **Parallel CPU work with shared memory.** Threads take turns on one core. A bundle worker runs on another core, but shares no heap: it talks by messages.

### Reflection

The compiler removes every member that code does not call directly. By default:

- `Class.forName`, `isInstance`, `getSuperclass`, `getInterfaces` and `cast` work for every kept class.
- `getDeclaredFields`, `getDeclaredMethods`, `getDeclaredConstructors` and their public forms return empty arrays. They do not throw.
- `isRecord`, `getPackageName`, `arrayType`, `componentType`, `descriptorString`, `getAnnotationsByType` and `isAnonymousClass` work.
- Typed `Field` and `Array` reads and writes (`getInt`, `setLong` and so on) widen as on the JVM.

Opt a class in to keep its fields, methods and constructors:

- Annotate it with `@Reflectable` from `toolchain/reflect-policy`.
- Or name it in `reflectClasses` (bundle module) or `vendor/reflect.txt`.

An opted-in class also keeps:

- generic superclass, interfaces and type variable bounds;
- record components and permitted subclasses;
- the enclosing method or constructor, when the enclosing class is opted in too;
- annotation member defaults and `Method.isDefault`;
- parameter names when compiled with `-parameters`, else `arg0`, `arg1` and so on.

Other settings:

| Setting | Keeps |
| --- | --- |
| `reflectProxies` | A proxy class for each matching interface, for `Proxy.newProxyInstance`. |
| `reflectConverted` | `valueOf(String)` and the `(String)` constructor, which option parsers call. |
| `flagon.preserve`, `vendor/preserve.txt` | Classes that only a classpath scan names. |

A reflective call to a removed member logs which class to opt in.

### Serialization

`ObjectOutputStream` and `ObjectInputStream` use the JVM's stream format, protocol version 2. The browser writes the same bytes as the JVM for the same objects, and reads streams the JVM wrote.

- **Your classes:** every `Serializable` class is kept and found by name. The build computes the default `serialVersionUID` from the class file.
- **Features:** `writeObject`, `readObject`, `readObjectNoData`, `writeReplace`, `readResolve`, `Externalizable`, records, enums, arrays, `serialPersistentFields`, `putFields`, `readFields` and `registerValidation`.
- **JDK classes:** boxed primitives, `String`, `ArrayList`, `LinkedList`, `ArrayDeque`, `HashMap`, `LinkedHashMap`, `HashSet`, `LinkedHashSet`, `TreeMap`, `TreeSet`, `Date`, `UUID`, `List.of`, `Set.of`, `Map.of`, the `Collections` empty, singleton and unmodifiable wrappers, `StackTraceElement`, and `Throwable` with its common subclasses. Other JDK classes throw `NotSerializableException`.
- **Limits:** no proxy classes. `HashMap` writes a bucket count from its size, not its history. A `LinkedHashMap` in access order reads back in insertion order.

### Resources

The build puts every non-class file of the JARs into `<bundle>.pack`, gzipped. The page streams the pack with the bundle and keeps it in Cache Storage.

- `getResourceAsStream` reads from the pack. `Class.getResourceAsStream` resolves a relative name beside the class.
- `getResource` returns a `flagon:` URL for a file in the pack, and `null` for other names. `openStream` on the URL reads the file.
- `getResources` returns at most one URL. A bundle holds one copy of each name, so a `META-INF/services` scan finds one file.
- Files of `apartFrom` size or larger load in the background after the program starts. A read of one before it arrives throws `IOException`. Reads never wait, because the reader can hold a lock.

### JDK shims

These JDK calls act differently in a browser:

| API | Behavior |
| --- | --- |
| `Runtime.maxMemory()` | 512 MB. |
| `Runtime.availableProcessors()` | The browser's `navigator.hardwareConcurrency`. |
| `System.exit`, `Runtime.exit` | Run the shutdown hooks, then stop the program. |
| `Runtime.halt` | Stops the program without hooks. |
| Shutdown hooks | Run on exit. Not run when the tab closes or reloads. |
| `ManagementFactory` | Uptime, start time and heap use from the runtime. |
| `new UUID(long, long)`, `UUID.fromString` | Work. |
| `Class.isAnonymousClass` | True when the name after the last `$` is all digits. |
| `FileInputStream.getChannel`, `FileOutputStream.getChannel` | Return a `FileChannel` over the file. |
| `Charset.isSupported` | True for the charsets TeaVM carries. |
| `HexFormat` | Works. |

### Class libraries

| Area | Supported | Not supported |
| --- | --- | --- |
| `java.security`, `javax.crypto` | `MessageDigest` MD5, SHA-1, SHA-256, SHA-384 and SHA-512. `Mac` HMAC over each. `SecretKeyFactory` `PBKDF2WithHmac*`. `Cipher` `AES/CFB8/NoPadding` and `RSA/ECB/PKCS1Padding`. `Signature` SHA-1 and SHA-2 with RSA or ECDSA. `KeyAgreement` ECDH. RSA and EC (secp256r1, secp384r1, secp521r1) keys from `KeyPairGenerator` and `KeyFactory`, with X.509 and PKCS#8 encodings. `KeyGenerator` AES. | Other algorithms throw `NoSuchAlgorithmException`. No `KeyStore`. EC math is not constant-time. |
| `java.awt`, `java.awt.geom` | `Graphics2D` on a `BufferedImage`: shapes, text, transforms, clips, `BasicStroke` and `AlphaComposite`, drawn on an `OffscreenCanvas`. Opaque fills, thin straight lines and unscaled copies are drawn in Java and match the JDK pixel for pixel. Logical fonts map to the browser's `sans-serif`, `serif` and `monospace`. The `geom` shapes, `Path2D` and `AffineTransform`. | Aliased canvas drawing: shapes and text are always antialiased. Paints other than `Color`. `Area`, `Arc2D`, `GlyphVector`, XOR mode. Windows. |
| `java.awt.image`, `javax.imageio` | `BufferedImage` with the integer, byte and gray types. `ImageIO` reads PNG in Java, and JPEG, GIF, BMP, WebP and interlaced PNG with the browser. It writes PNG. | Writing other formats. `ImageReader` and `ImageWriter`. |
| `java.awt.datatransfer` | A clipboard inside the page. | The system clipboard. |
| `java.nio.channels` | `FileChannel`. `Selector.open()` with no keys: `select` waits for its timeout or `wakeup`, so a Netty event loop runs tasks and local channels. | `SocketChannel`, `ServerSocketChannel`, `DatagramChannel`. |
| `javax.sound.midi` | The types. | Every device: `MidiSystem` throws `MidiUnavailableException`. |
| `javax.xml.parsers`, `org.w3c.dom` | `DocumentBuilder` parses XML into a DOM. | Schema validation, XSLT. |
| `java.sql` | `Date`, `Time`, `Timestamp`. | JDBC. |
| `java.text`, `java.util.Locale` | `NumberFormat`, `DecimalFormat`, `DateFormat`, `SimpleDateFormat`, `DateFormatSymbols`, `Collator`, and locale and currency names, with the page's `Intl` data for each locale. | `RuleBasedCollator`, `BreakIterator`, `Normalizer`. A `CollationKey` compares, but its bytes do not sort. |
| `java.time` | `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`, `Duration`, `Period`, `ZonedDateTime`, `OffsetDateTime`, `Year`, `YearMonth`, `Clock`, and `ZoneId` with the page's zone rules. `DateTimeFormatter` with the ISO formats, patterns and localized styles. The `ChronoField` and `ChronoUnit` fields and units, and `TemporalAdjusters`. | Other calendars, week fields, `MonthDay`, `OffsetTime`, `DateTimeFormatterBuilder`, `ZoneId.getAvailableZoneIds`. |
| `javax.naming` | The types. | Every lookup throws `NameNotFoundException`. |

## Files

`java.io` works on the Origin Private File System (OPFS).

- In a private window, or where OPFS refuses access, files live in memory for the session.
- OPFS calls suspend the calling green thread. Java code sees blocking calls.
- A file loads fully into memory when it opens.
- Streams write to OPFS on `flush()` and `close()`.
- `RandomAccessFile` writes on `close()`, or on every write in `rwd` and `rws` modes.

### Shipped files

Folders beside the JARs, such as `vendor/assets/`, ship as read-only files under `/`.

- Reads never touch OPFS.
- A program can add files to a shipped folder, but cannot replace a shipped file.
- A file that loads after start opens at once. Its first read throws `IOException` until it arrives.

## Networking

Each socket goes through a WebSocket to the socket proxy, which opens the TCP or TLS connection.

```text
green thread  ──WebSocket──>  socket proxy  ──TCP/TLS──>  server
```

| API | Behavior |
| --- | --- |
| `Socket` | Works through the proxy. |
| `URL`, `HttpURLConnection` | Work through the proxy. |
| `HttpsURLConnection` | The proxy makes the TLS connection. Casts to `javax.net.ssl.HttpsURLConnection` and certificate pinning do not work. |
| `Socket.setSoTimeout` | Stored. Reads still wait for data. |
| `InetAddress.getByName` | Returns an unresolved address. The proxy resolves the name. |
| `ServerSocket` | Throws: a page cannot listen. |
| `DatagramSocket`, `MulticastSocket` | Throw: the transport carries streams only. |

See [Socket proxy](packaging.md#socket-proxy) to run one.

### java.net.http

`HttpClient` sends each request with the page's `fetch`, not the socket proxy. `send` pauses the calling green thread. `sendAsync` runs `send` on the client's executor, or on a new thread.

| API | Behavior |
| --- | --- |
| Cross-origin requests | The server must allow them with CORS headers. |
| `Redirect.NEVER`, the default | A redirect throws `IOException`: a browser does not show where it points. Use `NORMAL` or `ALWAYS` to follow it. |
| Request headers | The browser drops the ones it forbids, such as `Cookie`. |
| `timeout`, `connectTimeout` | Cover the wait for the response headers. |
| Response bodies | Arrive whole before a body handler sees them. `ofInputStream` reads from memory. |
| `version()` of a response | `HTTP_1_1`. The browser does not tell. |
| Cookies, proxies, TLS settings | The browser handles them. The builder has no `cookieHandler`, `proxy`, `sslContext` or `sslParameters`. |
| `close`, `shutdownNow` | Stop new requests. Requests already sent still finish. |
| `WebSocket` | Not supported. |

## Threads

Threads are green threads on the page's one thread. They give concurrency, not parallelism. A thread yields when it waits; code that never waits holds the page until it returns.

For parallel work, `Platforms.get().workers().spawn(program)` starts the bundle again in a Web Worker and runs that catalog program there. The two sides pass text and bytes over a `Channel`. A worker has no display and no audio output. See [the API reference](api.md#devflagonplatformworkers).

| API | Behavior |
| --- | --- |
| `Thread.sleep`, `Object.wait`, `notify` | Suspend the green thread and yield. |
| `synchronized` | Suspends the thread while another holds the monitor. |
| `Executors.newFixedThreadPool(n)` | `n` green threads over one queue. |
| `Executors.newCachedThreadPool()` | A fixed pool of `availableProcessors()` threads. |
| `Executors.newWorkStealingPool()` | A `ForkJoinPool`. |
| `ScheduledThreadPoolExecutor` | Works. |
| `ExecutorService.close()` | Works in try-with-resources. An executor is not an `AutoCloseable`. |
| `ForkJoinPool`, `RecursiveTask`, `RecursiveAction`, `CountedCompleter` | Work, with `commonPool()`. A worker that joins a task no thread has started runs it itself. |
| `CompletableFuture` | The whole `CompletionStage` API, `orTimeout`, `completeOnTimeout` and `delayedExecutor`. Failures wrap in `CompletionException` as on the JVM. |
| `ReentrantLock`, `Condition`, `Semaphore`, `CountDownLatch` | Suspend the thread while blocked. |
| `CyclicBarrier`, `Phaser`, `Exchanger` | Work, with timeouts and tiered phasers. |
| `ReentrantReadWriteLock` | Works. Taking the write lock while holding the read lock throws `IllegalStateException`. |
| `StampedLock` | Works, with optimistic reads and stamp conversion. |
| `ConcurrentHashMap`, `ConcurrentSkipListMap`, `ConcurrentSkipListSet`, atomics | Work. Iterators of the skip-list types walk a snapshot. |
| `ConcurrentLinkedQueue`, `LinkedBlockingQueue`, `PriorityBlockingQueue`, `DelayQueue` | Work. |

The `java.util.concurrent` self-test prints the same lines in the browser as on the JVM.

## WASI

A [WASI component](packaging.md#wasi) runs headless in a WASI preview 2 host. wasmtime 51 and later runs it with `-W gc=y,function-references=y,exceptions=y`.

| Area | Behavior |
| --- | --- |
| Arguments | The first picks the program. The rest go to it. |
| `System.out`, `System.err` | stdout and stderr, as UTF-8. |
| Clocks, `Thread.sleep` | WASI clocks. A sleep on every thread blocks the component. |
| Threads | Green threads on one core, as in a page. |
| Files | `java.io` under the first preopened directory, which is `/`. With none, files are in memory until exit. |
| Exit status | `System.exit` runs the shutdown hooks. Preview 2 reports success or failure, so a status other than 0 exits with 1. |
| `@Export` methods | Functions of the component. Primitives only. |
| Exceptions | The class and message print. There is no stack trace. |
| Sockets | A connection throws `NetworkException`. |
| Display, `java.net.http`, `java.awt` drawing, bundle workers | Refused with `UnsupportedCapabilityException`. `ImageIO` throws `IOException`. |
| Audio | Plays silently. |
| Locale data | en-US for every locale. Every zone is UTC. |

## LWJGL

| Module | Supplies |
| --- | --- |
| `lwjgl/lwjgl2` | `Display`, `GL11` to `GL20` and `GL11C` to `GL20C`, the common ARB and EXT extensions, `Keyboard`, `Mouse`, `AL10`, `ALC10`, `BufferUtils`, `GLU`, `util.vector` and `WaveData`. |
| `lwjgl/lwjgl3` | `GLFW` with its callbacks, `GL30` to `GL33` and `GL30C` to `GL33C`, `MemoryUtil`, `MemoryStack` and `PointerBuffer`. |

An LWJGL 3 program needs both modules. `lwjgl/lwjgl3` has no LWJGL 3 OpenAL binding (`ALC.create`, `ALCapabilities`).

### Display

- `Display.update()` and `glfwSwapBuffers()` copy the frame to the canvas, then wait for `requestAnimationFrame`. Drawing goes to a back buffer, so a half-drawn frame never shows.
- `Display.sync(fps)` waits for frames to hold the rate.
- `Display.setIcon` and `glfwSetWindowPos` do nothing. The page owns the tab icon and canvas position.

## OpenGL

Desktop GL 1.1 to 3.3, drawn with WebGL 2.

| Feature | Support | Details |
| --- | --- | --- |
| Immediate mode | Yes | Each `glBegin`/`glEnd` block goes after the last one in a stream buffer, so a draw never waits on the one before. |
| `GL_QUADS` | Unindexed only | Drawn as triangle pairs. Indexed quads throw: the caller's index buffer is not rewritten. |
| Client arrays | Yes | Copied into buffers before the draw. Arrays interleaved in one buffer share one upload. |
| Display lists | Yes | `glGenLists`, `glNewList`, `glCallList`, `glDeleteLists`. A list's blocks share one buffer per vertex layout. |
| Lighting | Yes | 8 lights in eye space. One-sided: back faces use the front material. `glColorMaterial` tracks any one material. |
| Matrix stacks | Yes | `GL_MODELVIEW`, `GL_PROJECTION`, and a `GL_TEXTURE` stack per unit. |
| Texture environment | Yes | `GL_MODULATE`, `GL_REPLACE`, `GL_ADD`, `GL_DECAL`, `GL_BLEND`, and `GL_COMBINE` without `GL_DOT3`. |
| Texture coordinate generation | Partly | Object linear, eye linear and sphere map for S, T, R and Q. `GL_NORMAL_MAP` and `GL_REFLECTION_MAP` throw. |
| Flat shading | Yes | Takes the last vertex's color, as GL does. |
| Fog, alpha test | Yes | Done in the generated shaders. |
| Logic ops | No effect | WebGL has no logic op stage. The state is kept and draws use `GL_COPY`. |
| Polygon modes | `GL_FILL` only | No wireframe or point mode. |
| Textures | Yes | Byte formats convert to RGBA. No 16-bit integer channels. |
| Mipmaps | Yes | Through `glGenerateMipmap`. All minification filters. |
| Framebuffers | One color attachment | `GL_COLOR_ATTACHMENT1` and up throw. |
| GLSL 1.10 to 3.30 | Translated | See below. |
| GLSL above 3.30 | No | Throws `UnsupportedOperationException`. |

### Shaders

The build-time translator rewrites desktop GLSL for WebGL 2:

- A shader with `#version` becomes GLSL ES 3.00. One without becomes GLSL ES 1.00.
- Names ES 3.00 reserves, such as `sample`, get a `flagon_` prefix.
- Integer literals in float math become float literals.
- A constant uniform initializer is set after the program links.
- Fixed-function built-ins work: the matrices (`gl_ModelViewMatrix` and so on) become uniforms, and `gl_Vertex`, `gl_Color`, `gl_Normal` and `gl_MultiTexCoord*` become attributes.
- A program with only one stage gets a generated other stage.

## Audio

OpenAL 1.1 on Web Audio.

- Browsers start audio only after a user gesture. Flagon resumes audio on the first click or key press. Source states advance while audio waits, so game logic does not stall.
- Inverse, linear and exponential distance models map to `PannerNode`. Distances are clamped.
- Doppler changes the playback rate from the source and listener velocities.
- Streaming with `alSourceQueueBuffers` and `alSourceUnqueueBuffers` works.
- Not supported: capture (`ALC11`), cone outer gain, and unclamped distance models.

## Input

- Mouse Y is flipped to GL's bottom-left origin.
- Buttons map from the DOM order (left, middle, right) to the LWJGL order (left, right, middle).
- Wheel pixels add up into ticks of 120.
- `Mouse.setGrabbed(true)` and `GLFW_CURSOR_DISABLED` use Pointer Lock. The browser needs a click first.
- `Mouse.setCursorPosition` does nothing. Browsers do not move the cursor.
- The first touch point acts as the left mouse button. No multi-touch or on-screen keyboard.
- Keys map from `KeyboardEvent.code`, so the layout does not change key codes.
