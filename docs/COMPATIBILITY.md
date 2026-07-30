# Compatibility matrix

What the browser environment does and does not provide. Updated as each stage lands.

## Platform services

| Area | State | Notes |
|------|-------|-------|
| Wall clock | works | `Date.now()` |
| Monotonic clock | works, degraded resolution | `performance.now()`, clamped non-decreasing |
| Console / logging | works | mapped onto `console.*` levels |
| Lifecycle reporting | works | `window.__flagonHost` |
| Filesystem | not started | OPFS-backed virtual home directory |
| Threading | partial | blocking frame loops work via TeaVM green threads |
| Networking | not started | needs a proxy; browsers have no raw TCP |
| Input | works | DOM events and Pointer Lock, see below |
| Audio | not started | OpenAL → WebAudio |
| Graphics | partial | GL 1.1 fixed-function over WebGL2, see below |

## Java language and library

Verified in Chromium by the `selftest` program (80 checks), which runs in CI as a Playwright test.

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

Verified in Chromium by the `lwjgl`, `input` and `arrays` demos, which assert on pixels read back
from the live WebGL2 framebuffer and drive real key, button and wheel events, plus 64 JVM tests
that run the emulation against recording and fake devices.

| Call group | State |
|------------|-------|
| `Display` create/destroy/update/title/size/fullscreen | works |
| Blocking `while (!isCloseRequested()) { ...; Display.update(); }` | works |
| `GL11` clear, viewport, colour/depth mask, depth func, blend func | works |
| `GL11` matrix stacks: push/pop/load/translate/rotate/scale/ortho/frustum | works |
| Immediate mode: `glBegin`/`glVertex`/`glColor`/`glTexCoord`/`glEnd` | works |
| `GL_QUADS` | works, expanded to indexed triangles |
| Textures: gen/bind/delete, RGBA upload, nearest and linear filtering | works |
| Alpha test | `GL_GREATER` only |
| `Keyboard` events, held state, key names, repeat toggle | works |
| `Mouse` events, buttons, position, movement, wheel, grabbing | works |
| `Mouse.setCursorPosition` | no-op, see below |
| `BufferUtils` allocation, `MemoryUtil` text encoding | works |
| `MemoryUtil.getAddress` | refused, see below |
| Client arrays: `glVertexPointer`, `glColorPointer`, `glTexCoordPointer` | works |
| `glDrawArrays`, `glDrawElements` | works, except indexed `GL_QUADS` |
| `GL15` buffer objects: gen/bind/delete, data, sub-data | works |
| `glNormalPointer`, `glIndexPointer` | not emulated |
| Lighting, fog, display lists, `glTexEnv` | not emulated |
| Shader objects (`GL20`) | not started |
| `OpenAL` | not started |

Every unimplemented entry point throws `UnsupportedOperationException` naming the call. Nothing
is silently ignored: a dropped state change produces a rendering bug that is far more expensive
to track down than a stack trace.

### GL_QUADS is expanded, not dropped
WebGL2 has no `GL_QUADS`. Each quad becomes triangles `(0,1,2)` and `(0,2,3)` drawn through an
element buffer, preserving GL's winding so face culling behaves the same, and a trailing
incomplete quad is discarded as desktop GL does. The index pattern depends only on the quad
count and the indices for *n* quads are a prefix of those for any larger *n*, so the element
buffer is uploaded once and re-uploaded only when it grows.

### Client arrays are uploaded, because WebGL has no client memory
A pointer into a plain NIO buffer names memory the GPU cannot reach. The buffer is kept rather
than read once, and copied into an internal buffer object on every draw — desktop code sets a
pointer once and refills what is behind it every frame, and reading it at pointer-set time would
render whatever happened to be there.

A pointer into a buffer object costs nothing extra: the data is already on the GPU, and only the
attribute layout is set. That is the path worth taking for geometry that does not change.

A *disabled* array is not an absent one. GL feeds every vertex the current `glColor` or
`glTexCoord` instead, which maps onto a constant generic attribute; leaving the attribute
undefined would render black rather than the colour that was set.

Buffer bindings belong to the application. The pipeline binds buffers of its own to stage client
arrays and to hold quad indices, then restores what the caller had bound, so a draw is not
visible in `GL_ARRAY_BUFFER` afterwards.

`glDrawElements` refuses `GL_QUADS` rather than approximating it. Expanding quads means
rewriting the index buffer, and that buffer belongs to the caller — `glDrawArrays` can expand
quads because it generates the indices itself.

### Input conventions are converted at the browser boundary
Three differ, and each would be a subtle bug if left to callers:

- **Y axis.** The DOM measures down from the top, GL-era input up from the bottom.
- **Button order.** The DOM orders buttons left, middle, right; LWJGL left, right, middle.
- **Wheel units.** The DOM reports pixels, positive toward the user. Deltas accumulate to a
  120-unit notch rather than emitting one per event, so a trackpad does not scroll many times
  faster than a wheel. Line and page delta modes are approximated at 40 and 400 pixels.

Keys are identified by `KeyboardEvent.code`, which names the physical key and ignores the
layout — the same thing a scancode means. Using `KeyboardEvent.key` would report the key left of
`1` as `²` on an AZERTY keyboard and match no scancode.

Pointer lock requires a user gesture and may be refused, so `Mouse.setGrabbed` reports whether
the request was accepted and `isGrabbed()` is the authority on whether it took effect. Event
queues hold 256 events and drop the oldest, so an application that stops draining cannot grow
them without limit.

### Mouse.setCursorPosition does nothing
A page may not warp the pointer. A grabbed pointer has no position to set and a free one belongs
to the user. Desktop code calls this to re-centre a pointer it has already grabbed, where it
changes nothing — the one call implemented as a no-op rather than a refusal.

### Buffers have no address
`MemoryUtil.getAddress` and friends throw `UnsupportedCapabilityException`. Buffers live in the
VM heap, nothing on the page can reach them, and a fabricated pointer would be used as a real
one. `BufferUtils` allocates through `allocateDirect` in native byte order: callers routinely
write through one typed view and read through another, and Java's big-endian default would
silently scramble every one of them.

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
This will not be the last gap: any large jar reaches further into `java.*` than the sandbox does.
The pattern that worked here — link, read the missing-method name, then choose between a
call-site change and a classlib supplement — is the one to repeat rather than pre-empt.

### Null dereference does not throw `NullPointerException` by default
Without `strict`, TeaVM's JS backend emits no null checks, so a null dereference becomes a
JavaScript `TypeError` that `catch (NullPointerException)` cannot catch. Enabled `strict` in the
root POM; costs about 9% bundle size. Non-negotiable — desktop Java catches NPEs. The self-test
checks an NPE is catchable in place and across a call boundary, so losing the flag fails the
suite rather than quietly changing exception behaviour everywhere.

### TeaVM's JS entry point marshals `main`'s arguments as `Object[]`
`$rt_mainStarter` builds the argument array with `$rt_createArray($rt_objcls(), n)`, so `main`
receives an `Object[]` where its signature says `String[]`. Invisible until `strict` mode checks
a cast, then it surfaces as a bare `ClassCastException` far from the cause.
`BrowserPlatform.copyOf` re-types the array at the entry point, and the self-test casts its own
parameter back to `String[]` and echoes what it received so a regression fails at the boundary.

### Vite will not serve `public/` files through the module pipeline
The compiled bundle is a build artifact, correctly placed in `public/`, but a bare
`import('/flagon/sandbox.js')` makes Vite try to transform it and fail. `runBundle` resolves the
path to an absolute URL so the bundler treats it as external and the browser fetches it as-is.
