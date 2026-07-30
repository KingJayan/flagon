# Compatibility matrix

What the browser environment does and does not provide. Updated as each stage lands.

## Platform services

| Area | State | Notes |
|------|-------|-------|
| Wall clock | works | `Date.now()` |
| Monotonic clock | works, degraded resolution | `performance.now()`, clamped non-decreasing |
| Console / logging | works | mapped onto `console.*` levels |
| Lifecycle reporting | works | `window.__flagonHost` |
| Filesystem | not started | OPFS-backed `.minecraft` |
| Threading | partial | blocking frame loops work via TeaVM green threads |
| Networking | not started | needs a proxy; browsers have no raw TCP |
| Input | not started | Keyboard/Mouse, next stage |
| Audio | not started | OpenAL → WebAudio |
| Graphics | partial | GL 1.1 fixed-function over WebGL2, see below |

## Java language and library

Verified in Chromium by the `selftest` program (66 checks), which runs in CI as a Playwright test.

| Area | State |
|------|-------|
| Integer and long arithmetic, overflow, shifts | works |
| IEEE-754 doubles and floats, NaN, signed zero | works |
| Strings, `String.format`, JDK-compatible `hashCode` | works |
| Arrays, multidimensional arrays, bounds checks | works |
| Collections, streams, `TreeMap` ordering | works |
| Exceptions, cause chains, `finally`, `ClassCastException` | works |
| `NullPointerException` on null dereference | works **only with `strict` mode** |
| Interfaces, default methods, records, virtual dispatch | works |
| Lambdas, method references, captured locals | works |
| Reflection | untested — likely a problem for the game |

## LWJGL

Verified in Chromium by the `lwjgl` demo, which asserts on pixels read back from the live
WebGL2 framebuffer, plus 22 JVM tests that run the emulation against a recording device.

| Call group | State |
|------------|-------|
| `Display` create/destroy/update/title/size/fullscreen | works |
| Blocking `while (!isCloseRequested()) { ...; Display.update(); }` | works |
| `GL11` clear, viewport, colour/depth mask, depth func, blend func | works |
| `GL11` matrix stacks: push/pop/load/translate/rotate/scale/ortho/frustum | works |
| Immediate mode: `glBegin`/`glVertex`/`glColor`/`glTexCoord`/`glEnd` | works |
| Textures: gen/bind/delete, RGBA upload, nearest and linear filtering | works |
| Alpha test | `GL_GREATER` only |
| `GL_QUADS` | **not emulated** |
| Lighting, fog, display lists, `glTexEnv` | not emulated |
| `BufferUtils`, `MemoryUtil`, `Keyboard`, `Mouse`, `OpenAL` | not started |

Every unimplemented entry point throws `UnsupportedOperationException` naming the call. Nothing
is silently ignored: a dropped state change produces a rendering bug that is far more expensive
to track down than a stack trace.

### GL_QUADS has no WebGL equivalent
WebGL2 dropped `GL_QUADS`, and Minecraft 1.8.9 draws nearly everything with it. Emulating it
means expanding each quad into two triangles during immediate-mode capture — cheap to do, but it
changes vertex counts and index order, so it lands with the work that needs it rather than
speculatively now.

### The frame loop depends on TeaVM's async transformation
`Display.update()` suspends the calling green thread and resumes on `requestAnimationFrame`.
This only works for code reachable from an `@Async` method on a TeaVM-started thread, which
`main` is. Calling it from a plain JS callback would deadlock the loop.

### Display.sync(fps) does not sleep
Desktop LWJGL blocks to hit a frame rate. `requestAnimationFrame` already paces frames, and
sleeping would cost a frame rather than save one, so the request is recorded and ignored.

## Known gaps and shims

### `Integer::sum` is missing from TeaVM's classlib
Linking fails with `Method java.lang.Integer.sum(II)I was not found`. Sibling methods
(`Integer.parseInt`, `Long.sum`) are unaffected. Worked around at the call site with a lambda.
If the game jar hits this, the fix is a classlib supplement rather than more call-site edits.

### Null dereference does not throw `NullPointerException` by default
Without `strict`, TeaVM's JS backend emits no null checks, so a null dereference becomes a
JavaScript `TypeError` that `catch (NullPointerException)` cannot catch. Enabled `strict` in the
root POM; costs about 9% bundle size. Non-negotiable — desktop Java catches NPEs.

### TeaVM's JS entry point marshals `main`'s arguments as `Object[]`
`$rt_mainStarter` builds the argument array with `$rt_createArray($rt_objcls(), n)`, so `main`
receives an `Object[]` where its signature says `String[]`. Invisible until `strict` mode checks
a cast, then it surfaces as a bare `ClassCastException` far from the cause.
`BrowserPlatform.copyOf` re-types the array at the entry point.

### Vite will not serve `public/` files through the module pipeline
The compiled bundle is a build artifact, correctly placed in `public/`, but a bare
`import('/flagon/sandbox.js')` makes Vite try to transform it and fail. `runBundle` resolves the
path to an absolute URL so the bundler treats it as external and the browser fetches it as-is.
