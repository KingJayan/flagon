# Architecture

## Layers

```
application.jar (unmodified)
      ↓  compiled together, ahead of time
LWJGL compatibility layer      org.lwjgl.* source-compatible classes
      ↓
platform-api                   Platform, Clock, ConsoleSink, RuntimeHost,
                               GraphicsDevice, DisplaySurface, InputSource,
                               FileStore, AudioDevice, Network
      ↓
platform-browser               the only module that may touch browser APIs
      ↓
browser
```

The rule that matters: **nothing above `platform-api` may reference a browser API.**
The LWJGL compatibility layer depends only on `platform-api`, so the same code can run on a
different host — including a plain JVM, which is what makes most of it unit-testable.

`runtime` sits beside these layers. It owns startup and shutdown, but knows neither what a browser
is nor what LWJGL is.

## Composition

`Platforms` is installed once at boot as a static holder. This is a deliberate exception to
dependency injection: LWJGL's public API is static (`Display.create()`, `GL11.glClear()`), and
passing a platform object around would break source compatibility.

Each bundle (`bundle-sandbox`, `bundle-example`) is a composition root. It chooses a platform,
registers programs, and declares its output. Everything else about compilation lives in the parent
POM, including downstream bundles in other repositories.

`System.out` and `System.err` are redirected into the platform console during startup so
unmodified applications report through the active host automatically.

## Ahead-of-time compilation

TeaVM compiles Java bytecode to JavaScript at build time. There is no JVM and no runtime class
loading, so two constraints shape the project:

- **Reachability decides what ships.** Reflection and `ServiceLoader` are avoided; programs are
  registered explicitly in `ProgramRegistry`.
- **Application jars are build inputs.** Loading a jar means compiling it, so each application is
  built as a Maven module with the jar on its classpath. `example-app` and `bundle-example`
  demonstrate the downstream layout described in [DOWNSTREAM.md](DOWNSTREAM.md).

## Backend selection

TeaVM targets either JavaScript or WebAssembly GC. JavaScript (`flagon.target`) is the default
because its green-thread implementation is mature, while WASM GC threading is not.

`strict` mode is always enabled. It inserts Java null and bounds checks so desktop exception
semantics (`NullPointerException`, etc.) continue to work correctly.

## Graphics

The compatibility layer rebuilds the parts of OpenGL 1.x that WebGL2 does not provide:

- `Matrix4` and `GLState` maintain fixed-function state.
- `glBegin`/`glEnd` collect vertices and submit a single draw.
- Client arrays are staged into buffer objects when necessary.
- `FixedFunctionPipeline` emulates fixed-function rendering with one configurable shader.

`glUseProgram` switches between the fixed-function and shader pipelines. `GLState` selects the
active pipeline and restores caller-owned GL state after each draw.

Desktop GLSL is translated into GLSL ES by `GlslTranslator` inside `lwjgl-compat`. Browser-facing
code deals only in GLSL ES.

None of this depends on browser APIs, so it is covered by JVM unit tests against a recording
`GraphicsDevice`.

## Files

`FileStore` is the host-agnostic filesystem abstraction. `MemoryFileStore` keeps everything in the
heap; `OpfsFileStore` persists it in the browser's Origin Private File System.

`StoreFileSystem` connects TeaVM's `VirtualFileSystem` to the active `FileStore`, allowing
unmodified `java.io` code to work without modification.

Asynchrony stops at the store boundary: browser storage operations suspend the current green thread
so everything above continues to behave as blocking I/O.

## Sound

`AudioDevice` is a host-agnostic mixer exposing buffers, sources and a listener. `SilentAudioDevice`
implements the same contract without producing sound, making playback logic testable on the JVM.

`ALState` keeps the OpenAL state that belongs to the context rather than individual sources,
translating OpenAL semantics onto the underlying audio implementation.

## Network

`Network` exposes blocking `Connection`s rather than sockets. In the browser,
`ProxyNetwork` carries them over a websocket to a proxy; `UnavailableNetwork` refuses every
connection on hosts without networking.

`MemoryNetwork` provides an in-memory implementation for tests.

`NetworkUrls` installs a `URLStreamHandlerFactory` at boot so unmodified `java.net.URL` code
uses the active `Network` implementation transparently.

## Input

`InputSource` belongs to a `DisplaySurface`, because input belongs to a window. Events are queued
and polled rather than delivered through callbacks, matching desktop application structure.

`platform-api` defines the scancode numbering. Each host maps onto that numbering so applications
see consistent input regardless of platform.

The browser implementation needs no locking: DOM events only execute while the game loop is
suspended in `Frames.awaitNextFrame()`, so the queues are never modified while being drained.

## Host contract

A page drives a bundle through `window.__flagonHost`, a five-method interface:

- `log`
- `status`
- `ready`
- `exited`
- `fatal`

Keeping the host contract this small means embedding pages and test harnesses need know nothing
about how a bundle was built. Without a host bridge, bundles still run and report through the
console.