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
| Threading | works, cooperatively | green threads, with the executor half of `java.util.concurrent` supplied, see below |
| Networking | works through a proxy, sockets included | `Network.connect()` over a websocket, see below |
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
| `Class` identity, hierarchy, `isInstance`, `cast`, `forName` | works, see below |
| `getDeclaredFields`, `getDeclaredMethods`, `getConstructors` | answer empty unless the class opted in, see below |
| `Field.get`/`set`, `Method.invoke`, `Constructor.newInstance` | work on an opted-in class, refused otherwise |
| `Proxy.newProxyInstance` | works for an interface the build named, refused otherwise, see below |
| `File`, `FileInputStream`, `FileOutputStream`, `RandomAccessFile` | works, see below |
| `java.net.URL`, `HttpURLConnection` over HTTP | works through a proxy, see below |
| `java.net.Socket`, `InetAddress` | supplied by `socket-classlib`, carried by `Network.connect()` |
| `java.net.Proxy`, `Authenticator` | supplied by `socket-classlib`, carried but never routed by |
| `Socket.setSoTimeout` | recorded and readable, but a read still blocks |
| `ServerSocket` | refused: nothing can connect in to a page |
| `DatagramSocket` | absent; the proxy transports streams only |

### Reflection reaches types, not members

The compiler ships what it can see being reached, so a class keeps its identity -- `getName`,
`getSimpleName`, `getSuperclass`, `getInterfaces`, `isInstance`, `isAssignableFrom`, `cast`, and
`Class.forName` for a class something already reaches -- and `java.lang.reflect.Array` works.

Members do not survive by default. `getDeclaredFields`, `getDeclaredMethods` and `getConstructors`
return empty arrays rather than throwing, so code that walks the result does nothing. The layer
warns the first time a class answers one of these with nothing, names the class, and says how to
keep its members, so the failure is visible in the console instead of silent.
`Proxy.newProxyInstance` throws `SecurityException` for an interface nobody named.

A class can opt out of that, and then reflection over it behaves as it does on a JVM: every
declared field, method and constructor is listed, `Field.get`/`set` reach private fields,
`Method.invoke` and `Constructor.newInstance` work, and `Class.forName` finds the class by name.

Name the class one of two ways, both provided by `toolchain/reflect-policy`:

```java
@Reflectable
public final class Settings { ... }
```

```xml
<!-- for a jar you cannot annotate; globs, separated by commas or whitespace -->
<flagon.reflect.classes>com.example.Settings, com.example.model.*</flagon.reflect.classes>
```

A bundle gets neither unless it depends on `reflect-policy`. Keep the list small: a class named
here keeps everything it can reach, so naming an application wholesale gives up what ahead-of-time
compilation buys.

An interface `Proxy.newProxyInstance` may stand in for is named the same way, separately, because
a proxy is a class the compiler has to write rather than members it has to keep:

```xml
<flagon.reflect.proxies>com.example.Listener*</flagon.reflect.proxies>
```

### Mixin and runtime bytecode transform

Refused, and not by choice: a bundle is JavaScript or WebAssembly by the time it runs, so there is
no bytecode to rewrite, and the class loader has no `defineClass` to put a rewritten class into. A
class the compile never saw is not found. Fabric and Forge are built on exactly that, so a mod
loader cannot run here.

Applying mixins before the compile is a different question and stays open: merged into the jar
ahead of time they reach the compiler as ordinary classes, and the reflection above already loads
them. Until such a build step exists, treat the runtime as vanilla-only.

### What a ClassLoader can answer

`ClassLoader.loadClass` and `getResources` have no method in the browser classlib, so
`toolchain/jdk-shims` sends both to Flagon. `loadClass` asks `Class.forName`, which finds the
classes the build kept and reports the rest by name -- there is no loading at runtime, so a class
the compiler never saw is not there to find.

`getResources` finds nothing, and says so once per name. A bundle is one artifact: there is no
classpath with several copies of a file on it, and no URL to hand back for what is inside. Code
that scans for every `META-INF/services` entry finds none. `getResourceAsStream` reads a bundled
file and is the call that works.

### The rest of the JDK calls the classlib leaves out

`toolchain/jdk-shims` sends these to Flagon the same way, by rewriting the call rather than the
class behind it.

| Call | Answer |
|------|--------|
| `Runtime.maxMemory` | 512MB, what a tab is usually given; nothing in a page can read the real ceiling |
| `Runtime.exec` | refused; there is no operating system under a page to start a process on |
| `System.exit` | the same end `Runtime.exit` reaches, so the shutdown hooks run either way |
| `System.load`, `System.loadLibrary` | refused; a native library would have to be compiled into the bundle |
| `Class.getGenericSuperclass`, `getGenericInterfaces` | the plain class; generic types are erased by the time a bundle runs |
| `Class.getSigners` | empty; nothing signs a bundle |
| `Class.isAnonymousClass` | true for a name ending in a number, which is what the compiler gave it |
| `URL.openConnection(Proxy)` | the plain connection; the page reaches the network through the proxy it was served from and can pick no other |
| `Array.newInstance(Class, int...)` | an array of arrays, built one level at a time, because the classlib makes only one level at a time |

`toolchain/jdk-classlib` supplies whole classes the browser classlib has none of:
`java.sql.Date`, `Time` and `Timestamp`, which code that maps values by type names even when the
mapping never runs, and `java.lang.management.ManagementFactory`, whose uptime and heap numbers
come from the moment the bundle started and from `Runtime`, so a crash report prints something
true.

`java.net.IDN.toASCII` writes a host name in punycode. The browser does this itself for every URL
it fetches; the class is here for code that asks before it hands a name over.
`ServerSocket.getLocalPort` answers -1: no constructor here gets as far as a port.

Still missing: `java.util.UUID` has no bits and no `(long, long)` constructor. That needs a
transformer that builds method bodies, not one that rewrites a call.

### A jar nobody wrote

A jar you did not write cannot be annotated, and an obfuscated one cannot be globbed either: its
classes are named `adm` and `pk`. So `make app` does not ask. The build lists every class in
`vendor/` for the compiler, and each keeps its **constructors** -- which is what such a jar reflects
for: a registry handed a `Class` and asked to build one. Members beyond that still need a glob.

```xml
<!-- a file naming classes, one per line; the app build writes its own -->
<flagon.reflect.constructed>target/app-classes.txt</flagon.reflect.constructed>
```

Both backends behave identically, in both directions. The `selftest` program pins all of it --
including a class that is *not* opted in, sitting in the same bundle and still reporting zero
members -- so a TeaVM upgrade that changes any of it fails the suite rather than surprising an
application.

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

Both bindings ship. `lwjgl2-compat` carries `Display`, `Keyboard`, `Mouse`, `GL11`-`GL20` and
OpenAL; `lwjgl3-compat` carries `org.lwjgl.glfw` and `org.lwjgl.system`. They sit on the same
`lwjgl-core`, so an application takes whichever one it was written against and the other is left
out of the bundle by the compiler.

Verified in Chromium and Firefox on both backends by the `lwjgl`, `input`, `arrays`, `shaders`, `texture`, and
`audio` demos, which assert framebuffer output, drive real keyboard, mouse and wheel events, and
play audio through to completion. The compatibility layer is also covered by 251 JVM tests running against recording and
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
| Textures: gen/bind/delete, nearest and linear filtering | works |
| Texture uploads in `GL_RGBA`, `GL_RGB`, `GL_BGR`, `GL_BGRA`, `GL_LUMINANCE`, `GL_LUMINANCE_ALPHA`, `GL_ALPHA` | works, widened to rgba |
| Texture uploads of `GL_UNSIGNED_INT_8_8_8_8_REV` pixels in a four-component format | works |
| Texture uploads of any other pixel type | refused |
| `glTexParameteri` wrapping (`GL_REPEAT`, `GL_CLAMP`, `GL_MIRRORED_REPEAT`), min and mag filters separately | works |
| Mipmaps: uploads per level, `glGenerateMipmap`, `GL_GENERATE_MIPMAP`, base/max level | works |
| The four mipmapped minification filters | works |
| Alpha test | `GL_GREATER` only |
| `Keyboard` events, held state, key names, repeat toggle | works |
| `Mouse` events, buttons, position, movement, wheel, grabbing | works |
| `Mouse.setCursorPosition` | no-op, see below |
| `BufferUtils` allocation, `MemoryUtil` text encoding | works |
| `org.lwjgl.MemoryUtil.getAddress` (lwjgl 2) | refused, see below |
| `org.lwjgl.system.MemoryUtil`, `MemoryStack`, `PointerBuffer` (lwjgl 3) | works, see below |
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
| `glGetInteger` `GL_MAX_ELEMENTS_VERTICES` / `GL_MAX_ELEMENTS_INDICES` | works, unbounded |
| `glGetInteger`, `glGetFloat`, `glGetBoolean`, `glIsEnabled` of state the layer sets | works, answered from the mirror |
| Any other `glGetInteger`/`glGetString` name | refused |
| `glNormal3f`, `glNormalPointer`, `GL_NORMAL_ARRAY` | works |
| `GL_NORMALIZE`, `GL_RESCALE_NORMAL` | accepted, always in force |
| `glLight*`, `glLightModel` `GL_LIGHT_MODEL_AMBIENT` | works, eight lights, see below |
| `glMaterial*`, `glColorMaterial` | works, `GL_FRONT_AND_BACK` only |
| `glShadeModel` | `GL_SMOOTH` only |
| `glFog*` (`GL_LINEAR`, `GL_EXP`, `GL_EXP2`) | works |
| `glIndexPointer` | not emulated |
| `GL13` `glActiveTexture`, `glClientActiveTexture`, `glMultiTexCoord` | works, four units |
| `glTexEnvi` `GL_TEXTURE_ENV_MODE` | `GL_MODULATE`, `GL_REPLACE`, `GL_ADD` |
| `GL12` `glDrawRangeElements` | works, the range is checked and dropped |
| `GL14` `glBlendFuncSeparate`, `glBlendEquation`, `glBlendEquationSeparate`, `glBlendColor` | works |
| `GL14` `glMultiDrawArrays` | works, one draw per range |
| `GL14` `glPointParameter` `GL_POINT_SIZE_MIN`/`MAX` | works, clamps the point size |
| `GL14` `glPointParameter` fade and distance attenuation | refused |
| `GL20` shader/program objects, uniforms and generic attributes | works |
| Desktop GLSL 1.10 to 3.30 | works, translated |
| GLSL above 3.30 | refused |
| Fixed-function GLSL built-ins | works, translated, see below |
| `glBegin`/`glEnd` with a bound program | works for a fixed-function shader, else refused |
| A program supplying one shader stage | works, the other stage is written here |
| `AL` / `AL10` buffers, sources and listener | works |
| `alGetError` latching | works |
| Source gain, pitch, looping, positioning | works |
| Distance attenuation | works |
| Distance models (`AL_NONE` through `AL_EXPONENT_DISTANCE_CLAMPED`) | works, always clamped |
| Cone angles, min/max gain | refused |
| Doppler (`AL_VELOCITY`, `alDopplerFactor`, `alSpeedOfSound`) | works, computed here |
| Streaming (`alSourceQueueBuffers`) | works |
| `ALC10` device and context | works |
| `WaveData` | works, uncompressed pcm |
| Capture (`ALC11`) | not emulated |

An entry point that is present but asked for something it does not emulate throws
`UnsupportedOperationException` naming the call. Nothing is silently ignored: a stack trace is
cheaper than debugging incorrect rendering.

An entry point that is **absent** cannot throw at all: the bundle simply does not compile, and the
compiler names the missing method. If your application reaches for one, open an issue: the layer
grows by what applications actually call.

### GLU

`gluPerspective`, `gluLookAt`, `gluOrtho2D` and `gluPickMatrix` build their matrices out of the
`GL11` matrix calls, so they stack and pop with everything else. `gluProject` and `gluUnProject`
take the model-view and projection matrices and the viewport as buffers, the way lwjgl does, and
return false rather than throwing when the point has no answer.

`gluBuild2DMipmaps` uploads the base level and asks the device to build the rest, which is what a
driver does anyway. `gluScaleImage` resamples `GL_RGBA` bytes with a box filter and returns
`GLU_INVALID_ENUM` for any other format, rather than glu's bilinear filter over every format.
Both return zero on success, as glu does.

### Context capabilities

`GLContext.getCapabilities()` answers a `ContextCapabilities` built once per context. The
`OpenGL11` through `OpenGL20` flags are true because those classes are backed by real entry points
here; `OpenGL21` and above are false. The extension flags split in two: the ones WebGL2 carries in
core (multitexture, buffer objects, shaders, non-power-of-two textures, separate and constant
blending, framebuffer objects) are always true, and the rest are answered from the host's own
extension list, so `GL_EXT_texture_filter_anisotropic` is true only where the browser lists it.
`isSupported(String)` asks that list directly, with or without the `GL_` prefix.

`GLContext.useContext(Object)` rereads the list. There is one context here, the one
`Display.create()` opened, so the argument names it and is otherwise unused.

### The ARB and EXT names

Code written before an extension went into core calls it by its extension name.
`ARBMultitexture`, `ARBVertexBufferObject`, `ARBShaderObjects`, `ARBVertexShader`,
`ARBFramebufferObject`, `EXTFramebufferObject` and `EXTBlendFuncSeparate` are here, and each call
goes to the core call of the same shape. `ARB_shader_objects` holds programs and shaders in one
handle space, so `glDeleteObjectARB`, `glGetObjectParameteriARB` and `glGetInfoLogARB` ask which
of the two the handle is first.

`org.lwjgl.util.vector` carries `Vector3f`, `Vector4f` and `Matrix4f`, and `org.lwjgl.util.glu`
carries `Project` beside `GLU`. The matrix math is the same code the fixed-function pipeline uses.

### What the fixed-function pipeline has no stage for

`glTexGen`, `glTexGeni` and `glLogicOp` throw `UnsupportedOperationException` and say why. WebGL
generates no texture coordinates, so the coordinate array or `glTexCoord` has to carry them, and
it combines a fragment with the framebuffer by blending only. `glTexEnv` with a buffer takes
`GL_TEXTURE_ENV_MODE` and refuses the rest: the compatibility shader has no use for an
environment colour.

`glPixelStorei` keeps `GL_UNPACK_ROW_LENGTH`, `GL_UNPACK_SKIP_ROWS` and `GL_UNPACK_SKIP_PIXELS`
and picks that rectangle out of the buffer before the upload reaches the device.
`GL_UNPACK_SWAP_BYTES` and `GL_UNPACK_LSB_FIRST` are refused: a pixel is read here in the order it
lies.

`glGetTexImage` attaches the level to a framebuffer of its own and reads it back from there,
because WebGL reads back from a framebuffer only. It puts the texture and framebuffer that were
bound back before it returns. `glGetTexLevelParameteri` answers `GL_TEXTURE_WIDTH` and
`GL_TEXTURE_HEIGHT` from the last upload to that level, and throws if nothing was uploaded yet.

`Display.setIcon` does nothing and answers 0: a tab takes its icon from the page.

### Lighting, materials and fog

The fixed-function light model is evaluated in the vertex shader, per vertex, the way desktop GL
evaluates it. Eight lights are available. A light's position is carried into eye space by the
modelview matrix in force when `glLight` sets it, so moving the camera afterwards leaves the light
where it was put; `w == 0` makes it directional. Constant, linear and quadratic attenuation all
apply to a positional light.

One material is kept, not one per face, so `glMaterial` and `glColorMaterial` take
`GL_FRONT_AND_BACK` and refuse anything narrower. `glColorMaterial` takes `GL_AMBIENT_AND_DIFFUSE`,
the mode applications actually use. `glShadeModel` takes `GL_SMOOTH`: flat shading needs a
provoking vertex, which GL ES does not expose.

Fog is applied per fragment along the eye-space distance, in all three of GL's curves, and reaches
the color but never the alpha.

`glPushAttrib` saves whether lighting, fog, color material and each light are enabled, and the
current normal. It does not save light or material parameters — `GL_LIGHTING_BIT` and `GL_FOG_BIT`
are not emulated.

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

Immediate mode is refused while a program is bound, unless that program's shaders read the
fixed-function state themselves -- see below.

### Desktop GLSL

Desktop GLSL 1.10 and 1.20 become GLSL ES 1.00: the version directive is removed, equivalent
language features are preserved, and fragment shaders are given a default precision qualifier
when required.

GLSL 1.30 to 3.30 — the core-profile versions, 1.50 among them — become GLSL ES 3.00, which is
what WebGL2 compiles:

| Written | Compiled |
|---------|----------|
| `#version 150 core` | `#version 300 es` |
| `texture2D`, `texture2DLod`, `texture2DProj`, `textureCube`, `texture3D`, `shadow2D` | `texture`, `textureLod`, `textureProj` |
| `gl_FragColor` | a declared `out vec4 flagon_FragColor` |
| a fragment shader with no `precision float` | `precision highp float;` |
| `gl_FragData` | refused; the page draws to one colour buffer |

`in` and `out` are already what those versions say, so they are left alone, and an `#extension`
directive keeps its place ahead of the injected declarations. GLSL above 3.30 is refused.

### Fixed-function GLSL

GLSL ES has no `gl_Vertex`, no `gl_ModelViewProjectionMatrix` and no `ftransform()`, but the
fixed-function pipeline here already keeps that state and already feeds it to its own shader. A
shader naming any of it is promoted to GLSL ES 3.00 and rewritten to read the same attributes and
uniforms, and the program it links into then draws through that pipeline -- so `glBegin`,
`glVertexPointer` and `glColor` feed it, exactly as they do on the desktop.

| Written | Read as |
|---------|---------|
| `gl_Vertex`, `gl_Color`, `gl_Normal` | the position, colour and normal client arrays |
| `gl_MultiTexCoord0` to `gl_MultiTexCoord3` | the four texture coordinate arrays |
| `gl_ModelViewMatrix`, `gl_ProjectionMatrix`, `gl_ModelViewProjectionMatrix`, `gl_NormalMatrix` | the matrices `glLoadMatrix` and friends set |
| `ftransform()` | the same product, as a declared function |
| `gl_FrontColor`, `gl_BackColor`, `gl_TexCoord[]`, `gl_FogFragCoord` | varyings the layer declares in both stages |
| `gl_Fog` | a struct uniform the layer uploads from `glFog*` |
| `attribute`, `varying` | `in` and `out` |

`gl_LightSource`, `gl_TextureMatrix`, `gl_SecondaryColor` and `gl_ClipPlane` are still refused,
naming which one was asked for: nothing here feeds them. Lighting still works for the built-in
pipeline -- it is reading the lights *from a shader of your own* that has no equivalent.

GL links whichever stages a program was given and leaves the rest fixed-function. WebGL needs
both, so the missing stage is written here over the same varyings, and the program links and draws.
A fragment shader left alone this way carries fog only when the vertex stage writes
`gl_FogFragCoord`, because nothing else can supply the distance. A fragment shader that reads a
varying of its own is refused, naming it: no written vertex stage can know what to put there.

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

Web Audio removed its own Doppler, so the shift is computed here instead: both velocities are
measured along the line from the source to the listener, each clamped to the speed sound travels,
and the ratio OpenAL specifies multiplies the source's playback rate. `alDopplerFactor(0)`
switches it off and `alSpeedOfSound` (or `alDopplerVelocity`, which names the same speed) sets what
it is measured against. `AL_PITCH` still reads back what the caller set, not the shifted rate.

`AL_SOURCE_RELATIVE` is emulated by offsetting the source relative to the listener.

### Networking

A browser cannot open raw TCP sockets, so networking here means `dev.flagon.platform.net`, and it
needs a proxy served alongside the page.

TeaVM's classlib has no `java.net.Socket`, so the `socket-classlib` module supplies one, along with
`InetAddress`, `InetSocketAddress`, `ServerSocket` and the socket exceptions. It supplies `Proxy`,
`Authenticator` and `PasswordAuthentication` too: an application that picks its own proxy passes it
around unchanged, though the only proxy a page really goes through is the one the shell serves. A bundle that depends
on it can run unmodified socket code; the class is a shell over `NetworkSocket`, which is the same
`Network.connect()` everything else here goes through. Options a page does not own -- keep alive, no
delay, buffer sizes, traffic class -- are accepted and ignored, and `ServerSocket` refuses at the
call rather than being absent.

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

Both `Origin` and `Host` come from whoever is asking, so matching them to each other proves
nothing on its own: a name an attacker controls that resolves to 127.0.0.1 matches itself, which is
how DNS rebinding reaches a loopback service. The upgrade is therefore refused unless `Host` names
loopback, the address the proxy was told to bind, or an explicit `--allowed-host`.

That origin check binds browsers and nothing else: any client speaking raw HTTP sets both headers
itself, so a proxy anyone can reach is a proxy anyone can open sockets through. It therefore binds
to loopback unless given a `--token`, which the page must then carry as `?proxytoken=` on its url:

```
bun run proxy -- --root dist --host 0.0.0.0 --token "$FLAGON_PROXY_TOKEN" --allow game.example.com:25565
```

Keep `--allow` as narrow as the game actually needs. A public proxy left on the loopback default
tunnels callers into whatever else listens on its own host.

Every file it serves carries a content security policy, `nosniff`, `no-referrer` and
`frame-ancestors 'none'`. The policy keeps the page to its own origin for scripts, styles, fetches
and sockets; it has to allow `unsafe-eval` because TeaVM's WebAssembly loader evaluates a string.

`java.net.URL` follows the same path. Flagon installs a `URLStreamHandlerFactory` at boot so
unmodified HTTP code transparently uses the active `Network` implementation.

#### HTTPS

A page cannot speak TLS down a socket it does not own, so the proxy does the handshake for it.
`Network.connectSecure()` asks for it, `https://` URLs through `java.net.URL` take it by
themselves, and the proxy checks the certificate the way any other client would. A server whose
authority the host does not already ship needs `--tls-ca <file>`.

```
bun run proxy -- --root dist --allow sessionserver.mojang.com:443 --allow api.mojang.com:443
```

Know what this does and does not buy. The leg from the far server to the proxy is real TLS with a
real certificate check. The leg from the proxy to the page is a WebSocket carrying plaintext, so it
is private only as far as whatever carries it -- `wss://` from a page served over HTTPS, or
loopback in development. Anyone who can read that hop reads the session token with it. So run the
proxy yourself, serve it over HTTPS wherever it is not loopback, and keep `--allow` to the hosts
the game actually needs.

`HttpsURLConnection` itself is not supplied: `url.openConnection()` gives an `HttpURLConnection`
that happens to be encrypted, so code that casts to the `javax.net.ssl` type, pins a certificate,
or asks for the cipher suite does not get what it wants.

Three capabilities remain unavailable:

- **Listening.** Browsers cannot accept inbound socket connections, so `ServerSocket` refuses.
- **Read timeouts.** A `Connection` read blocks in the host with no way to hand it a deadline, so
  `setSoTimeout` is kept for whoever reads it back and a read waits rather than giving up.
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

### GLFW

`glfwInit`, `glfwCreateWindow`, `glfwSwapBuffers`, `glfwPollEvents`, `glfwWindowShouldClose` and
the key, char, mouse-button, cursor, scroll, size, focus and close callbacks work, over the same
surface `Display` uses. Verified in the browser by the `glfw` demo.

The page owns the window, so some of GLFW's surface is answered rather than obeyed:

| Call | Behaviour |
|------|-----------|
| `glfwWindowHint` | remembered, not obeyed; the page decides the context it can give |
| `glfwCreateWindow` a second time | refused, and `glfwGetError` reports why |
| `glfwSetWindowPos`, `glfwIconifyWindow`, `glfwSetCursorPos` | no-ops |
| `glfwSetWindowMonitor` | asks the surface for fullscreen, which the host may refuse |
| `glfwSetInputMode(GLFW_CURSOR, GLFW_CURSOR_DISABLED)` | asks for a pointer lock; read `glfwGetInputMode` back, since a browser may require a gesture and refuse |
| `glfwJoystickPresent` | false; no gamepad is wired up |
| `glfwGetKeyName` | null |

Keys reach callers in GLFW's own numbering, translated from the host scancode; the `scancode`
argument of the key callback carries the host's number, as GLFW's does.

### What third-party LWJGL 3 code needs

`examples/thirdparty` holds LWJGL's own guide sample, compiled against the real
`org.lwjgl:lwjgl`, `lwjgl-glfw` and `lwjgl-opengl` artifacts exactly as a desktop project would,
and bundled against this layer instead. It runs in the browser, verified by the `hello-lwjgl3`
program. Running it first found two things this layer had wrong:

- `GLFW` defined `NULL`. Real LWJGL leaves that to `MemoryUtil`, so a program static-importing both
  would not compile. `NULL` now belongs to `MemoryUtil` alone.
- `glfwTerminate()` cleared the error callback, so the documented
  `glfwTerminate(); glfwSetErrorCallback(null).free();` threw. GLFW keeps the callback across a
  terminate, and now so does this.

`org.lwjgl.Version` and `org.lwjgl.glfw.Callbacks` (`glfwFreeCallbacks`) were added for the same
program. What it still refuses is `GLFWAllocator` and `glfwInitAllocator`: those hand GLFW a real
`malloc`, and an address here names a registered buffer rather than memory.

### GL 3.2 core profile

`GL30`, `GL31`, `GL32` and `GL33` in `lwjgl3-compat` cover what a core-profile client calls:
vertex array objects, `glGetStringi`, `glVertexAttribIPointer`, the instanced draws and
`glVertexAttribDivisor`. `GL.createCapabilities()` reports 3.2 with the 3.3 entry points present,
and carries the extension names the context reports. Verified in the browser by the
`core-profile` demo.

`GL11` through `GL20` are the same classes for both bindings and ship in `lwjgl2-compat`; put
both artifacts on the classpath and the AOT compiler drops whatever the application never reaches.

LWJGL 3 splits each version in two -- `GL11C` holds the core profile, `GL11` extends it and adds
the fixed-function half -- and modern code imports the `C` classes by name. The same split is here,
with the same inheritance, so `GL11C.glClear` and `GL11.glClear` are one method and nothing is
duplicated. `GL11C` through `GL15C`, `GL20C` and `GL30C` through `GL33C` are all present. What sits
on which side is held by a test: nothing gl 3.2 core removed may appear on a `C` class.

| Call | Behaviour |
|------|-----------|
| `glDrawElementsBaseVertex` and its variants | refused; webgl2 reads every index as written, and rewriting the index buffer belongs to the caller |
| `glBindFragDataLocation` | accepted for colour 0, refused above it; the shader declares its own output location |
| `glVertexAttribIPointer` | needs a bound `GL_ARRAY_BUFFER`; the core profile has no client-side arrays |
| `glDrawArraysInstanced`, `glDrawElementsInstanced` | need a program bound with `glUseProgram`, and refuse `GL_QUADS` |
| `glIsVertexArray` | true only for the array currently bound |

### Framebuffer objects

`GL30` opens framebuffers, renderbuffers and their attachments: `glGenFramebuffers`,
`glBindFramebuffer`, `glFramebufferTexture2D`, `glFramebufferRenderbuffer`,
`glCheckFramebufferStatus`, `glRenderbufferStorage` and `glGenerateMipmap`. Rendering into a
texture and reading it back is verified in the browser by the `framebuffer` demo.
`glTexImage2D` with a null buffer reserves a level, which is how a colour attachment is made.

| Call | Behaviour |
|------|-----------|
| `GL_COLOR_ATTACHMENT1` and above | refused; the page draws to one colour buffer |
| `glFramebufferTexture2D` with a target other than `GL_TEXTURE_2D` | refused |
| `glRenderbufferStorage` formats | `GL_RGBA8`, `GL_DEPTH_COMPONENT16/24`, `GL_DEPTH24_STENCIL8`, `GL_STENCIL_INDEX8`; anything else is refused |
| `glIsFramebuffer` | true only for the framebuffer currently bound |
| deleting the bound framebuffer | binds the surface back, as gl does |

### Buffer addresses

LWJGL 2's `org.lwjgl.MemoryUtil.getAddress()` throws `UnsupportedCapabilityException`. Nothing in
LWJGL 2 needs the number back, so refusing beats inventing one.

LWJGL 3 is built on addresses, so `org.lwjgl.system.MemoryUtil` hands them out. They are not
pointers: the high half names a slot in a table of registered buffers and the low half a byte
offset into it. Adding to one walks the buffer, `memByteBuffer` turns one back into the bytes it
came from, and an address this build never handed out is refused rather than followed. `MemoryStack`
carves its frames out of one arena and forgets their addresses when the frame closes.

Nothing outside the program can read an address, so a jar that passes one to real native code has
nowhere to send it. `BufferUtils` and `memAlloc` still allocate direct, native-endian buffers so
typed views behave as desktop code expects.

### Threads, executors and futures

TeaVM gives green threads: `Thread`, `synchronized`, `wait`/`notify`, `sleep`, `join`, the atomics,
`ConcurrentHashMap` and the blocking queues all work. They are cooperative, not parallel -- one
runs at a time and a switch happens only where one suspends -- so threads here buy ordering and
responsiveness, never a second core.

What TeaVM has no classes for at all is the half a modern client is built on: `ExecutorService`,
`Executors`, `Future`, `CompletableFuture`, `CountDownLatch`, `ReentrantLock`,
`ReentrantReadWriteLock`, `ConcurrentLinkedQueue`. `toolchain/concurrent-classlib` supplies those over the green threads, the
way `socket-classlib` supplies `java.net`. Depend on it and a render thread draining what worker
threads produced runs unchanged; the `threading` demo runs exactly that shape in the browser.

| Written | Behaviour |
|---------|-----------|
| `Executors.newFixedThreadPool(n)` | `n` green worker threads over one task queue |
| `Executors.newCachedThreadPool`, `newWorkStealingPool` | bounded to `availableProcessors()`; more green threads buy nothing |
| `CompletableFuture` chains (`supplyAsync`, `thenApply`, `thenCompose`, `thenCombine`, `allOf`, `exceptionally`) | work, on the shared pool |
| `future.get()`, `latch.await()`, `lock.lock()` | suspend the calling green thread, so the frame loop keeps running |
| `ReentrantReadWriteLock` | many readers or one writer, both reentrant |
| taking the write lock while holding the read lock | refused by name; the wait it would cost has nothing left to end it |
| `ForkJoinPool` | absent; ask for an executor instead |
| a task that never suspends | holds the page until it returns, exactly as it would hold a real thread's core |

The consequence worth planning around: work is interleaved, not parallel. A chunk pipeline whose
stages suspend keeps the frame loop drawing, and one that computes straight through for 40ms costs
a frame.

### Shutdown hooks

`Runtime.addShutdownHook` has no method in the browser classlib, so `toolchain/jdk-shims` sends it,
`removeShutdownHook`, `exit` and `halt` to Flagon at compile time. The hooks run when `main`
returns, when the program stops with an exception, and when the program calls `Runtime.exit`.
`halt` skips them, the way it does on a desktop JVM.

They run one after another on the calling thread, in the order they were added, because green
threads are cooperative. A hook that throws is reported and does not stop the rest.

Closing or reloading the tab does **not** run them. A page being torn down is not a safe place to
re-enter the runtime -- doing it aborts the navigation on the WebAssembly backend -- and a browser
gives an unload handler no time it has to honour anyway. So a hook that saves a world on exit runs
when the program ends, and does not run when the reader closes the tab. Save on a timer as well.

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