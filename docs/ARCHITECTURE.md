# Architecture

## Layers

```
minecraft.jar (unmodified)
      ↓  compiled together, ahead of time
LWJGL compatibility layer      org.lwjgl.* source-compatible classes
      ↓
platform-api                   Platform, Clock, ConsoleSink, RuntimeHost
      ↓
platform-browser               the only module that may touch browser APIs
      ↓
browser
```

The rule that matters: **nothing above `platform-api` may reference a browser API.** The LWJGL
layer is written against `platform-api` alone, so the same compatibility code could run on a
different host — including a plain JVM, which is what makes it testable.

`runtime` sits beside these layers rather than inside them. It owns the boot sequence and knows
neither what a browser is nor what LWJGL is.

## Composition

`Platforms` is a static holder, installed once at boot. This is a deliberate exception to
dependency injection: LWJGL's public API is static (`Display.create()`, `GL11.glClear()`), so the
compatibility layer cannot thread a platform argument through without breaking the source
compatibility that is the entire point of the project.

Each bundle module (`bundle-sandbox`, later a game bundle) is a composition root: it is the only
place that knows both which host it targets and which programs exist.

## Ahead-of-time compilation

TeaVM compiles Java bytecode to JavaScript at build time. There is no JVM in the browser and no
class loading at runtime, which has two consequences that shape the code:

- **Reachability decides what ships.** Reflection and `ServiceLoader` are unreliable, so programs
  are registered explicitly in a `ProgramRegistry`.
- **The game jar is a build input, not a runtime input.** Loading `minecraft.jar` means compiling
  it, so the bundle for the game is produced by a Maven module that takes the jar on its
  classpath.

## Backend selection

TeaVM can target JavaScript or WebAssembly GC. The default is JavaScript (`flagon.target`),
for two reasons:

- Its green-thread support (`Thread`, `synchronized`, `wait`/`notify` via CPS transformation) is
  mature, and Minecraft 1.8.9 is threaded. The WASM GC backend's threading story is not there yet.
- Correctness first. The property exists so the WASM GC backend can be evaluated at the rendering
  stage, where its performance would actually matter.

`strict` mode is on. It costs about 9% bundle size and inserts null and bounds checks, without
which a null dereference surfaces as a JavaScript `TypeError` that `catch (NullPointerException)`
cannot catch. Desktop Java code relies on catching those.

## Fixed-function emulation

WebGL2 has no fixed-function pipeline, so the parts of GL 1.x that desktop code relies on are
rebuilt above `platform-api`:

- `Matrix4` and `GLState` keep the modelview and projection stacks that GL kept internally.
- `glBegin`/`glEnd` capture interleaved vertices into a growable array and issue one draw.
- `FixedFunctionPipeline` folds per-vertex colour, one texture unit, and the alpha test into a
  single shader whose uniforms carry what GL would have held as state. Features toggle by
  uniform rather than by recompiling, so state changes stay cheap.

None of this touches a browser API, which is why it is covered by JVM unit tests running against
a recording `GraphicsDevice` rather than only in a browser.

## Host contract

A page drives a bundle through `window.__flagonHost`, a five-method object
(`log`, `status`, `ready`, `exited`, `fatal`). Keeping it this small means a test harness or an
embedding page needs to know nothing about how the bundle was compiled. A bundle loaded without a
bridge still runs and reports to the console.
