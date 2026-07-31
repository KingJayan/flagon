# Compatibility matrix

What the browser environment does and does not provide. Updated as each stage lands.

## Platform services

| Area | State | Notes |
|------|-------|-------|
| Wall clock | works | `Date.now()` |
| Monotonic clock | works, degraded resolution | `performance.now()`, clamped non-decreasing |
| Console / logging | works | mapped onto `console.*` levels |
| `System.out` / `System.err` | works | split into lines and forwarded to the console |
| Lifecycle reporting | works | `window.__flagonHost` |
| Filesystem | works | `java.io` on the origin private file system, see below |
| Threading | partial | blocking frame loops work via TeaVM green threads |
| Networking | not started | needs a proxy; browsers have no raw TCP |
| Input | works | DOM events and Pointer Lock, see below |
| Audio | works | `OpenAL` on web audio, see below |
| Graphics | partial | GL 1.1 fixed-function and GL 2.0 shaders over WebGL2, see below |

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
| `File`, `FileInputStream`, `FileOutputStream`, `RandomAccessFile` | works, see below |

### Files are real, and they persist
`java.io` reaches its filesystem through TeaVM's `VirtualFileSystem`, whose default is a heap tree
that a page loses on reload. Flagon replaces it at boot with one backed by the **origin private
file system**: a per-origin directory tree the browser keeps. Unmodified file code works, and what
it writes is still there on the next load. The `files` demo proves it by counting its own runs
across a reload.

OPFS is asynchronous on the main thread — every handle arrives in a promise, and the synchronous
access handles that would avoid that exist only inside a worker. Each call suspends the calling
green thread and resumes when the browser answers, the same mechanism the frame loop uses.

An open file is held whole in memory. The browser hands out a file at a time rather than a byte at
a time, so reading any part of one costs a read of all of it; edits happen in the heap and go back
on flush or close. The cost is that a file is as large in memory as on disk while it is open.

Where OPFS is unavailable the store falls back to the heap, and says so on the console — an
application that saves its work would otherwise appear to succeed and lose it.

### RandomAccessFile writes straight through
`TRandomAccessFile.close()` in TeaVM 0.12.0 drops its accessor without closing or flushing it, and
`autoFlush` is only set for modes `rws` and `rwd`. Against TeaVM's in-memory filesystem writes
mutate shared state as they happen and nothing is lost; against any store that writes contents
back, everything is.

`RandomAccessFile` is the only caller that opens a file both readable and writable, so those
accessors write through on every write. Streams, which do close properly, keep the cheaper path.

## LWJGL

Verified in Chromium by the `lwjgl`, `input`, `arrays`, `shaders` and `audio` demos, which
assert on pixels read back from the live WebGL2 framebuffer, drive real key, button and wheel
events, and play a tone through to its end, plus 115 JVM tests that run the emulation against
recording and fake devices.

| Call group | State |
|------------|-------|
| `Display` create/destroy/update/title/size/fullscreen | works |
| Blocking `while (!isCloseRequested()) { ...; Display.update(); }` | works |
| `GL11` clear, viewport, color/depth mask, depth func, blend func | works |
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
| `GL20` shader and program objects, info logs, `glValidateProgram` | works |
| `GL20` uniforms: scalar, vector and matrix forms | works |
| `GL20` generic attributes: pointer, enable, bind and query location | works |
| Desktop GLSL 1.10 and 1.20 shader source | works, translated |
| GLSL 1.30 and above, fixed-function GLSL built-ins | refused, see below |
| `glBegin`/`glEnd` while a program is bound | refused, see below |
| `AL` create/destroy, `AL10` buffers, sources and listener | works |
| `alGetError` latching, `AL_INVALID_NAME` on a name that is gone | works |
| Source gain, pitch, looping, position, relative positioning | works |
| Distance attenuation: reference distance, rolloff, max distance | works |
| `AL_NONE`, `AL_INVERSE_DISTANCE`, `AL_INVERSE_DISTANCE_CLAMPED` | works |
| Other distance models, cone angles, min and max gain, `AL_DIRECTION` | refused, see below |
| Doppler shift, `AL_VELOCITY` | recorded, no effect, see below |
| Streaming: `alSourceQueueBuffers` and its queue counters | refused, see below |
| `ALC10`, capture, `WaveData` | not emulated |

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
undefined would render black rather than the color that was set.

Buffer bindings belong to the application. The pipeline binds buffers of its own to stage client
arrays and to hold quad indices, then restores what the caller had bound, so a draw is not
visible in `GL_ARRAY_BUFFER` afterwards.

`glDrawElements` refuses `GL_QUADS` rather than approximating it. Expanding quads means
rewriting the index buffer, and that buffer belongs to the caller — `glDrawArrays` can expand
quads because it generates the indices itself.

### A bound program replaces the fixed-function pipeline
`glUseProgram` decides which pipeline a draw goes through. With a program bound, draws read the
generic attribute arrays set by `glVertexAttribPointer` and nothing of GL's own state reaches the
shader; with none bound, they read the client arrays of `GL11` and the built-in shader that
reproduces fixed-function output. `glUseProgram(0)` puts the second one back.

`glBegin` is refused while a program is bound. Immediate mode exists to feed the fixed-function
pipeline, and on the desktop a shader reads those vertices through `gl_Vertex` and its
neighbours — which GLSL ES does not have. Drawing them through the program anyway would put
geometry on screen with the wrong shader reading it.

### Desktop GLSL is translated, and only so far
WebGL2 compiles GLSL ES 1.00 and 3.00. Desktop GLSL 1.10 and 1.20 are close enough to the first
that the version directive is dropped and the source passes through: `attribute`, `varying`,
`texture2D` and `gl_FragColor` mean the same thing in both. A fragment shader is given
`precision mediump float;` if it declares no precision of its own, which desktop GLSL has no way
to express and ES requires.

Two things are refused by name rather than handed to the browser as a syntax error. GLSL 1.30 and
above diverge too far to rewrite. The fixed-function built-ins —
`gl_ModelViewProjectionMatrix`, `gl_Vertex`, `gl_MultiTexCoord0`, `ftransform()` and the rest —
are GL state that GL ES never had; a shader using them needs its matrices and attributes passed
in as its own uniforms and attributes.

### Vertex attributes must be aligned
WebGL requires an attribute's offset and stride to be a multiple of its component size, where
desktop GL does not. An interleaved format of two floats plus three colour bytes has an 11-byte
stride: legal on the desktop, and rejected here. Flagon throws
`UnsupportedCapabilityException` naming the offending value, because the browser's own answer is
an `INVALID_OPERATION` that draws nothing and reports nothing.

### Sound is silent until the page is touched
A browser suspends an `AudioContext` created without a user gesture, and every source it plays is
inaudible until the page is clicked or typed into. Flagon asks for the context back on the first
of either and says on the console that it is waiting, because a run that made no sound and
reported nothing looks like a broken mixer rather than a browser policy.

`AudioDevice.isOutputLive()` is the honest answer to "can anything be heard right now", and the
`audio` demo prints it. Playback itself is unaffected: a suspended context still advances a
source to its end, so code waiting on `AL_STOPPED` is never stranded.

### One buffer per source, played whole
Web audio plays a buffer from beginning to end and reports only that it finished.
`alSourceQueueBuffers` needs a mixer that says how much of a queue it has consumed, so streaming
is refused by name rather than approximated — a music track that silently plays its first chunk
forever is worse than a stack trace. Attach a single buffer with `alSourcei(AL_BUFFER)`.

A source's node is rebuilt on every `alSourcePlay`, because a web audio source that has been
started cannot be started again. Its gain and panner outlive it and hold what OpenAL keeps on the
source, so gain, pitch and position survive a stop and a replay.

### Distance attenuation maps onto the panner; doppler does not
`AL_REFERENCE_DISTANCE`, `AL_ROLLOFF_FACTOR` and `AL_MAX_DISTANCE` are the same three numbers a
`PannerNode` takes, and inverse attenuation is the model both default to. `AL_NONE` is expressed
as a rolloff of zero, and `alGetSourcef` still reports the factor that was set rather than the
zero the browser was given. Linear and exponential models are refused by name.

Web audio removed velocity and doppler shift from its panner and listener outright.
`AL_VELOCITY` is recorded so a caller reading it back sees what it set, and warns once that it
produces no pitch shift. `AL_SOURCE_RELATIVE` has no counterpart either and is emulated by
offsetting the source by the listener, which is what OpenAL means by it.

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
to the user. Desktop code calls this to re-center a pointer it has already grabbed, where it
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
