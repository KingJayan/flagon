# Architecture

## Layers

```
application.jar (unmodified)
      ↓  compiled together, ahead of time
lwjgl2-compat / lwjgl3-compat  org.lwjgl.* source-compatible classes
      ↓
lwjgl-core                     the emulation they share, carrying no org.lwjgl class
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

`toolchain/` holds the two modules that are not ordinary Java compiled into a bundle. Both are
opt-in: a bundle that depends on one gets what it offers, and one that does not carries none of it.

`toolchain/socket-classlib` declares classes in `java.net`, to supply the `Socket` teavm's classlib
has none of. It is compiled with `--patch-module` because the jvm owns that package name, and holds
no behaviour -- `NetworkSocket` in `runtime` does, where a plain jvm can test it.

`toolchain/reflect-policy` runs inside the compiler rather than the browser. It is a
`ReflectionPolicy` found through `META-INF/services`, and it decides which classes keep their
members through compilation; see [Reflection](#reflection) below.

`toolchain/app-entry` runs before the compiler. It reads whatever jars were put in `vendor/`, asks
the application among them what it runs, and writes `bundle-app`s programs from the answer. Writing the calls rather than
reflecting over them is what lets reachability find the application at all.

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

- **Reachability decides what ships.** Member reflection therefore finds nothing unless a class is
  named ahead of time, which [Reflection](#reflection) below covers.
  `ServiceLoader` is avoided; programs are registered explicitly in `ProgramRegistry`.
- **Application jars are build inputs.** Loading a jar means compiling it, so each application is
  built as a Maven module with the jar on its classpath. `examples/app` and `examples/bundle`
  demonstrate the downstream layout described in [DOWNSTREAM.md](DOWNSTREAM.md).

## Backend selection

TeaVM targets either WebAssembly GC or JavaScript; `flagon.target` chooses, and `-Pjs` sets it
along with the file extension bundles are emitted under. WASM GC is the default, and JavaScript is
the fallback for engines without it: the shell validates a GC module before choosing, so a browser
that predates the proposal runs the JavaScript bundle instead of failing to instantiate.

The two backends differ in one place that reaches into this codebase: a Java array compiled to
JavaScript *is* a typed array, so a browser API can be handed a view over its storage, while under
WASM GC it lives in the WebAssembly heap where JavaScript cannot see it and has to be copied out.
`JsArrays` is that seam. Its branch is folded away at compile time, so each bundle carries only the
path its backend needs, and callers say how many elements they mean rather than passing a whole
growable array.

A WASM GC bundle is a `.wasm` module rather than an ES module: the shell instantiates it through
the loader TeaVM ships and calls the same exported `main`, where a JavaScript bundle is entered by
importing it. The build unpacks that loader from the compiler's own jar next to the bundles, so it
cannot drift from the toolchain that produced them. Nothing above the shell knows the difference —
`?backend=wasm` selects one, and a program is handed the same arguments and the same host bridge
either way.

`strict` mode is always enabled. It inserts Java null and bounds checks so desktop exception
semantics (`NullPointerException`, etc.) continue to work correctly.

## Reflection

A class keeps its identity through compilation -- its name, its place in the hierarchy, casts and
`instanceof` -- because those are decided by reachability. Its members are not: nothing reaches
them, so nothing ships them, and `getDeclaredFields()` answers empty rather than throwing.

`toolchain/reflect-policy` is how a class opts out of that. It names classes to the compiler, which
then keeps every declared field, method and constructor, and lets `Class.forName` find them. A
class is named either by carrying `@Reflectable`, for source the bundle owns, or by matching a glob
in the `flagon.reflect.classes` build property, for a jar that cannot be annotated. An interface
`Proxy.newProxyInstance` may stand in for is named by `flagon.reflect.proxies`, the compiler having
to write the proxy rather than keep members.

The cost is paid per class, which is why it is opt-in: keeping a class whole keeps everything it
can reach whole too. Naming a whole application defeats the point of ahead-of-time compilation.

A jar dropped in `vendor/` is the exception, because there is nobody to ask: it cannot be annotated
and, obfuscated, cannot be globbed. `app-entry` lists its classes to a file named by
`flagon.reflect.constructed`, and each keeps its constructors alone -- enough for the registries
such a jar builds its world with, and far short of keeping it whole.

[COMPATIBILITY.md](COMPATIBILITY.md#reflection-reaches-types-not-members) records what each half
does, and the `selftest` program asserts both in a real bundle so a teavm upgrade cannot change it
quietly.

## Threads

Green threads, so one runs at a time and a switch happens where one suspends. `socket-classlib`
supplies `java.net`; `concurrent-classlib` supplies the executor and future half of
`java.util.concurrent` on the same footing, because TeaVM ships neither. Both are patched into
`java.base` at compile time and reach the compiler as ordinary classes.

A render thread draining what workers produced is therefore a supported shape, and the `threading`
demo runs it. What it is not is parallel: the workers interleave with the frame loop rather than
running beside it. [COMPATIBILITY.md](COMPATIBILITY.md#threads-executors-and-futures) records what
each part does.

## Bytecode at runtime

A mod loader built on Mixin transforms classes as they load: it reads the bytes, rewrites them with
ASM and hands them to a class loader to define. None of that survives here, and the reason is not a
missing feature but the shape of the target. There is no bytecode at runtime to rewrite -- the
program was compiled to JavaScript or WebAssembly before it shipped -- and the class loader the
runtime does expose has no `defineClass`, so there is nowhere to put a class that arrives late. The
`selftest` program asserts both, in a real bundle: a class the compile never saw is not found, and
no class loader can define one from bytes.

That makes runtime transform a no-go, and it puts Fabric and Forge, whose loaders are built on it,
out of reach at runtime. What is in reach is applying the same transform before the compile: mixins
merged into the jar ahead of time reach TeaVM as ordinary classes, and the reflection above is
already enough to load them. Until such a build step exists, this is a vanilla-only runtime.

## Graphics

The compatibility layer rebuilds the parts of OpenGL 1.x that WebGL2 does not provide:

- `Matrix4` and `GLState` maintain fixed-function state.
- `glBegin`/`glEnd` collect vertices and submit a single draw.
- Client arrays are staged into buffer objects when necessary.
- `FixedFunctionPipeline` emulates fixed-function rendering with one configurable shader.

`glUseProgram` switches between the fixed-function and shader pipelines. `GLState` selects the
active pipeline and restores caller-owned GL state after each draw.

Desktop GLSL is translated into GLSL ES by `GlslTranslator` inside `lwjgl-core`. Browser-facing
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

A page drives a bundle through `window.__flagonHost`, a six-method interface:

- `log`
- `catalog`
- `status`
- `ready`
- `exited`
- `fatal`

Keeping the host contract this small means embedding pages and test harnesses need know nothing
about how a bundle was built. Without a host bridge, bundles still run and report through the
console.