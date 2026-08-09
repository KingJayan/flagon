# Compatibility matrix

What the browser environment does and does not provide. Updated as each stage lands.

## Backends

The same sources compile to either backend: a default build emits WebAssembly GC, `-Pjs` emits
JavaScript. The shell runs WASM GC where the engine supports it and the JavaScript bundle where it
does not. Everything below describes both unless a row says otherwise.

| Area | WASM GC | JavaScript |
|------|---------|------------|
| Boots, runs a program, exits | works | works |
| Java language and library | works, one gap below | works |
| Arguments, console, lifecycle reporting | works | works |
| Graphics, input, files, audio, networking | works | works |

Every browser test runs once per backend, so the two columns are held level by the same
assertions. What remains is in the [issue tracker](https://github.com/KingJayan/flagon/issues).

## Platform services

| Area | State | Notes |
|------|-------|-------|
| Wall clock | works | `Date.now()` |
| Monotonic clock | works, degraded resolution | `performance.now()`, clamped non-decreasing |
| Console / logging | works | mapped onto `console.*` levels |
| `System.out` / `System.err` | works | split into lines and forwarded to the console |
| Lifecycle reporting | works | `window.__flagonHost` |
| Filesystem | works | `java.io` on the Origin Private File System, see below |
| Threading | partial | blocking frame loops via TeaVM green threads |
| Networking | works through a proxy, not through `java.net.Socket` | `Network.connect()` over a websocket, see below |
| Input | works | DOM events and Pointer Lock, see below |
| Audio | works | `OpenAL` over Web Audio, see below |
| Graphics | partial | GL 1.1 fixed-function and GL 2.0 shaders over WebGL2, see below |

## Java language and library

Verified in Chromium and Firefox by the `selftest` program (80 checks), which runs in CI as a Playwright test
on both backends.

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
| `java.net.URL`, `HttpURLConnection` over HTTP | works through a proxy, see below |
| `java.net.Socket`, `ServerSocket`, `DatagramSocket` | absent from the classlib; use `Network.connect()` |

### Files are real, and they persist

TeaVM normally backs `java.io` with an in-memory `VirtualFileSystem` that disappears on reload.
Flagon replaces it at boot with one backed by the **Origin Private File System (OPFS)**, so
unmodified file code persists across page loads. The `files` demo proves this by counting its own
runs across a reload.

OPFS is asynchronous on the main thread. Each filesystem operation suspends the calling green
thread and resumes when the browser responds, so everything above the store continues to behave as
blocking I/O.

Files are cached in memory while open. Reading any part of a file requires loading the whole file,
and changes are written back on flush or close.

Where OPFS is unavailable, Flagon falls back to an in-memory store and reports the fallback on the
console rather than silently discarding persistent data.

### RandomAccessFile writes straight through

TeaVM's `TRandomAccessFile.close()` drops its accessor without flushing it unless opened
with `rws` or `rwd`. Against TeaVM's default in-memory filesystem this goes unnoticed; against a
real backing store it loses data.

`RandomAccessFile` is the only caller that opens a file for both reading and writing, so its
accessors write through on every write. Streams still use the cheaper buffered path because they
close correctly.

## LWJGL

Verified in Chromium and Firefox on both backends by the `lwjgl`, `input`, `arrays`, `shaders`, `texture`, and
`audio` demos, which assert framebuffer output, drive real keyboard, mouse and wheel events, and
play audio through to completion. The compatibility layer is also covered by 183 JVM tests running against recording and
fake devices.

| Call group | State |
|------------|-------|
| `Display` create/destroy/update/title/size/fullscreen | works |
| Blocking `while (!isCloseRequested()) { ...; Display.update(); }` | works |
| `GL11` clear, viewport, color/depth mask, depth func, blend func | works |
| `glScissor` and `GL_SCISSOR_TEST` | works |
| `glCullFace`, `glFrontFace` | works |
| `GL11` matrix stacks: push/pop/load/translate/rotate/scale/ortho/frustum | works |
| Immediate mode: `glBegin`/`glVertex`/`glColor`/`glTexCoord`/`glEnd` | works |
| `GL_QUADS` | works, expanded to indexed triangles |
| Textures: gen/bind/delete, RGBA upload, nearest and linear filtering | works |
| `glTexParameteri` wrapping (`GL_REPEAT`, `GL_CLAMP`, `GL_MIRRORED_REPEAT`), min and mag filters separately | works |
| Mipmaps: uploads per level, `glGenerateMipmap`, `GL_GENERATE_MIPMAP`, base/max level | works |
| The four mipmapped minification filters | works |
| Alpha test | `GL_GREATER` only |
| `Keyboard` events, held state, key names, repeat toggle | works |
| `Mouse` events, buttons, position, movement, wheel, grabbing | works |
| `Mouse.setCursorPosition` | no-op, see below |
| `BufferUtils` allocation, `MemoryUtil` text encoding | works |
| `MemoryUtil.getAddress` | refused, see below |
| Client arrays | works |
| `glDrawArrays`, `glDrawElements` | works, except indexed `GL_QUADS` |
| `GL15` buffer objects | works |
| Display lists: `glGenLists`, `glNewList`, `glCallList`, `glDeleteLists`, `glIsList` | works, see below |
| `glPushAttrib` / `glPopAttrib` | works |
| `glLoadMatrix`, `glMultMatrix` | works |
| `glReadPixels`, `glCopyTexSubImage2D` | works, RGBA bytes |
| Stencil test, func, op, mask, clear | works |
| `glPolygonOffset`, `glLineWidth`, `glPointSize` | works |
| `glHint` | accepted, advisory, no effect |
| `glPolygonMode` | `GL_FILL` only |
| `glFinish`, `glFlush` | works |
| `glGetError` | works |
| `glGetString` vendor/renderer/version/GLSL version/extensions | works, describes WebGL2 |
| `glGetInteger` max texture size, texture units, vertex attributes | works |
| `glGetInteger`, `glGetFloat`, `glGetBoolean`, `glIsEnabled` of state the layer sets | works, answered from the mirror |
| Any other `glGetInteger`/`glGetString` name | refused |
| `glNormalPointer`, `glIndexPointer` | not emulated |
| Lighting, fog, display lists, `glTexEnv` | not emulated |
| `GL20` shader/program objects, uniforms and generic attributes | works |
| Desktop GLSL 1.10 / 1.20 | works, translated |
| GLSL 1.30+, fixed-function GLSL built-ins | refused |
| `glBegin`/`glEnd` with a bound program | refused |
| `AL` / `AL10` buffers, sources and listener | works |
| `alGetError` latching | works |
| Source gain, pitch, looping, positioning | works |
| Distance attenuation | works |
| Distance models (`AL_NONE` through `AL_EXPONENT_DISTANCE_CLAMPED`) | works, always clamped |
| Cone angles, min/max gain | refused |
| Doppler (`AL_VELOCITY`) | recorded, no effect |
| Streaming (`alSourceQueueBuffers`) | works |
| `ALC10` device and context | works |
| `WaveData` | works, uncompressed pcm |
| Capture (`ALC11`) | not emulated |

An entry point that is present but asked for something it does not emulate throws
`UnsupportedOperationException` naming the call. Nothing is silently ignored: a stack trace is
cheaper than debugging incorrect rendering.

An entry point that is **absent** cannot throw at all: the bundle simply does not compile, and the
compiler names the missing method. The list below is what an application is most likely to reach
for and not find.

### What is not here

`GL12`, `GL13`, `GL14`, `GLContext`, `ContextCapabilities` and `org.lwjgl.util.glu.GLU` do not
exist. Neither do these `GL11` entry points:

| Missing | Why |
|---------|-----|
| `glLight*`, `glMaterial*`, `glColorMaterial`, `glFog*`, `glShadeModel`, `glNormal3f`, `glNormalPointer` | fixed-function lighting and fog, which the translated pipeline does not implement |
| `glActiveTexture`, `glClientActiveTexture` | one texture unit is bound at a time |

If your application needs one of these, open an issue: the layer grows by what applications
actually call.

### Mipmaps

A texture carries as many levels as the application uploads. `glTexImage2D` and `glTexSubImage2D`
write the level they are given, `glGenerateMipmap` derives every level below the base, and
`GL_GENERATE_MIPMAP` rebuilds them after each later upload — WebGL has only the explicit call, so
the parameter is emulated by making it.

`GL_TEXTURE_MIN_FILTER` takes the four mipmapped filters. `GL_TEXTURE_MAG_FILTER` does not, because
magnification samples one level and there is nothing smaller to magnify from; asking for one is a
mistake rather than an unsupported feature, so it raises `IllegalArgumentException`.

`GL_CLAMP` is accepted as `GL_CLAMP_TO_EDGE`, which is the only clamp WebGL2 has.

A texture takes its parameters when its name is first bound, as desktop GL does, so re-uploading an
image no longer resets the filtering the application chose. Those defaults are `GL_NEAREST`
filtering on both axes, so a texture with only a base level samples correctly without being told to.

### GL_QUADS

WebGL2 has no `GL_QUADS`.

Flagon expands every quad into indexed triangles while preserving winding order. Incomplete quads
are discarded, matching desktop GL. The generated index buffer is uploaded only when it grows.

### Client arrays

WebGL cannot read client memory directly.

Pointers into NIO buffers are staged into internal buffer objects at draw time. Pointers into VBOs
are used directly.

Disabled arrays become constant generic attributes, matching fixed-function OpenGL. Temporary
buffer bindings are restored after every draw, so caller state is preserved.

Indexed `GL_QUADS` are refused rather than rewritten, because rewriting the caller's index buffer
would change application-owned data.

### Shader programs

`glUseProgram` switches between two rendering pipelines.

With a program bound, draws use generic vertex attributes and the application's shader. With no
program bound, draws use fixed-function client arrays and the built-in compatibility shader.

Immediate mode is refused while a program is bound. Desktop GLSL reads `gl_Vertex` and related
built-ins that GLSL ES does not provide, so attempting to emulate the combination would silently
produce incorrect rendering.

### Desktop GLSL

Desktop GLSL 1.10 and 1.20 are translated into GLSL ES where possible. Version directives are
removed, equivalent language features are preserved, and fragment shaders are given a default
precision qualifier when required.

GLSL 1.30+ and fixed-function GLSL built-ins (`gl_Vertex`,
`gl_ModelViewProjectionMatrix`, `ftransform()`, etc.) are refused. They depend on functionality
that does not exist in GL ES.

### Vertex attributes

WebGL requires attribute offsets and strides to be aligned to their component size.

Formats that are legal on desktop GL but illegal in WebGL throw
`UnsupportedCapabilityException` naming the offending value rather than failing with an opaque
`INVALID_OPERATION`.

### Audio activation

Browsers suspend `AudioContext` until a user gesture.

Flagon resumes audio after the first click or key press and reports the wait on the console.
Playback timing continues while suspended, so sources still reach `AL_STOPPED` and code waiting on
them is not stranded.

### Audio playback

A queued source schedules every buffer on the graph back to back, so the times between them are
the graph's and not a callback's. `AL_BUFFERS_PROCESSED` counts the ones the graph has reported
done, and `alSourceUnqueueBuffers` hands those back to be refilled.

A source holds a queue or a single buffer, never both, as OpenAL has it: mixing the two leaves
`AL_INVALID_OPERATION`.

Each call to `alSourcePlay` creates a new Web Audio source node. Gain, pitch and position are
stored separately so they survive stop and replay.

### Distance attenuation

Reference distance, rolloff factor and maximum distance map directly onto a `PannerNode`, and the
inverse, linear and exponential models onto its three curves. `AL_NONE` has no curve, so it is a
rolloff of zero; the source keeps the rolloff it was given.

The panner always clamps distance between the reference and maximum distances, so the unclamped
models behave as their clamped counterparts.

Velocity is recorded for compatibility but produces no Doppler effect because Web Audio removed
that functionality. `AL_SOURCE_RELATIVE` is emulated by offsetting the source relative to the
listener.

### Networking

A browser cannot open raw TCP sockets, so `java.net.Socket` is not in the classlib and an
application that uses it will not compile into a bundle. Networking here means
`dev.flagon.platform.net`, and it needs a proxy served alongside the page.

`Network.connect()` opens a websocket to a proxy, which owns the real socket and forwards bytes in
both directions. To callers, a `Connection` remains a blocking byte stream; reads suspend the
current green thread until data arrives.

The proxy is a server, not part of the bundle, and a statically hosted build that runs none has no
network at all — every `Network.connect()` fails, saying so. `vite dev` and `vite preview` serve
one; anywhere else, `web/proxy-server.ts` serves the built site and the same endpoint together:

```
bun run proxy -- --root dist --port 8080 --allow game.example.com:25565
```

The proxy decides which addresses may be reached: `--allow` replaces the loopback default, and every
entry names a port — `game.example.com:25565`, or `localhost:*` for any port of one host. It grants
the socket only to pages from its own origin, because browsers do not apply the same-origin policy
to websockets themselves; `--origin https://site.example.com` names another origin where the site
is hosted apart from the proxy.

That origin check binds browsers and nothing else: any client speaking raw HTTP sets both headers
itself, so a proxy anyone can reach is a proxy anyone can open sockets through. It therefore binds
to loopback unless given a `--token`, which the page must then carry as `?proxytoken=` on its url:

```
bun run proxy -- --root dist --host 0.0.0.0 --token "$FLAGON_PROXY_TOKEN" --allow game.example.com:25565
```

Keep `--allow` as narrow as the game actually needs. A public proxy left on the loopback default
tunnels callers into whatever else listens on its own host.

`java.net.URL` follows the same path. Flagon installs a `URLStreamHandlerFactory` at boot so
unmodified HTTP code transparently uses the active `Network` implementation.

Three capabilities remain unavailable:

- **HTTPS.** TLS over raw sockets is not implemented. HTTPS URLs fall back to TeaVM's XHR-based
  handler and remain subject to CORS.
- **Listening.** Browsers cannot accept inbound socket connections.
- **UDP.** The proxy transports streams only.

### Input

Input belongs to a `DisplaySurface`, so `InputSource` hangs from it. Events are queued and polled
rather than delivered through callbacks, matching desktop application structure.

`platform-api` defines the scancode numbering. Each host maps onto those values so applications
see consistent input regardless of platform.

The browser implementation requires no locking. DOM events execute only while the game loop is
suspended in `Frames.awaitNextFrame()`, so event queues are never modified while being drained.

### Browser input conventions

The browser differs from desktop input in three places:

- **Y axis.** DOM coordinates grow downward; desktop GL input grows upward.
- **Mouse buttons.** DOM orders buttons left, middle, right; LWJGL uses left, right, middle.
- **Mouse wheel.** DOM reports pixel deltas. Flagon accumulates them into 120-unit wheel notches
  so trackpads and wheels behave consistently.

Keys are identified by `KeyboardEvent.code`, preserving physical key positions regardless of
keyboard layout.

Pointer lock requires a user gesture and may be refused. `Mouse.setGrabbed()` reports whether the
request succeeded, and `isGrabbed()` is the authoritative state.

Event queues are bounded to 256 entries and discard the oldest events when full.

A touch is delivered as the left mouse button: it presses where it lands, moves while it drags and
releases where it lifts. Only the first touch of a gesture is followed, since a desktop application
has one pointer. There is no on-screen keyboard, so touch alone reaches only what a mouse can.

### Mouse.setCursorPosition

Browsers do not allow pages to warp the cursor.

A grabbed pointer has no position to set, and an ungrabbed pointer belongs to the user. The method
is therefore implemented as a no-op, matching the common desktop use of recentering an already
grabbed cursor.

### Buffer addresses

`MemoryUtil.getAddress()` and related methods throw
`UnsupportedCapabilityException`.

Buffers live in the managed heap and cannot be exposed as native pointers. `BufferUtils`
continues to allocate direct, native-endian buffers so typed views behave as desktop code expects.

### Frame timing

`Display.update()` suspends the current green thread and resumes on
`requestAnimationFrame()`. This depends on TeaVM's async transformation and only works from a
TeaVM-managed thread.

`Display.sync(fps)` holds the loop until the frame it asked for is due. A page has no way to
block, so it waits the only way it can: by presenting again, each present costing one
`requestAnimationFrame()`. Asking for a rate the host already runs at costs nothing, and asking
for a slower one spends the frames between deadlines presenting the same image, which the frame
counter behind the shell's fps badge counts. A loop that falls behind takes the rate it can
manage rather than drawing the frames it missed.

## Known gaps and shims

### A constant index past the end, under WASM GC

`strict` inserts bound checks on both backends, but an index the compiler can prove is out of range
escapes them under WASM GC: the access lowers to a bare `array.get`, which traps in the engine as a
`RuntimeError` no Java `catch` block can see, where the JavaScript backend throws
`ArrayIndexOutOfBoundsException`. An index the compiler cannot fold is checked correctly on both.

Reported as issue #35. The self test reaches past the end of an array through a value the compiler
cannot see through, so it tests the semantics rather than the optimizer.

### `NullPointerException`

TeaVM's JavaScript backend omits null checks unless `strict` mode is enabled, causing null
dereferences to surface as JavaScript `TypeError`s instead of catchable
`NullPointerException`s.

`strict` is enabled in the root POM despite the bundle-size cost. The self-test verifies that NPEs
remain catchable both directly and across call boundaries.

### `main(String[])`

TeaVM's generated entry point constructs `main`'s arguments as `Object[]` rather than `String[]`.

`BrowserPlatform.copyOf()` re-types the array at the entry point, and the self-test verifies the
conversion so regressions fail immediately.

### Vite module loading

The compiled bundle is a build artifact under `public/`.

Importing it directly causes Vite to treat it as a module and attempt a transform. `runBundle`
instead resolves the bundle to an absolute URL so the browser fetches it unchanged.