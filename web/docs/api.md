# API reference

Every public type in `runtime`, `platform/api`, `platform/browser`, `platform/wasi` and `@Reflectable`. The [stable API](packaging.md#stable-api) is `FlagonRuntime`, the program types, `BrowserPlatform.create` and `@Reflectable`. Everything else may change between releases.

`platform/api` holds the host contracts and the targets of the build's JDK reroutes. Application code does not call the reroute targets: the build sends JDK calls to them.

## dev.flagon.runtime

### ConsoleStreams

Routes `System.out` and `System.err` into the platform console, one line at a time.

```java
class ConsoleStreams
    static void install(ConsoleSink console);
    static void restore();
```

### FlagonRuntime

Boots a platform and runs one program. `boot` installs the platform, routes `System.out` and URLs, picks the program from the first argument, runs it, and reports each phase to the host. It returns 0, or -1 when a phase throws.

```java
class FlagonRuntime
    static final String PHASE_PLATFORM;
    static final String PHASE_SELECT;
    static final String PHASE_PROGRAM;
    static int boot(Platform platform, ProgramRegistry registry);
```

### MainProgram

A `Program` that calls a `main(String[])` method reference.

```java
class MainProgram implements Program
    MainProgram(String id, String description, Main main);
```

### MainProgram.Main

A `main(String[])` method. Use a method reference, so the compiler keeps the call graph.

```java
interface MainProgram.Main
    void run(String[] arguments);
```

### Program

One runnable program in a bundle.

```java
interface Program
    String id();
    String description();
    void run(String[] arguments);
```

### ProgramCatalog

Writes the registry as JSON for the page. The build runs it on the JVM.

```java
class ProgramCatalog
    static String json(ProgramRegistry registry);
    static void writeTo(ProgramRegistry registry, String path);
```

### ProgramRegistry

The programs in a bundle, in registration order. The first is the default.

```java
class ProgramRegistry
    ProgramRegistry register(Program program);
    Program find(String id);
    List<Program> all();
    String ids();
    boolean isEmpty();
```

## dev.flagon.runtime.net

### NetworkSocket

The `java.net.Socket` implementation over the platform `Network`.

```java
class NetworkSocket
    NetworkSocket();
    NetworkSocket(Network network);
    void connect(String host, int port, int timeoutMillis);
    boolean isConnected();
    boolean isClosed();
    boolean isInputShutdown();
    boolean isOutputShutdown();
    String host();
    int port();
    int getSoTimeout();
    void setSoTimeout(int millis); // records the read timeout, which reads here cannot act on
    InputStream inputStream();
    OutputStream outputStream();
    void shutdownInput();
    void shutdownOutput();
    void close();
```

### NetworkUrls

Installs the `URLStreamHandlerFactory` that opens `http` and `https` URLs over the platform `Network`.

```java
class NetworkUrls
    static void install(Network network, ConsoleSink console);
```

## dev.flagon.reflect

### Reflectable

Keeps the class's fields, methods, constructors and reflection metadata.

```java
@interface Reflectable
```

## dev.flagon.platform

### Clock

Wall-clock and monotonic time.

```java
interface Clock
    long currentTimeMillis();
    long nanoTime();
```

### ConsoleSink

Where log lines go.

```java
interface ConsoleSink
    void write(Level level, String message, Throwable error);
    default void debug(String message);
    default void info(String message);
    default void warn(String message);
    default void error(String message, Throwable error);
```

### ConsoleSink.Level

A log level.

```java
enum ConsoleSink.Level
    static final Level DEBUG;
    static final Level INFO;
    static final Level WARN;
    static final Level ERROR;
```

### Names

Objects under the small integer names GL and AL hand out. A freed name is used again before a new one.

```java
class Names<T> implements Iterable<T>
    Names();
    int add(T object);
    void put(int name, T object); // puts an object under a name the caller chose, freeing any gap before it
    T get(int name);
    boolean has(int name);
    T remove(int name);
    int size(); // one past the highest name in use
    void clear();
```

### Platform

The services a host gives the runtime. `display` opens the one display surface.

```java
interface Platform
    String name();
    Clock clock();
    ConsoleSink console();
    RuntimeHost host();
    FileStore files();
    AudioDevice audio();
    Network network();
    default Fetch fetch(); // how java.net.http sends requests; refuses them by default.
    default Drawing drawing(); // the 2d canvas java.awt draws on; refuses by default.
    default Workers workers(); // bundle workers; refuses by default.
    boolean hasDisplay();
    DisplaySurface display(int width, int height, String title); // opens the window or returns current.
```

### Platforms

Holds the installed `Platform` for static LWJGL code.

```java
class Platforms
    static void install(Platform platform);
    static Platform get();
    static boolean isInstalled();
    static void reset();
```

### RuntimeHost

The page that runs the bundle. The runtime reports its phases here.

```java
interface RuntimeHost
    String[] arguments();
    void catalog(String[] ids, String[] descriptions);
    void status(String phase, String detail);
    void ready();
    void exited(int code);
    void fatal(String phase, Throwable error); // terminal: no further callbacks follow.
```

### UnsupportedCapabilityException

Thrown when the host has no way to do what was asked.

```java
class UnsupportedCapabilityException extends RuntimeException
    UnsupportedCapabilityException(String message);
```

## dev.flagon.platform reroute targets

The build rewrites JDK calls to these methods. A call on an instance passes the instance as the first argument.

### Arrays

`java.lang.reflect.Array` calls TeaVM lacks. The build reroutes them here.

```java
class Arrays
    static Object newInstance(Class<?> type, int length); // the one-level form, which the classlib does have, so this only hands it over
    static Object newInstance(Class<?> type, int[] lengths); // an array of arrays, built one level at a time, because the classlib makes only one level at a time.
    static boolean getBoolean(Object array, int index);
    static byte getByte(Object array, int index);
    static char getChar(Object array, int index);
    static short getShort(Object array, int index);
    static int getInt(Object array, int index);
    static long getLong(Object array, int index);
    static float getFloat(Object array, int index);
    static double getDouble(Object array, int index);
    static void setBoolean(Object array, int index, boolean value);
    static void setByte(Object array, int index, byte value);
    static void setChar(Object array, int index, char value);
    static void setShort(Object array, int index, short value);
    static void setInt(Object array, int index, int value);
    static void setLong(Object array, int index, long value);
    static void setFloat(Object array, int index, float value);
    static void setDouble(Object array, int index, double value);
```

### Charsets

`Charset.isSupported` without the JDK provider lookup.

```java
class Charsets
    static boolean isSupported(String name);
```

### ClassLoaders

`ClassLoader` class and resource lookups, rerouted by the build.

```java
class ClassLoaders
    static Class<?> loadClass(ClassLoader loader, String name);
    static InputStream getResourceAsStream(ClassLoader loader, String name);
    static Enumeration<URL> getResources(ClassLoader loader, String name);
    static URL getResource(ClassLoader loader, String name);
    static URL getSystemResource(String name);
```

### Classes

`Class` reflection calls TeaVM lacks or answers wrongly, rerouted by the build.

```java
class Classes
    static Method[] getDeclaredMethods(Class<?> type); // without the method the build adds to carry a class's own reflection data
    static Constructor<?>[] getDeclaredConstructors(Class<?> type); // without the constructor the build adds for java serialization
    static Constructor<?>[] getConstructors(Class<?> type);
    static Method[] getMethods(Class<?> type);
    static Type getGenericSuperclass(Class<?> type);
    static Type[] getGenericInterfaces(Class<?> type);
    static TypeVariable<?>[] getTypeParameters(Class<?> type);
    static boolean isRecord(Class<?> type);
    static boolean isSealed(Class<?> type);
    static Class<?>[] getPermittedSubclasses(Class<?> type);
    static Method getEnclosingMethod(Class<?> type);
    static Constructor<?> getEnclosingConstructor(Class<?> type);
    static <A extends Annotation> A getDeclaredAnnotation(Class<?> type, Class<A> asked);
    static <A extends Annotation> A[] getAnnotationsByType(Class<?> type, Class<A> asked);
    static <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<?> type, Class<A> asked);
    static String getPackageName(Class<?> type);
    static Class<?> arrayType(Class<?> type);
    static Class<?> componentType(Class<?> type);
    static String descriptorString(Class<?> type);
    static boolean isSynthetic(Class<?> type);
    static boolean isHidden(Class<?> type);
    static Class<?> getNestHost(Class<?> type);
    static boolean isNestmateOf(Class<?> type, Class<?> other);
    static Object[] getSigners(Class<?> type);
    static ProtectionDomain getProtectionDomain(Class<?> type);
    static URL getResource(Class<?> type, String name);
    static InputStream getResourceAsStream(Class<?> type, String name);
    static boolean isAnonymousClass(Class<?> type);
```

### Fields

Typed `Field` reads and writes, rerouted by the build. They widen as the JDK does.

```java
class Fields
    static boolean getBoolean(Field field, Object holder);
    static byte getByte(Field field, Object holder);
    static char getChar(Field field, Object holder);
    static short getShort(Field field, Object holder);
    static int getInt(Field field, Object holder);
    static long getLong(Field field, Object holder);
    static float getFloat(Field field, Object holder);
    static double getDouble(Field field, Object holder);
    static void setBoolean(Field field, Object holder, boolean value);
    static void setByte(Field field, Object holder, byte value);
    static void setChar(Field field, Object holder, char value);
    static void setShort(Field field, Object holder, short value);
    static void setInt(Field field, Object holder, int value);
    static void setLong(Field field, Object holder, long value);
    static void setFloat(Field field, Object holder, float value);
    static void setDouble(Field field, Object holder, double value);
```

### Files

`getChannel` for file streams.

```java
class Files
    static FileChannel getChannel(FileInputStream stream);
    static FileChannel getChannel(FileOutputStream stream);
```

### KeptClasses

The classes the build kept, for class path scans.

```java
class KeptClasses
```

### Maps

`Collections` map wrappers TeaVM lacks.

```java
class Maps
    static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, V> map);
```

### Methods

`Method` calls TeaVM lacks, rerouted by the build.

```java
class Methods
    static boolean isDefault(Method method); // teavm reports every interface method as abstract, so the build notes which have a body
    static Object getDefaultValue(Method method); // the default of an annotation member, as the build wrote it, or null
    static <A extends Annotation> A[] getAnnotationsByType(Method method, Class<A> asked);
    static <A extends Annotation> A[] getDeclaredAnnotationsByType(Method method, Class<A> asked);
```

### Packages

`Package` manifest attributes. Each returns `null`: a bundle has no manifest.

```java
class Packages
    static String getSpecificationTitle(Package of);
    static String getSpecificationVersion(Package of);
    static String getSpecificationVendor(Package of);
    static String getImplementationTitle(Package of);
    static String getImplementationVersion(Package of);
    static String getImplementationVendor(Package of);
```

### ReflectionWatch

Wraps member listings. When a class was not kept for reflection, it logs how to keep it.

```java
class ReflectionWatch
    static Method[] declaredMethods(Class<?> type);
    static Method[] methods(Class<?> type);
    static Field[] declaredFields(Class<?> type);
    static Field[] fields(Class<?> type);
    static Constructor<?>[] declaredConstructors(Class<?> type);
    static Constructor<?>[] constructors(Class<?> type);
```

### ResourceUrls

`flagon:` URLs for files in the resource pack.

```java
class ResourceUrls
    static final String SCHEME;
    static URL of(String name);
```

### Resources

The resource pack and the files kept beside it.

```java
class Resources
    static Source of(byte[] bytes);
    static void clear(); // the pack is gone; every lookup finds nothing
    static void install(byte[] bytes);
    static void install(Source source);
    static void install(Source source, Fetcher apart);
    static boolean isShippedFile(String path); // whether the program shipped a file at this path beside its jars
    static boolean isShippedFolder(String path);
    static String[] listShipped(String path); // the names in a shipped folder, or an empty array when it is none
    static Set<String> apart(); // the hashes of the resources kept beside the pack
    static int count();
    static Set<String> names();
    static boolean has(String name);
    static byte[] read(String name);
    static int readInto(String name, byte[] into);
    static InputStream open(String name); // a resource kept apart is fetched on the first read, so opening one only checks it
    static int sizeOf(String name);
```

### Resources.Source

The pack's bytes, read where they arrived.

```java
interface Resources.Source
    int length();
    byte get(int at);
    void copy(int from, byte[] into, int to, int count);
```

### Resources.Fetcher

Starts the download of a file kept beside the pack, by its SHA-256 name. It must not wait.

```java
interface Resources.Fetcher
    byte[] fetch(String hash);
```

### Runtimes

`Runtime.maxMemory` and `Runtime.exec`, rerouted by the build.

```java
class Runtimes
    static long maxMemory(Runtime runtime);
    static Process exec(Runtime runtime, String command);
    static Process exec(Runtime runtime, String[] command);
```

### ShutdownHooks

`addShutdownHook`, `removeShutdownHook`, `exit` and `halt`, rerouted by the build.

```java
class ShutdownHooks
    static void add(Runtime runtime, Thread hook);
    static boolean remove(Runtime runtime, Thread hook);
    static void exit(Runtime runtime, int code);
    static void halt(Runtime runtime, int code);
    static void runAll();
    static int pending();
    static void reset();
```

### Systems

`System.exit`, `loadLibrary` and `load`, rerouted by the build.

```java
class Systems
    static void exit(int status);
    static void loadLibrary(String name);
    static void load(String path);
    static String mapLibraryName(String name);
```

### Urls

`URL.openConnection(Proxy)`, rerouted by the build.

```java
class Urls
    static URLConnection openConnection(URL url, Proxy proxy);
```

### Uuids

`UUID` constructors and accessors TeaVM lacks.

```java
class Uuids
    static UUID of(long high, long low);
    static UUID fromString(String text); // the jdk's reading: five dashed hex fields, each masked to its width
    static long getMostSignificantBits(UUID id);
    static long getLeastSignificantBits(UUID id);
    static UUID nameUUIDFromBytes(byte[] name);
```

## dev.flagon.platform.audio

### Audio

PCM frame arithmetic for `AudioDevice` implementations.

```java
class Audio
    static void requireWholeFrames(AudioFormat format, int length, int sampleRate);
    static void requireWholeFrames(AudioFormat format, int length);
    static int frames(AudioFormat format, int lengthInBytes);
    static double durationInSeconds(AudioFormat format, int lengthInBytes, int sampleRate);
    static float[] channelSamples(AudioFormat format, byte[] data, int length, int channel);
```

### AudioDevice

A mixer: sources play PCM buffers at positions in space. Buffers and sources are integer names.

```java
interface AudioDevice
    int NO_BUFFER;
    int NO_SOURCE;
    String name();
    boolean isOutputLive();
    int createBuffer();
    void bufferData(int buffer, AudioFormat format, byte[] data, int length, int sampleRate);
    void deleteBuffer(int buffer);
    boolean isBuffer(int buffer);
    AudioFormat bufferFormat(int buffer);
    int bufferSampleRate(int buffer);
    int bufferSizeInBytes(int buffer);
    int createSource();
    void deleteSource(int source);
    boolean isSource(int source);
    void attachBuffer(int source, int buffer);
    int attachedBuffer(int source);
    void queueBuffers(int source, int[] buffers);
    int[] unqueueBuffers(int source, int count);
    int buffersQueued(int source);
    int buffersProcessed(int source);
    void play(int source);
    void pause(int source);
    void stop(int source);
    void rewind(int source);
    SourceState sourceState(int source);
    void setLooping(int source, boolean looping);
    boolean isLooping(int source);
    void setGain(int source, float gain);
    float gain(int source);
    void setPitch(int source, float pitch);
    float pitch(int source);
    void setSourceRelative(int source, boolean relative);
    boolean isSourceRelative(int source);
    void setSourcePosition(int source, float x, float y, float z);
    void setSourceVelocity(int source, float x, float y, float z);
    void sourcePosition(int source, float[] into);
    void sourceVelocity(int source, float[] into);
    void setDistanceModel(DistanceModel model);
    void setDopplerFactor(float factor);
    float dopplerFactor();
    void setSpeedOfSound(float speed);
    float speedOfSound();
    float dopplerShift(int source);
    DistanceModel distanceModel();
    void setSourceAttenuation(int source, float referenceDistance, float rolloffFactor, float maxDistance);
    float referenceDistance(int source);
    float rolloffFactor(int source);
    float maxDistance(int source);
    void setListenerGain(float gain);
    float listenerGain();
    void setListenerPosition(float x, float y, float z);
    void setListenerVelocity(float x, float y, float z);
    void listenerPosition(float[] into);
    void listenerVelocity(float[] into);
    void setListenerOrientation(float[] atThenUp);
    void listenerOrientation(float[] into);
    void shutdown();
```

### AudioException

An audio request the device could not do.

```java
class AudioException extends RuntimeException
    AudioException(String message);
    AudioException(String message, Throwable cause);
```

### AudioFormat

The PCM layouts OpenAL buffers hold.

```java
enum AudioFormat
    static final AudioFormat MONO_8;
    static final AudioFormat MONO_16;
    static final AudioFormat STEREO_8;
    static final AudioFormat STEREO_16;
    int channels();
    int bitsPerSample();
    int frameSizeInBytes();
```

### DistanceModel

How a source's gain falls with distance.

```java
enum DistanceModel
    static final DistanceModel NONE;
    static final DistanceModel INVERSE;
    static final DistanceModel INVERSE_CLAMPED;
    static final DistanceModel LINEAR;
    static final DistanceModel LINEAR_CLAMPED;
    static final DistanceModel EXPONENT;
    static final DistanceModel EXPONENT_CLAMPED;
```

### Doppler

The Doppler pitch shift from source and listener velocities. Web Audio has none.

```java
class Doppler
    static final float DEFAULT_FACTOR;
    static final float DEFAULT_SPEED_OF_SOUND;
    static float shift(float[] sourcePosition, float[] sourceVelocity, float[] listenerPosition, float[] listenerVelocity, float factor, float speedOfSound); // openal 1.1 measures both velocities along the line from the source to the listener and clamps each to the speed sound travels, so a source outrunning its own sound does not invert the shift.
```

### SilentAudioDevice

An `AudioDevice` that plays nothing and keeps time. For tests and hosts without audio.

```java
class SilentAudioDevice implements AudioDevice
    SilentAudioDevice(Clock clock);
```

### SourceState

The state of a source: initial, playing, paused or stopped.

```java
enum SourceState
    static final SourceState INITIAL;
    static final SourceState PLAYING;
    static final SourceState PAUSED;
    static final SourceState STOPPED;
```

## dev.flagon.platform.display

### DisplaySurface

A window with a graphics device and an input source.

```java
interface DisplaySurface
    int width();
    int height();
    void setTitle(String title);
    void setSize(int width, int height); // resizes the drawable area; hosts that cannot honor the request keep their own size.
    boolean isFullscreen();
    boolean setFullscreen(boolean fullscreen);
    boolean isFocused();
    boolean isVisible();
    boolean isCloseRequested();
    void requestClose();
    GraphicsDevice graphics();
    InputSource input();
    void present(); // displays the current frame and returns when the next one should be drawn.
    void destroy();
```

## dev.flagon.platform.fs

### FilePaths

Absolute `/`-separated paths, resolved without a store.

```java
class FilePaths
    static final String ROOT;
    static String normalize(String path);
    static String join(String path, String name);
    static String parent(String path);
    static String name(String path);
    static boolean isRoot(String path);
```

### FileStore

Files and directories by absolute path.

```java
interface FileStore
    String name();
    boolean exists(String path);
    boolean isDirectory(String path);
    boolean isFile(String path);
    String[] list(String path);
    int size(String path);
    long lastModified(String path);
    byte[] read(String path);
    void write(String path, byte[] data); // replaces the file's contents, creating it if it does not exist.
    boolean createFile(String path);
    boolean createDirectory(String path);
    boolean delete(String path);
    boolean move(String from, String to);
```

### FileStoreException

A file operation the store could not do.

```java
class FileStoreException extends RuntimeException
    FileStoreException(String message);
    FileStoreException(String message, Throwable cause);
```

### MemoryFileStore

A `FileStore` in the heap.

```java
class MemoryFileStore implements FileStore
    MemoryFileStore();
```

### StoreFileSystem

Puts `java.io.File` and the file streams on a `FileStore`.

```java
class StoreFileSystem implements VirtualFileSystem
    StoreFileSystem(FileStore store); // writes each close and flush to the store before returning
    static void install(FileStore store); // closes and flushes reach the store on a thread of their own
```

## dev.flagon.platform.graphics

### Argb

Converts `0xAARRGGBB` ints to RGBA bytes.

```java
class Argb
    static byte[] spread(int[] argb, int count);
```

### AttributeType

The component type of a vertex attribute.

```java
enum AttributeType
    static final AttributeType BYTE;
    static final AttributeType UNSIGNED_BYTE;
    static final AttributeType SHORT;
    static final AttributeType UNSIGNED_SHORT;
    static final AttributeType INT;
    static final AttributeType UNSIGNED_INT;
    static final AttributeType FLOAT;
    int sizeInBytes();
```

### BlendEquation

How blending combines source and destination.

```java
enum BlendEquation
    static final BlendEquation ADD;
    static final BlendEquation SUBTRACT;
    static final BlendEquation REVERSE_SUBTRACT;
    static final BlendEquation MIN;
    static final BlendEquation MAX;
```

### BlendFactor

A blend factor.

```java
enum BlendFactor
    static final BlendFactor ZERO;
    static final BlendFactor ONE;
    static final BlendFactor SRC_COLOR;
    static final BlendFactor ONE_MINUS_SRC_COLOR;
    static final BlendFactor DST_COLOR;
    static final BlendFactor ONE_MINUS_DST_COLOR;
    static final BlendFactor SRC_ALPHA;
    static final BlendFactor ONE_MINUS_SRC_ALPHA;
    static final BlendFactor DST_ALPHA;
    static final BlendFactor ONE_MINUS_DST_ALPHA;
    static final BlendFactor CONSTANT_COLOR;
    static final BlendFactor ONE_MINUS_CONSTANT_COLOR;
    static final BlendFactor CONSTANT_ALPHA;
    static final BlendFactor ONE_MINUS_CONSTANT_ALPHA;
    static final BlendFactor SRC_ALPHA_SATURATE;
```

### BufferTarget

What a buffer is bound as.

```java
enum BufferTarget
    static final BufferTarget ARRAY;
    static final BufferTarget ELEMENT;
```

### BufferUsage

How often a buffer's data changes.

```java
enum BufferUsage
    static final BufferUsage STATIC;
    static final BufferUsage DYNAMIC;
    static final BufferUsage STREAM;
```

### Canvas2D

A 2D canvas with the HTML canvas model: one current path, a transform, and state that `save` and `restore` stack. Colors are `0xAARRGGBB`, not premultiplied.

```java
interface Canvas2D
    int width();
    int height();
    void put(int[] argb, int offset, int stride, int x, int y, int width, int height);
    void get(int[] argb, int offset, int stride, int x, int y, int width, int height);
    void save();
    void restore();
    void setTransform(double m00, double m10, double m01, double m11, double m02, double m12);
    void beginPath();
    void moveTo(double x, double y);
    void lineTo(double x, double y);
    void quadTo(double cx, double cy, double x, double y);
    void curveTo(double cx1, double cy1, double cx2, double cy2, double x, double y);
    void ellipse(double cx, double cy, double rx, double ry, double start, double end, boolean counterclockwise); // radians, clockwise
    void closePath();
    void clip(boolean evenOdd);
    void fill(boolean evenOdd);
    void stroke();
    void clearRect(double x, double y, double width, double height);
    void color(int argb);
    void line(double width, String cap, String join, double miterLimit, double[] dash, double dashPhase);
    void alpha(double alpha);
    void composite(String operation); // a canvas globalCompositeOperation
    void smoothing(boolean smooth);
    void font(String font); // a CSS font
    void fillText(String text, double x, double y);
    double[] measure(String text); // width, ascent and descent
    void drawImage(Canvas2D source, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh);
```

### CompareFunc

A depth, stencil or texture comparison.

```java
enum CompareFunc
    static final CompareFunc NEVER;
    static final CompareFunc LESS;
    static final CompareFunc EQUAL;
    static final CompareFunc LESS_OR_EQUAL;
    static final CompareFunc GREATER;
    static final CompareFunc NOT_EQUAL;
    static final CompareFunc GREATER_OR_EQUAL;
    static final CompareFunc ALWAYS;
```

### CullFace

Which faces culling removes.

```java
enum CullFace
    static final CullFace FRONT;
    static final CullFace BACK;
    static final CullFace FRONT_AND_BACK;
```

### Drawing

The host's 2D raster: the canvases `java.awt` draws on, and the image files `javax.imageio` decodes. `decode` pauses only the calling green thread.

```java
interface Drawing
    boolean isAvailable();
    Canvas2D canvas(int width, int height); // a transparent canvas
    Decoded decode(byte[] file); // throws IOException when the host cannot read the file.
    static Drawing unavailable(String reason); // refuses every call with this reason.
```

### Drawing.Decoded

A decoded image: one `0xAARRGGBB` int per pixel, row by row, not premultiplied.

```java
record Drawing.Decoded(int width, int height, int[] argb)
```

### FramebufferAttachment

Where an image attaches to a framebuffer.

```java
enum FramebufferAttachment
    static final FramebufferAttachment COLOR;
    static final FramebufferAttachment DEPTH;
    static final FramebufferAttachment STENCIL;
    static final FramebufferAttachment DEPTH_STENCIL;
```

### FramebufferStatus

Whether a framebuffer is complete.

```java
enum FramebufferStatus
    static final FramebufferStatus COMPLETE;
    static final FramebufferStatus INCOMPLETE_ATTACHMENT;
    static final FramebufferStatus INCOMPLETE_MISSING_ATTACHMENT;
    static final FramebufferStatus INCOMPLETE_DIMENSIONS;
    static final FramebufferStatus UNSUPPORTED;
    static final FramebufferStatus UNKNOWN;
```

### GraphicsDevice

The rendering contract, close to OpenGL ES 3.0. Objects are integer names.

```java
interface GraphicsDevice
    int NO_BUFFER;
    int NO_VERTEX_ARRAY;
    int NO_TEXTURE;
    int NO_PROGRAM;
    int NO_FRAMEBUFFER;
    int NO_RENDERBUFFER;
    int NO_SHADER;
    int NO_UNIFORM;
    void viewport(int x, int y, int width, int height);
    void clearColor(float red, float green, float blue, float alpha);
    void clearDepth(double depth);
    void clear(boolean color, boolean depth, boolean stencil);
    void setDepthTest(boolean enabled);
    void setDepthWrite(boolean enabled);
    void setDepthFunc(CompareFunc func);
    void setBlend(boolean enabled);
    void setBlendFunc(BlendFactor source, BlendFactor destination);
    void setBlendFuncSeparate(BlendFactor sourceColor, BlendFactor destinationColor, BlendFactor sourceAlpha, BlendFactor destinationAlpha); // color and alpha blend
    void setBlendEquation(BlendEquation equation);
    void setBlendEquationSeparate(BlendEquation color, BlendEquation alpha);
    void setBlendColor(float red, float green, float blue, float alpha);
    void setCullFace(boolean enabled);
    void setCulledFaces(CullFace faces);
    void setFrontFace(Winding winding);
    void setScissorTest(boolean enabled);
    void setScissor(int x, int y, int width, int height);
    void setColorMask(boolean red, boolean green, boolean blue, boolean alpha);
    void setStencilTest(boolean enabled);
    void setStencilFunc(CompareFunc func, int reference, int mask);
    void setStencilOp(StencilOp stencilFail, StencilOp depthFail, StencilOp pass);
    void setStencilMask(int mask);
    void clearStencil(int value);
    void setPolygonOffsetFill(boolean enabled);
    void setPolygonOffset(float factor, float units);
    void setLineWidth(float width);
    int createBuffer();
    void deleteBuffer(int buffer);
    void bindBuffer(BufferTarget target, int buffer);
    void bufferData(BufferTarget target, int sizeInBytes, BufferUsage usage); // reserves ,sizeInBytes, of uninitialised storage, replacing any contents.
    void bufferData(BufferTarget target, byte[] data, int length, BufferUsage usage); // uploads ,length, bytes from ,data,, replacing the bound buffer's contents.
    void bufferData(BufferTarget target, float[] data, int length, BufferUsage usage); // uploads ,length, floats from ,data,, replacing the bound buffer's contents.
    void bufferData(BufferTarget target, int[] data, int length, BufferUsage usage); // uploads ,length, ints from ,data,, replacing the bound buffer's contents.
    void bufferSubData(BufferTarget target, int offsetInBytes, byte[] data, int length);
    void bufferSubData(BufferTarget target, int offsetInBytes, float[] data, int length);
    int createProgram(String vertexSource, String fragmentSource);
    int createShader(ShaderStage stage);
    void shaderSource(int shader, String source);
    void compileShader(int shader);
    boolean isShaderCompiled(int shader);
    String shaderInfoLog(int shader);
    void deleteShader(int shader);
    boolean isShader(int shader);
    int createProgram();
    void attachShader(int program, int shader);
    void detachShader(int program, int shader);
    void bindAttributeLocation(int program, int location, String name);
    void linkProgram(int program);
    boolean isProgramLinked(int program);
    void validateProgram(int program);
    boolean isProgramValid(int program);
    String programInfoLog(int program);
    boolean isProgram(int program);
    void deleteProgram(int program);
    void useProgram(int program);
    int uniformLocation(int program, String name);
    void uniformMatrix4(int location, float[] matrix);
    void uniform1i(int location, int value);
    void uniform1f(int location, float value);
    void uniform2f(int location, float x, float y);
    void uniform3f(int location, float x, float y, float z);
    void uniform4f(int location, float x, float y, float z, float w);
    void uniform2i(int location, int x, int y);
    void uniform3i(int location, int x, int y, int z);
    void uniform4i(int location, int x, int y, int z, int w);
    void uniformFloats(int location, int components, float[] values, int count); // sets ,count, vectors of ,components, floats each, read from ,values,.
    void uniformInts(int location, int components, int[] values, int count); // sets ,count, vectors of ,components, ints each, read from ,values,.
    void uniformMatrices(int location, int size, boolean transpose, float[] values, int count); // sets ,count, square matrices of ,size,x,size, floats, column-major.
    int attributeLocation(int program, String name);
    int maxAttributes();
    int createVertexArray();
    void bindVertexArray(int array);
    void deleteVertexArray(int array);
    void enableAttribute(int location);
    void disableAttribute(int location);
    void attributePointer(int location, int components, AttributeType type, boolean normalized, int strideInBytes, int offsetInBytes);
    void integerAttributePointer(int location, int components, AttributeType type, int strideInBytes, int offsetInBytes);
    void constantAttribute(int location, float x, float y, float z, float w);
    int createTexture();
    void deleteTexture(int texture);
    void activeTexture(int unit);
    void bindTexture(int unit, int texture);
    void textureImage(int level, int width, int height, byte[] rgba);
    void textureSubImage(int level, int x, int y, int width, int height, byte[] rgba);
    default void textureImageArgb(int level, int width, int height, int[] argb, int count); // the same upload from one 0xAARRGGBB int per pixel
    default void textureSubImageArgb(int level, int x, int y, int width, int height, int[] argb, int count);
    void generateMipmap();
    void setTextureBaseLevel(int level);
    void setTextureMaxLevel(int level);
    void setTextureMinLod(float lod);
    void setTextureMaxLod(float lod);
    void copyTextureSubImage(int x, int y, int framebufferX, int framebufferY, int width, int height);
    void setTextureMinFilter(TextureFilter filter);
    void setTextureMagFilter(TextureFilter filter);
    void setTextureWrapS(TextureWrap wrap);
    void setTextureWrapT(TextureWrap wrap);
    void drawArrays(Primitive primitive, int first, int count);
    void drawElements(Primitive primitive, int count, IndexType type, int offsetInBytes);
    void drawArraysInstanced(Primitive primitive, int first, int count, int instances);
    void drawElementsInstanced(Primitive primitive, int count, IndexType type, int offsetInBytes, int instances);
    void attributeDivisor(int location, int divisor);
    int createFramebuffer();
    void bindFramebuffer(int framebuffer);
    void deleteFramebuffer(int framebuffer);
    void framebufferTexture(FramebufferAttachment attachment, int texture, int level);
    void framebufferRenderbuffer(FramebufferAttachment attachment, int renderbuffer);
    FramebufferStatus framebufferStatus();
    int boundFramebuffer();
    int createRenderbuffer();
    void bindRenderbuffer(int renderbuffer);
    void deleteRenderbuffer(int renderbuffer);
    void renderbufferStorage(RenderbufferFormat format, int width, int height);
    void textureStorage(int level, int width, int height);
    byte[] readPixels(int x, int y, int width, int height);
    void finish();
    void flush();
    int width();
    int height();
    GraphicsError takeError();
    String information(GraphicsInfo information);
    int limit(GraphicsLimit limit);
```

### GraphicsError

A GL ES error.

```java
enum GraphicsError
    static final GraphicsError NONE;
    static final GraphicsError INVALID_ENUM;
    static final GraphicsError INVALID_VALUE;
    static final GraphicsError INVALID_OPERATION;
    static final GraphicsError INVALID_FRAMEBUFFER_OPERATION;
    static final GraphicsError OUT_OF_MEMORY;
    static final GraphicsError CONTEXT_LOST;
```

### GraphicsInfo

A string that describes the device.

```java
enum GraphicsInfo
    static final GraphicsInfo VENDOR;
    static final GraphicsInfo RENDERER;
    static final GraphicsInfo VERSION;
    static final GraphicsInfo SHADING_LANGUAGE_VERSION;
    static final GraphicsInfo EXTENSIONS;
```

### GraphicsLimit

A numeric limit of the device.

```java
enum GraphicsLimit
    static final GraphicsLimit MAX_TEXTURE_SIZE;
    static final GraphicsLimit MAX_TEXTURE_UNITS;
    static final GraphicsLimit MAX_VERTEX_ATTRIBUTES;
    static final GraphicsLimit MAX_VERTEX_UNIFORM_COMPONENTS;
    static final GraphicsLimit MAX_FRAGMENT_UNIFORM_COMPONENTS;
    static final GraphicsLimit MAX_VARYING_COMPONENTS;
    static final GraphicsLimit MAX_VERTEX_TEXTURE_UNITS;
    static final GraphicsLimit MAX_COMBINED_TEXTURE_UNITS;
    static final GraphicsLimit MAX_ARRAY_TEXTURE_LAYERS;
```

### IndexType

The width of one index.

```java
enum IndexType
    static final IndexType UNSIGNED_BYTE;
    static final IndexType UNSIGNED_SHORT;
    static final IndexType UNSIGNED_INT;
    int sizeInBytes();
```

### Primitive

What a draw call assembles.

```java
enum Primitive
    static final Primitive POINTS;
    static final Primitive LINES;
    static final Primitive LINE_STRIP;
    static final Primitive LINE_LOOP;
    static final Primitive TRIANGLES;
    static final Primitive TRIANGLE_STRIP;
    static final Primitive TRIANGLE_FAN;
```

### RenderbufferFormat

The storage of a renderbuffer.

```java
enum RenderbufferFormat
    static final RenderbufferFormat RGBA8;
    static final RenderbufferFormat DEPTH16;
    static final RenderbufferFormat DEPTH24;
    static final RenderbufferFormat DEPTH24_STENCIL8;
    static final RenderbufferFormat STENCIL8;
```

### ShaderException

A shader that failed to compile or a program that failed to link. The message holds the log.

```java
class ShaderException extends RuntimeException
    ShaderException(String message);
```

### ShaderStage

A programmable stage.

```java
enum ShaderStage
    static final ShaderStage VERTEX;
    static final ShaderStage FRAGMENT;
```

### StencilOp

What the stencil test does to the stored value.

```java
enum StencilOp
    static final StencilOp KEEP;
    static final StencilOp ZERO;
    static final StencilOp REPLACE;
    static final StencilOp INCREMENT;
    static final StencilOp INCREMENT_WRAP;
    static final StencilOp DECREMENT;
    static final StencilOp DECREMENT_WRAP;
    static final StencilOp INVERT;
```

### TextureFilter

How a texture samples between texels and mipmap levels.

```java
enum TextureFilter
    static final TextureFilter NEAREST;
    static final TextureFilter LINEAR;
    static final TextureFilter NEAREST_MIPMAP_NEAREST;
    static final TextureFilter LINEAR_MIPMAP_NEAREST;
    static final TextureFilter NEAREST_MIPMAP_LINEAR;
    static final TextureFilter LINEAR_MIPMAP_LINEAR;
```

### TextureWrap

What a coordinate outside 0 to 1 samples.

```java
enum TextureWrap
    static final TextureWrap REPEAT;
    static final TextureWrap CLAMP_TO_EDGE;
    static final TextureWrap MIRRORED_REPEAT;
```

### Winding

The vertex order of a front face.

```java
enum Winding
    static final Winding CLOCKWISE;
    static final Winding COUNTER_CLOCKWISE;
```

## dev.flagon.platform.input

### InputSource

Keyboard and mouse state and event queues.

```java
interface InputSource
    KeyEvent nextKeyEvent();
    boolean isKeyDown(int scancode);
    void setRepeatEvents(boolean enabled);
    boolean isRepeatEventsEnabled();
    MouseEvent nextMouseEvent();
    boolean isButtonDown(int button);
    int mouseX();
    int mouseY();
    int takeDeltaX();
    int takeDeltaY();
    int takeWheel();
    boolean isPointerInside();
    boolean isGrabbed();
    boolean setGrabbed(boolean grabbed);
    void reset();
```

### KeyEvent

A key press or release.

```java
class KeyEvent
    static final char NO_CHARACTER;
    KeyEvent(int key, char character, boolean pressed, boolean repeat, long nanos);
    int key();
    char character();
    boolean isPressed();
    boolean isRepeat();
    long nanos();
```

### MouseEvent

A mouse button, move or wheel event.

```java
class MouseEvent
    static final int NO_BUTTON;
    MouseEvent(int button, boolean pressed, int x, int y, int deltaX, int deltaY, int wheel, long nanos);
    int button();
    boolean isPressed();
    int x();
    int y();
    int deltaX();
    int deltaY();
    int wheel();
    long nanos();
```

### Scancode

Key numbers, by physical position. Hosts report them and callers compare against them.

```java
class Scancode
    static final int NONE;
    static final int COUNT;
    static final int ESCAPE;
    static final int NUMBER_1;
    static final int NUMBER_2;
    static final int NUMBER_3;
    static final int NUMBER_4;
    static final int NUMBER_5;
    static final int NUMBER_6;
    static final int NUMBER_7;
    static final int NUMBER_8;
    static final int NUMBER_9;
    static final int NUMBER_0;
    static final int MINUS;
    static final int EQUALS;
    static final int BACKSPACE;
    static final int TAB;
    static final int Q;
    static final int W;
    static final int E;
    static final int R;
    static final int T;
    static final int Y;
    static final int U;
    static final int I;
    static final int O;
    static final int P;
    static final int LEFT_BRACKET;
    static final int RIGHT_BRACKET;
    static final int RETURN;
    static final int LEFT_CONTROL;
    static final int A;
    static final int S;
    static final int D;
    static final int F;
    static final int G;
    static final int H;
    static final int J;
    static final int K;
    static final int L;
    static final int SEMICOLON;
    static final int APOSTROPHE;
    static final int GRAVE;
    static final int LEFT_SHIFT;
    static final int BACKSLASH;
    static final int Z;
    static final int X;
    static final int C;
    static final int V;
    static final int B;
    static final int N;
    static final int M;
    static final int COMMA;
    static final int PERIOD;
    static final int SLASH;
    static final int RIGHT_SHIFT;
    static final int NUMPAD_MULTIPLY;
    static final int LEFT_ALT;
    static final int SPACE;
    static final int CAPS_LOCK;
    static final int F1;
    static final int F2;
    static final int F3;
    static final int F4;
    static final int F5;
    static final int F6;
    static final int F7;
    static final int F8;
    static final int F9;
    static final int F10;
    static final int NUM_LOCK;
    static final int SCROLL_LOCK;
    static final int NUMPAD_7;
    static final int NUMPAD_8;
    static final int NUMPAD_9;
    static final int NUMPAD_SUBTRACT;
    static final int NUMPAD_4;
    static final int NUMPAD_5;
    static final int NUMPAD_6;
    static final int NUMPAD_ADD;
    static final int NUMPAD_1;
    static final int NUMPAD_2;
    static final int NUMPAD_3;
    static final int NUMPAD_0;
    static final int NUMPAD_DECIMAL;
    static final int F11;
    static final int F12;
    static final int F13;
    static final int F14;
    static final int F15;
    static final int NUMPAD_EQUALS;
    static final int NUMPAD_ENTER;
    static final int RIGHT_CONTROL;
    static final int NUMPAD_COMMA;
    static final int NUMPAD_DIVIDE;
    static final int PRINT_SCREEN;
    static final int RIGHT_ALT;
    static final int PAUSE;
    static final int HOME;
    static final int UP;
    static final int PAGE_UP;
    static final int LEFT;
    static final int RIGHT;
    static final int END;
    static final int DOWN;
    static final int PAGE_DOWN;
    static final int INSERT;
    static final int DELETE;
    static final int LEFT_META;
    static final int RIGHT_META;
    static final int APPS;
```

## dev.flagon.platform.net

### Connection

A two-way byte stream to one address.

```java
interface Connection
    String host();
    int port();
    boolean isOpen();
    int available();
    int read(byte[] into, int offset, int length); // reads at least one byte, blocking until there is one.
    void write(byte[] data, int offset, int length);
    void close(); // closes the connection; closing one already closed does nothing.
```

### Fetch

Sends HTTP requests for `java.net.http`. `send` pauses only the calling green thread.

```java
interface Fetch
    String name();
    boolean isAvailable();
    Response send(Request request); // throws TimedOut, or NetworkException when it fails.
    static Fetch unavailable(String reason); // refuses every request with this reason.
```

### Fetch.Request

One request. `timeoutMillis` 0 waits without limit. With `followRedirects` false, a redirect fails.

```java
record Fetch.Request(String method, String url, List<Map.Entry<String, String>> headers,
                     byte[] body, long timeoutMillis, boolean followRedirects)
```

### Fetch.Response

The whole answer. `url` is where it came from after redirects. Header names are lowercase.

```java
record Fetch.Response(int status, String url, Map<String, List<String>> headers, byte[] body)
```

### Fetch.TimedOut

No answer arrived within the request's timeout.

```java
class Fetch.TimedOut extends NetworkException
    TimedOut(String message);
```

### MemoryNetwork

A `Network` whose servers are objects in the heap. For tests.

```java
class MemoryNetwork implements Network
    void serve(String host, int port, Service service);
    List<String> secured(); // what the tls handshake was asked for, in the order it was asked
    void refuse(String host, int port);
```

### MemoryNetwork.Service

A server: one request in, one response out.

```java
interface MemoryNetwork.Service
    byte[] answer(byte[] request);
```

### Network

Opens connections.

```java
interface Network
    String name();
    boolean isAvailable();
    Connection connect(String host, int port);
    default Connection connectSecure(String host, int port);
```

### NetworkException

A connection that failed to open or failed while open.

```java
class NetworkException extends RuntimeException
    NetworkException(String message);
    NetworkException(String message, Throwable cause);
```

### UnavailableNetwork

The `Network` of a host without one. Every connect throws.

```java
class UnavailableNetwork implements Network
    UnavailableNetwork(String reason);
```

## dev.flagon.platform.workers

Bundle workers: the running bundle started again in a Web Worker, which runs one catalog program and talks to its parent by messages.

```java
Channel echo = Platforms.get().workers().spawn("worker-echo");
echo.send(new Message("frame", pixels));
Message answer = echo.receive();
echo.close();
```

### Channel

One end of the channel between a bundle and a worker. Messages keep their order. `receive` pauses only the calling green thread.

```java
interface Channel extends AutoCloseable
    void send(Message message);
    default void send(String text);
    default void send(byte[] data);
    Message receive(); // null once the other end has closed and every message is read.
    Message poll(); // null when none has arrived yet.
    boolean isOpen(); // false once the other end has closed or the worker has exited.
    void close(); // closing a spawned worker's channel stops the worker.
```

### Message

Text, bytes or both. The bytes cross to the other thread as a copy in a transferred `ArrayBuffer`.

```java
record Message(String text, byte[] data)
```

### Workers

Starts workers and finds the parent. A worker has no display and no audio output. Its console lines reach the parent's console, prefixed with the program id.

```java
interface Workers
    boolean isAvailable();
    Channel spawn(String program, String... arguments); // runs this catalog program in a new worker.
    Channel parent(); // the spawning bundle's channel; null outside a worker.
    static Workers unavailable(String reason); // refuses every spawn with this reason.
```

## dev.flagon.platform.wasi

### WasiPlatform

The headless `Platform` of a [WASI component](packaging.md#wasi): stdio, clocks, arguments and files. `create` takes the arguments `main` received. `java.io` works below the first preopened directory, as `/`, or in memory when there is none.

```java
class WasiPlatform implements Platform
    static WasiPlatform create(String[] arguments);
```

## dev.flagon.platform.browser

### BrowserClock

`Date.now()` and `performance.now()`.

```java
class BrowserClock implements Clock
```

### BrowserConsole

Writes log lines to the host bridge, or to `console`.

```java
class BrowserConsole implements ConsoleSink
```

### BrowserPlatform

The browser `Platform`. `create` takes the arguments `main` received.

```java
class BrowserPlatform implements Platform
    static BrowserPlatform create(String[] arguments);
    Fetch fetch(); // a BrowserFetch.
```

### BrowserRuntimeHost

The `RuntimeHost` over `window.__flagonHost`.

```java
class BrowserRuntimeHost implements RuntimeHost
```

### HostBridge

The JavaScript `window.__flagonHost` object. See [Host bridge](architecture.md#host-bridge).

```java
interface HostBridge extends JSObject
    void log(String level, String text);
    void catalog(JSArray<JSString> ids, JSArray<JSString> descriptions);
    void status(String phase, String detail);
    void ready();
    void exited(int code);
    void fatal(String phase, String message, String stack);
```

## dev.flagon.platform.browser.audio

### WebAudioDevice

The `AudioDevice` on Web Audio. Each playing source has its own node graph.

```java
class WebAudioDevice implements AudioDevice
    static WebAudioDevice createIfAvailable(ConsoleSink console);
```

## dev.flagon.platform.browser.display

### CanvasSurface

The `DisplaySurface` on the page's canvas.

```java
class CanvasSurface implements DisplaySurface
    static final String CANVAS_ID;
    static CanvasSurface open(int width, int height, String title, Clock clock);
    boolean isDestroyed();
```

### Frames

The frame loop. `awaitNextFrame` suspends the thread until the next `requestAnimationFrame`.

```java
class Frames
    static void awaitNextFrame();
```

## dev.flagon.platform.browser.fs

### OpfsFileStore

The `FileStore` on the Origin Private File System.

```java
class OpfsFileStore implements FileStore
    static OpfsFileStore createIfAvailable();
```

## dev.flagon.platform.browser.graphics

### BrowserDrawing

The `Drawing` over `OffscreenCanvas`. It decodes with `createImageBitmap`, through `self.__flagonImages`.

```java
class BrowserDrawing implements Drawing
```

### WebGL2

The JSO binding of `WebGL2RenderingContext`. Its methods mirror the [WebGL 2 API](https://developer.mozilla.org/docs/Web/API/WebGL2RenderingContext).

```java
interface WebGL2 extends JSObject
```

### WebGLGraphicsDevice

The `GraphicsDevice` on WebGL 2.

```java
class WebGLGraphicsDevice implements GraphicsDevice
    WebGLGraphicsDevice(WebGL2 gl);
    void resizeScreen(); // sizes the back buffer to the canvas; call after the canvas changes size
    void presentScreen(); // copies the finished frame to the canvas
```

## dev.flagon.platform.browser.js

### JsArrays

Converts between Java arrays and JavaScript typed arrays.

```java
class JsArrays
    static byte[] bytesOf(Int8Array values);
    static Int8Array bytes(byte[] values, int length);
    static Float32Array floats(float[] values, int length);
    static Uint8Array unsignedBytes(byte[] values, int length);
    static Int32Array ints(int[] values, int length);
    static Uint32Array unsignedInts(int[] values, int length);
    static Uint8Array rgba(int[] argb, int count, Uint32Array into);
```

## dev.flagon.platform.browser.net

### BrowserFetch

The `Fetch` over the page's `fetch`, through `window.__flagonFetch`. The page's CORS rules apply.

```java
class BrowserFetch implements Fetch
```

### ProxyNetwork

The `Network` over WebSockets to the socket proxy.

```java
class ProxyNetwork implements Network
    static ProxyNetwork createIfAvailable();
```

## dev.flagon.platform.browser.workers

### BrowserWorkers

The `Workers` over Web Workers, through `self.__flagonWorkers`. A worker loads the same bundle URL as the page.

```java
class BrowserWorkers implements Workers
```
