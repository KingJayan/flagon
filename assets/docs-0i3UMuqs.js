import"./modulepreload-polyfill-B5Qt9EMX.js";const we=`# API reference

Every public type in \`runtime\`, \`platform/api\`, \`platform/browser\`, \`platform/wasi\`, \`platform/web\` and \`@Reflectable\`. The [stable API](packaging.md#stable-api) is \`FlagonRuntime\`, the program types, \`BrowserPlatform.create\` and \`@Reflectable\`. Everything else may change between releases.

\`platform/api\` holds the host contracts and the targets of the build's JDK reroutes. Application code does not call the reroute targets: the build sends JDK calls to them.

## dev.flagon.runtime

### ConsoleStreams

Routes \`System.out\` and \`System.err\` into the platform console, one line at a time.

\`\`\`java
class ConsoleStreams
    static void install(ConsoleSink console);
    static void restore();
\`\`\`

### FlagonRuntime

Boots a platform and runs one program. \`boot\` installs the platform, routes \`System.out\` and URLs, picks the program from the first argument, runs it, and reports each phase to the host. It returns 0, or -1 when a phase throws.

\`\`\`java
class FlagonRuntime
    static final String PHASE_PLATFORM;
    static final String PHASE_SELECT;
    static final String PHASE_PROGRAM;
    static int boot(Platform platform, ProgramRegistry registry);
\`\`\`

### MainProgram

A \`Program\` that calls a \`main(String[])\` method reference.

\`\`\`java
class MainProgram implements Program
    MainProgram(String id, String description, Main main);
\`\`\`

### MainProgram.Main

A \`main(String[])\` method. Use a method reference, so the compiler keeps the call graph.

\`\`\`java
interface MainProgram.Main
    void run(String[] arguments);
\`\`\`

### Program

One runnable program in a bundle.

\`\`\`java
interface Program
    String id();
    String description();
    void run(String[] arguments);
\`\`\`

### ProgramCatalog

Writes the registry as JSON for the page. The build runs it on the JVM.

\`\`\`java
class ProgramCatalog
    static String json(ProgramRegistry registry);
    static void writeTo(ProgramRegistry registry, String path);
\`\`\`

### ProgramRegistry

The programs in a bundle, in registration order. The first is the default.

\`\`\`java
class ProgramRegistry
    ProgramRegistry register(Program program);
    Program find(String id);
    List<Program> all();
    String ids();
    boolean isEmpty();
\`\`\`

## dev.flagon.runtime.net

### NetworkSocket

The \`java.net.Socket\` implementation over the platform \`Network\`.

\`\`\`java
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
\`\`\`

### NetworkUrls

Installs the \`URLStreamHandlerFactory\` that opens \`http\` and \`https\` URLs over the platform \`Network\`.

\`\`\`java
class NetworkUrls
    static void install(Network network, ConsoleSink console);
\`\`\`

## dev.flagon.reflect

### Reflectable

Keeps the class's fields, methods, constructors and reflection metadata.

\`\`\`java
@interface Reflectable
\`\`\`

## dev.flagon.platform

### Clock

Wall-clock and monotonic time.

\`\`\`java
interface Clock
    long currentTimeMillis();
    long nanoTime();
\`\`\`

### ConsoleSink

Where log lines go.

\`\`\`java
interface ConsoleSink
    void write(Level level, String message, Throwable error);
    default void debug(String message);
    default void info(String message);
    default void warn(String message);
    default void error(String message, Throwable error);
\`\`\`

### ConsoleSink.Level

A log level.

\`\`\`java
enum ConsoleSink.Level
    static final Level DEBUG;
    static final Level INFO;
    static final Level WARN;
    static final Level ERROR;
\`\`\`

### Names

Objects under the small integer names GL and AL hand out. A freed name is used again before a new one.

\`\`\`java
class Names<T> implements Iterable<T>
    Names();
    int add(T object);
    void put(int name, T object); // puts an object under a name the caller chose, freeing any gap before it
    T get(int name);
    boolean has(int name);
    T remove(int name);
    int size(); // one past the highest name in use
    void clear();
\`\`\`

### Platform

The services a host gives the runtime. \`display\` opens the one display surface.

\`\`\`java
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
\`\`\`

### Platforms

Holds the installed \`Platform\` for static LWJGL code.

\`\`\`java
class Platforms
    static void install(Platform platform);
    static Platform get();
    static boolean isInstalled();
    static void reset();
\`\`\`

### RuntimeHost

The page that runs the bundle. The runtime reports its phases here.

\`\`\`java
interface RuntimeHost
    String[] arguments();
    void catalog(String[] ids, String[] descriptions);
    void status(String phase, String detail);
    void ready();
    void exited(int code);
    void fatal(String phase, Throwable error); // terminal: no further callbacks follow.
\`\`\`

### UnsupportedCapabilityException

Thrown when the host has no way to do what was asked.

\`\`\`java
class UnsupportedCapabilityException extends RuntimeException
    UnsupportedCapabilityException(String message);
\`\`\`

## dev.flagon.platform reroute targets

The build rewrites JDK calls to these methods. A call on an instance passes the instance as the first argument.

### Arrays

\`java.lang.reflect.Array\` calls TeaVM lacks. The build reroutes them here.

\`\`\`java
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
\`\`\`

### Charsets

\`Charset.isSupported\` without the JDK provider lookup.

\`\`\`java
class Charsets
    static boolean isSupported(String name);
\`\`\`

### ClassLoaders

\`ClassLoader\` class and resource lookups, rerouted by the build.

\`\`\`java
class ClassLoaders
    static Class<?> loadClass(ClassLoader loader, String name);
    static InputStream getResourceAsStream(ClassLoader loader, String name);
    static Enumeration<URL> getResources(ClassLoader loader, String name);
    static URL getResource(ClassLoader loader, String name);
    static URL getSystemResource(String name);
\`\`\`

### Classes

\`Class\` reflection calls TeaVM lacks or answers wrongly, rerouted by the build.

\`\`\`java
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
\`\`\`

### Fields

Typed \`Field\` reads and writes, rerouted by the build. They widen as the JDK does.

\`\`\`java
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
\`\`\`

### Files

\`getChannel\` for file streams.

\`\`\`java
class Files
    static FileChannel getChannel(FileInputStream stream);
    static FileChannel getChannel(FileOutputStream stream);
\`\`\`

### KeptClasses

The classes the build kept, for class path scans.

\`\`\`java
class KeptClasses
\`\`\`

### Maps

\`Collections\` map wrappers TeaVM lacks.

\`\`\`java
class Maps
    static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, V> map);
\`\`\`

### Methods

\`Method\` calls TeaVM lacks, rerouted by the build.

\`\`\`java
class Methods
    static boolean isDefault(Method method); // teavm reports every interface method as abstract, so the build notes which have a body
    static Object getDefaultValue(Method method); // the default of an annotation member, as the build wrote it, or null
    static <A extends Annotation> A[] getAnnotationsByType(Method method, Class<A> asked);
    static <A extends Annotation> A[] getDeclaredAnnotationsByType(Method method, Class<A> asked);
\`\`\`

### Packages

\`Package\` manifest attributes. Each returns \`null\`: a bundle has no manifest.

\`\`\`java
class Packages
    static String getSpecificationTitle(Package of);
    static String getSpecificationVersion(Package of);
    static String getSpecificationVendor(Package of);
    static String getImplementationTitle(Package of);
    static String getImplementationVersion(Package of);
    static String getImplementationVendor(Package of);
\`\`\`

### ReflectionWatch

Wraps member listings. When a class was not kept for reflection, it logs how to keep it.

\`\`\`java
class ReflectionWatch
    static Method[] declaredMethods(Class<?> type);
    static Method[] methods(Class<?> type);
    static Field[] declaredFields(Class<?> type);
    static Field[] fields(Class<?> type);
    static Constructor<?>[] declaredConstructors(Class<?> type);
    static Constructor<?>[] constructors(Class<?> type);
\`\`\`

### ResourceUrls

\`flagon:\` URLs for files in the resource pack.

\`\`\`java
class ResourceUrls
    static final String SCHEME;
    static URL of(String name);
\`\`\`

### Resources

The resource pack and the files kept beside it.

\`\`\`java
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
\`\`\`

### Resources.Source

The pack's bytes, read where they arrived.

\`\`\`java
interface Resources.Source
    int length();
    byte get(int at);
    void copy(int from, byte[] into, int to, int count);
\`\`\`

### Resources.Fetcher

Starts the download of a file kept beside the pack, by its SHA-256 name. It must not wait.

\`\`\`java
interface Resources.Fetcher
    byte[] fetch(String hash);
\`\`\`

### Runtimes

\`Runtime.maxMemory\` and \`Runtime.exec\`, rerouted by the build.

\`\`\`java
class Runtimes
    static long maxMemory(Runtime runtime);
    static Process exec(Runtime runtime, String command);
    static Process exec(Runtime runtime, String[] command);
\`\`\`

### ShutdownHooks

\`addShutdownHook\`, \`removeShutdownHook\`, \`exit\` and \`halt\`, rerouted by the build.

\`\`\`java
class ShutdownHooks
    static void add(Runtime runtime, Thread hook);
    static boolean remove(Runtime runtime, Thread hook);
    static void exit(Runtime runtime, int code);
    static void halt(Runtime runtime, int code);
    static void runAll();
    static int pending();
    static void reset();
\`\`\`

### Systems

\`System.exit\`, \`loadLibrary\` and \`load\`, rerouted by the build.

\`\`\`java
class Systems
    static void exit(int status);
    static void loadLibrary(String name);
    static void load(String path);
    static String mapLibraryName(String name);
\`\`\`

### Urls

\`URL.openConnection(Proxy)\`, rerouted by the build.

\`\`\`java
class Urls
    static URLConnection openConnection(URL url, Proxy proxy);
\`\`\`

### Uuids

\`UUID\` constructors and accessors TeaVM lacks.

\`\`\`java
class Uuids
    static UUID of(long high, long low);
    static UUID fromString(String text); // the jdk's reading: five dashed hex fields, each masked to its width
    static long getMostSignificantBits(UUID id);
    static long getLeastSignificantBits(UUID id);
    static UUID nameUUIDFromBytes(byte[] name);
\`\`\`

## dev.flagon.platform.audio

### Audio

PCM frame arithmetic for \`AudioDevice\` implementations.

\`\`\`java
class Audio
    static void requireWholeFrames(AudioFormat format, int length, int sampleRate);
    static void requireWholeFrames(AudioFormat format, int length);
    static int frames(AudioFormat format, int lengthInBytes);
    static double durationInSeconds(AudioFormat format, int lengthInBytes, int sampleRate);
    static float[] channelSamples(AudioFormat format, byte[] data, int length, int channel);
\`\`\`

### AudioDevice

A mixer: sources play PCM buffers at positions in space. Buffers and sources are integer names.

\`\`\`java
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
\`\`\`

### AudioException

An audio request the device could not do.

\`\`\`java
class AudioException extends RuntimeException
    AudioException(String message);
    AudioException(String message, Throwable cause);
\`\`\`

### AudioFormat

The PCM layouts OpenAL buffers hold.

\`\`\`java
enum AudioFormat
    static final AudioFormat MONO_8;
    static final AudioFormat MONO_16;
    static final AudioFormat STEREO_8;
    static final AudioFormat STEREO_16;
    int channels();
    int bitsPerSample();
    int frameSizeInBytes();
\`\`\`

### DistanceModel

How a source's gain falls with distance.

\`\`\`java
enum DistanceModel
    static final DistanceModel NONE;
    static final DistanceModel INVERSE;
    static final DistanceModel INVERSE_CLAMPED;
    static final DistanceModel LINEAR;
    static final DistanceModel LINEAR_CLAMPED;
    static final DistanceModel EXPONENT;
    static final DistanceModel EXPONENT_CLAMPED;
\`\`\`

### Doppler

The Doppler pitch shift from source and listener velocities. Web Audio has none.

\`\`\`java
class Doppler
    static final float DEFAULT_FACTOR;
    static final float DEFAULT_SPEED_OF_SOUND;
    static float shift(float[] sourcePosition, float[] sourceVelocity, float[] listenerPosition, float[] listenerVelocity, float factor, float speedOfSound); // openal 1.1 measures both velocities along the line from the source to the listener and clamps each to the speed sound travels, so a source outrunning its own sound does not invert the shift.
\`\`\`

### SilentAudioDevice

An \`AudioDevice\` that plays nothing and keeps time. For tests and hosts without audio.

\`\`\`java
class SilentAudioDevice implements AudioDevice
    SilentAudioDevice(Clock clock);
\`\`\`

### SourceState

The state of a source: initial, playing, paused or stopped.

\`\`\`java
enum SourceState
    static final SourceState INITIAL;
    static final SourceState PLAYING;
    static final SourceState PAUSED;
    static final SourceState STOPPED;
\`\`\`

## dev.flagon.platform.display

### DisplaySurface

A window with a graphics device and an input source.

\`\`\`java
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
\`\`\`

## dev.flagon.platform.fs

### FilePaths

Absolute \`/\`-separated paths, resolved without a store.

\`\`\`java
class FilePaths
    static final String ROOT;
    static String normalize(String path);
    static String join(String path, String name);
    static String parent(String path);
    static String name(String path);
    static boolean isRoot(String path);
\`\`\`

### FileStore

Files and directories by absolute path.

\`\`\`java
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
\`\`\`

### FileStoreException

A file operation the store could not do.

\`\`\`java
class FileStoreException extends RuntimeException
    FileStoreException(String message);
    FileStoreException(String message, Throwable cause);
\`\`\`

### MemoryFileStore

A \`FileStore\` in the heap.

\`\`\`java
class MemoryFileStore implements FileStore
    MemoryFileStore();
\`\`\`

### StoreFileSystem

Puts \`java.io.File\` and the file streams on a \`FileStore\`.

\`\`\`java
class StoreFileSystem implements VirtualFileSystem
    StoreFileSystem(FileStore store); // writes each close and flush to the store before returning
    static void install(FileStore store); // closes and flushes reach the store on a thread of their own
\`\`\`

## dev.flagon.platform.graphics

### Argb

Converts \`0xAARRGGBB\` ints to RGBA bytes.

\`\`\`java
class Argb
    static byte[] spread(int[] argb, int count);
\`\`\`

### AttributeType

The component type of a vertex attribute.

\`\`\`java
enum AttributeType
    static final AttributeType BYTE;
    static final AttributeType UNSIGNED_BYTE;
    static final AttributeType SHORT;
    static final AttributeType UNSIGNED_SHORT;
    static final AttributeType INT;
    static final AttributeType UNSIGNED_INT;
    static final AttributeType FLOAT;
    int sizeInBytes();
\`\`\`

### BlendEquation

How blending combines source and destination.

\`\`\`java
enum BlendEquation
    static final BlendEquation ADD;
    static final BlendEquation SUBTRACT;
    static final BlendEquation REVERSE_SUBTRACT;
    static final BlendEquation MIN;
    static final BlendEquation MAX;
\`\`\`

### BlendFactor

A blend factor.

\`\`\`java
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
\`\`\`

### BufferTarget

What a buffer is bound as.

\`\`\`java
enum BufferTarget
    static final BufferTarget ARRAY;
    static final BufferTarget ELEMENT;
\`\`\`

### BufferUsage

How often a buffer's data changes.

\`\`\`java
enum BufferUsage
    static final BufferUsage STATIC;
    static final BufferUsage DYNAMIC;
    static final BufferUsage STREAM;
\`\`\`

### Canvas2D

A 2D canvas with the HTML canvas model: one current path, a transform, and state that \`save\` and \`restore\` stack. Colors are \`0xAARRGGBB\`, not premultiplied.

\`\`\`java
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
\`\`\`

### CompareFunc

A depth, stencil or texture comparison.

\`\`\`java
enum CompareFunc
    static final CompareFunc NEVER;
    static final CompareFunc LESS;
    static final CompareFunc EQUAL;
    static final CompareFunc LESS_OR_EQUAL;
    static final CompareFunc GREATER;
    static final CompareFunc NOT_EQUAL;
    static final CompareFunc GREATER_OR_EQUAL;
    static final CompareFunc ALWAYS;
\`\`\`

### CullFace

Which faces culling removes.

\`\`\`java
enum CullFace
    static final CullFace FRONT;
    static final CullFace BACK;
    static final CullFace FRONT_AND_BACK;
\`\`\`

### Drawing

The host's 2D raster: the canvases \`java.awt\` draws on, and the image files \`javax.imageio\` decodes. \`decode\` pauses only the calling green thread.

\`\`\`java
interface Drawing
    boolean isAvailable();
    Canvas2D canvas(int width, int height); // a transparent canvas
    Decoded decode(byte[] file); // throws IOException when the host cannot read the file.
    static Drawing unavailable(String reason); // refuses every call with this reason.
\`\`\`

### Drawing.Decoded

A decoded image: one \`0xAARRGGBB\` int per pixel, row by row, not premultiplied.

\`\`\`java
record Drawing.Decoded(int width, int height, int[] argb)
\`\`\`

### FramebufferAttachment

Where an image attaches to a framebuffer.

\`\`\`java
enum FramebufferAttachment
    static final FramebufferAttachment COLOR;
    static final FramebufferAttachment DEPTH;
    static final FramebufferAttachment STENCIL;
    static final FramebufferAttachment DEPTH_STENCIL;
\`\`\`

### FramebufferStatus

Whether a framebuffer is complete.

\`\`\`java
enum FramebufferStatus
    static final FramebufferStatus COMPLETE;
    static final FramebufferStatus INCOMPLETE_ATTACHMENT;
    static final FramebufferStatus INCOMPLETE_MISSING_ATTACHMENT;
    static final FramebufferStatus INCOMPLETE_DIMENSIONS;
    static final FramebufferStatus UNSUPPORTED;
    static final FramebufferStatus UNKNOWN;
\`\`\`

### GraphicsDevice

The rendering contract, close to OpenGL ES 3.0. Objects are integer names.

\`\`\`java
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
\`\`\`

### GraphicsError

A GL ES error.

\`\`\`java
enum GraphicsError
    static final GraphicsError NONE;
    static final GraphicsError INVALID_ENUM;
    static final GraphicsError INVALID_VALUE;
    static final GraphicsError INVALID_OPERATION;
    static final GraphicsError INVALID_FRAMEBUFFER_OPERATION;
    static final GraphicsError OUT_OF_MEMORY;
    static final GraphicsError CONTEXT_LOST;
\`\`\`

### GraphicsInfo

A string that describes the device.

\`\`\`java
enum GraphicsInfo
    static final GraphicsInfo VENDOR;
    static final GraphicsInfo RENDERER;
    static final GraphicsInfo VERSION;
    static final GraphicsInfo SHADING_LANGUAGE_VERSION;
    static final GraphicsInfo EXTENSIONS;
\`\`\`

### GraphicsLimit

A numeric limit of the device.

\`\`\`java
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
\`\`\`

### IndexType

The width of one index.

\`\`\`java
enum IndexType
    static final IndexType UNSIGNED_BYTE;
    static final IndexType UNSIGNED_SHORT;
    static final IndexType UNSIGNED_INT;
    int sizeInBytes();
\`\`\`

### Primitive

What a draw call assembles.

\`\`\`java
enum Primitive
    static final Primitive POINTS;
    static final Primitive LINES;
    static final Primitive LINE_STRIP;
    static final Primitive LINE_LOOP;
    static final Primitive TRIANGLES;
    static final Primitive TRIANGLE_STRIP;
    static final Primitive TRIANGLE_FAN;
\`\`\`

### RenderbufferFormat

The storage of a renderbuffer.

\`\`\`java
enum RenderbufferFormat
    static final RenderbufferFormat RGBA8;
    static final RenderbufferFormat DEPTH16;
    static final RenderbufferFormat DEPTH24;
    static final RenderbufferFormat DEPTH24_STENCIL8;
    static final RenderbufferFormat STENCIL8;
\`\`\`

### ShaderException

A shader that failed to compile or a program that failed to link. The message holds the log.

\`\`\`java
class ShaderException extends RuntimeException
    ShaderException(String message);
\`\`\`

### ShaderStage

A programmable stage.

\`\`\`java
enum ShaderStage
    static final ShaderStage VERTEX;
    static final ShaderStage FRAGMENT;
\`\`\`

### StencilOp

What the stencil test does to the stored value.

\`\`\`java
enum StencilOp
    static final StencilOp KEEP;
    static final StencilOp ZERO;
    static final StencilOp REPLACE;
    static final StencilOp INCREMENT;
    static final StencilOp INCREMENT_WRAP;
    static final StencilOp DECREMENT;
    static final StencilOp DECREMENT_WRAP;
    static final StencilOp INVERT;
\`\`\`

### TextureFilter

How a texture samples between texels and mipmap levels.

\`\`\`java
enum TextureFilter
    static final TextureFilter NEAREST;
    static final TextureFilter LINEAR;
    static final TextureFilter NEAREST_MIPMAP_NEAREST;
    static final TextureFilter LINEAR_MIPMAP_NEAREST;
    static final TextureFilter NEAREST_MIPMAP_LINEAR;
    static final TextureFilter LINEAR_MIPMAP_LINEAR;
\`\`\`

### TextureWrap

What a coordinate outside 0 to 1 samples.

\`\`\`java
enum TextureWrap
    static final TextureWrap REPEAT;
    static final TextureWrap CLAMP_TO_EDGE;
    static final TextureWrap MIRRORED_REPEAT;
\`\`\`

### Winding

The vertex order of a front face.

\`\`\`java
enum Winding
    static final Winding CLOCKWISE;
    static final Winding COUNTER_CLOCKWISE;
\`\`\`

## dev.flagon.platform.input

### InputSource

Keyboard and mouse state and event queues.

\`\`\`java
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
\`\`\`

### KeyEvent

A key press or release.

\`\`\`java
class KeyEvent
    static final char NO_CHARACTER;
    KeyEvent(int key, char character, boolean pressed, boolean repeat, long nanos);
    int key();
    char character();
    boolean isPressed();
    boolean isRepeat();
    long nanos();
\`\`\`

### MouseEvent

A mouse button, move or wheel event.

\`\`\`java
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
\`\`\`

### Scancode

Key numbers, by physical position. Hosts report them and callers compare against them.

\`\`\`java
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
\`\`\`

## dev.flagon.platform.net

### Connection

A two-way byte stream to one address.

\`\`\`java
interface Connection
    String host();
    int port();
    boolean isOpen();
    int available();
    int read(byte[] into, int offset, int length); // reads at least one byte, blocking until there is one.
    void write(byte[] data, int offset, int length);
    void close(); // closes the connection; closing one already closed does nothing.
\`\`\`

### Fetch

Sends HTTP requests for \`java.net.http\`. \`send\` pauses only the calling green thread.

\`\`\`java
interface Fetch
    String name();
    boolean isAvailable();
    Response send(Request request); // throws TimedOut, or NetworkException when it fails.
    static Fetch unavailable(String reason); // refuses every request with this reason.
\`\`\`

### Fetch.Request

One request. \`timeoutMillis\` 0 waits without limit. With \`followRedirects\` false, a redirect fails.

\`\`\`java
record Fetch.Request(String method, String url, List<Map.Entry<String, String>> headers,
                     byte[] body, long timeoutMillis, boolean followRedirects)
\`\`\`

### Fetch.Response

The whole answer. \`url\` is where it came from after redirects. Header names are lowercase.

\`\`\`java
record Fetch.Response(int status, String url, Map<String, List<String>> headers, byte[] body)
\`\`\`

### Fetch.TimedOut

No answer arrived within the request's timeout.

\`\`\`java
class Fetch.TimedOut extends NetworkException
    TimedOut(String message);
\`\`\`

### MemoryNetwork

A \`Network\` whose servers are objects in the heap. For tests.

\`\`\`java
class MemoryNetwork implements Network
    void serve(String host, int port, Service service);
    List<String> secured(); // what the tls handshake was asked for, in the order it was asked
    void refuse(String host, int port);
\`\`\`

### MemoryNetwork.Service

A server: one request in, one response out.

\`\`\`java
interface MemoryNetwork.Service
    byte[] answer(byte[] request);
\`\`\`

### Network

Opens connections.

\`\`\`java
interface Network
    String name();
    boolean isAvailable();
    Connection connect(String host, int port);
    default Connection connectSecure(String host, int port);
\`\`\`

### NetworkException

A connection that failed to open or failed while open.

\`\`\`java
class NetworkException extends RuntimeException
    NetworkException(String message);
    NetworkException(String message, Throwable cause);
\`\`\`

### UnavailableNetwork

The \`Network\` of a host without one. Every connect throws.

\`\`\`java
class UnavailableNetwork implements Network
    UnavailableNetwork(String reason);
\`\`\`

## dev.flagon.platform.workers

Bundle workers: the running bundle started again in a Web Worker, which runs one catalog program and talks to its parent by messages.

\`\`\`java
Channel echo = Platforms.get().workers().spawn("worker-echo");
echo.send(new Message("frame", pixels));
Message answer = echo.receive();
echo.close();
\`\`\`

### Channel

One end of the channel between a bundle and a worker. Messages keep their order. \`receive\` pauses only the calling green thread.

\`\`\`java
interface Channel extends AutoCloseable
    void send(Message message);
    default void send(String text);
    default void send(byte[] data);
    Message receive(); // null once the other end has closed and every message is read.
    Message poll(); // null when none has arrived yet.
    boolean isOpen(); // false once the other end has closed or the worker has exited.
    void close(); // closing a spawned worker's channel stops the worker.
\`\`\`

### Message

Text, bytes or both. The bytes cross to the other thread as a copy in a transferred \`ArrayBuffer\`.

\`\`\`java
record Message(String text, byte[] data)
\`\`\`

### Workers

Starts workers and finds the parent. A worker has no display and no audio output. Its console lines reach the parent's console, prefixed with the program id.

\`\`\`java
interface Workers
    boolean isAvailable();
    Channel spawn(String program, String... arguments); // runs this catalog program in a new worker.
    Channel parent(); // the spawning bundle's channel; null outside a worker.
    static Workers unavailable(String reason); // refuses every spawn with this reason.
\`\`\`

## dev.flagon.platform.wasi

### WasiPlatform

The headless \`Platform\` of a [WASI component](packaging.md#wasi): stdio, clocks, arguments and files. \`create\` takes the arguments \`main\` received. \`java.io\` works below the first preopened directory, as \`/\`, or in memory when there is none.

\`\`\`java
class WasiPlatform implements Platform
    static WasiPlatform create(String[] arguments);
\`\`\`

## dev.flagon.platform.browser

### BrowserClock

\`Date.now()\` and \`performance.now()\`.

\`\`\`java
class BrowserClock implements Clock
\`\`\`

### BrowserConsole

Writes log lines to the host bridge, or to \`console\`.

\`\`\`java
class BrowserConsole implements ConsoleSink
\`\`\`

### BrowserPlatform

The browser \`Platform\`. \`create\` takes the arguments \`main\` received.

\`\`\`java
class BrowserPlatform implements Platform
    static BrowserPlatform create(String[] arguments);
    Fetch fetch(); // a BrowserFetch.
\`\`\`

### BrowserRuntimeHost

The \`RuntimeHost\` over \`window.__flagonHost\`.

\`\`\`java
class BrowserRuntimeHost implements RuntimeHost
\`\`\`

### HostBridge

The JavaScript \`window.__flagonHost\` object. See [Host bridge](architecture.md#host-bridge).

\`\`\`java
interface HostBridge extends JSObject
    void log(String level, String text);
    void catalog(JSArray<JSString> ids, JSArray<JSString> descriptions);
    void status(String phase, String detail);
    void ready();
    void exited(int code);
    void fatal(String phase, String message, String stack);
\`\`\`

## dev.flagon.platform.browser.audio

### WebAudioDevice

The \`AudioDevice\` on Web Audio. Each playing source has its own node graph.

\`\`\`java
class WebAudioDevice implements AudioDevice
    static WebAudioDevice createIfAvailable(ConsoleSink console);
\`\`\`

## dev.flagon.platform.browser.display

### CanvasSurface

The \`DisplaySurface\` on the page's canvas.

\`\`\`java
class CanvasSurface implements DisplaySurface
    static final String CANVAS_ID;
    static CanvasSurface open(int width, int height, String title, Clock clock);
    boolean isDestroyed();
\`\`\`

### Frames

The frame loop. \`awaitNextFrame\` suspends the thread until the next \`requestAnimationFrame\`.

\`\`\`java
class Frames
    static void awaitNextFrame();
\`\`\`

## dev.flagon.platform.browser.fs

### OpfsFileStore

The \`FileStore\` on the Origin Private File System.

\`\`\`java
class OpfsFileStore implements FileStore
    static OpfsFileStore createIfAvailable();
\`\`\`

## dev.flagon.platform.browser.graphics

### BrowserDrawing

The \`Drawing\` over \`OffscreenCanvas\`. It decodes with \`createImageBitmap\`, through \`self.__flagonImages\`.

\`\`\`java
class BrowserDrawing implements Drawing
\`\`\`

### WebGL2

The JSO binding of \`WebGL2RenderingContext\`. Its methods mirror the [WebGL 2 API](https://developer.mozilla.org/docs/Web/API/WebGL2RenderingContext).

\`\`\`java
interface WebGL2 extends JSObject
\`\`\`

### WebGLGraphicsDevice

The \`GraphicsDevice\` on WebGL 2.

\`\`\`java
class WebGLGraphicsDevice implements GraphicsDevice
    WebGLGraphicsDevice(WebGL2 gl);
    void resizeScreen(); // sizes the back buffer to the canvas; call after the canvas changes size
    void presentScreen(); // copies the finished frame to the canvas
\`\`\`

## dev.flagon.platform.browser.js

### JsArrays

Converts between Java arrays and JavaScript typed arrays.

\`\`\`java
class JsArrays
    static byte[] bytesOf(Int8Array values);
    static Int8Array bytes(byte[] values, int length);
    static Float32Array floats(float[] values, int length);
    static Uint8Array unsignedBytes(byte[] values, int length);
    static Int32Array ints(int[] values, int length);
    static Uint32Array unsignedInts(int[] values, int length);
    static Uint8Array rgba(int[] argb, int count, Uint32Array into);
\`\`\`

## dev.flagon.platform.browser.net

### BrowserFetch

The \`Fetch\` over the page's \`fetch\`, through \`window.__flagonFetch\`. The page's CORS rules apply.

\`\`\`java
class BrowserFetch implements Fetch
\`\`\`

### ProxyNetwork

The \`Network\` over WebSockets to the socket proxy.

\`\`\`java
class ProxyNetwork implements Network
    static ProxyNetwork createIfAvailable();
\`\`\`

## dev.flagon.platform.browser.workers

### BrowserWorkers

The \`Workers\` over Web Workers, through \`self.__flagonWorkers\`. A worker loads the same bundle URL as the page.

\`\`\`java
class BrowserWorkers implements Workers
\`\`\`

## dev.flagon.web

\`platform/web\`, published as \`dev.flagon:platform-web\`: JSO bindings for the web APIs that TeaVM's \`jso-apis\` lacks, and an IndexedDB store. Add it to a bundle that uses them.

Each binding tells whether the browser has its API: \`isSupported()\`, or \`get()\` that returns null. A method that returns a \`JSPromise\` does not wait. \`Promises.await\` waits for it.

\`\`\`java
RTCPeerConnection peer = new RTCPeerConnection();
RTCSessionDescriptionInit offer = Promises.await(peer.createOffer());
\`\`\`

### Promises

Waits for a promise. The Java thread parks, and the page keeps running.

\`\`\`java
final class Promises
    static <T> T await(JSPromise<T> promise); // the value; throws WebException when it rejects
\`\`\`

### WebException

What a web API rejected with.

\`\`\`java
class WebException extends RuntimeException
    WebException(String name, String message);
    String name(); // the DOMException name, such as NotFoundError or SecurityError
    static RuntimeException of(Object error); // the exception for a rejection value
\`\`\`

## dev.flagon.web.indexeddb

### IndexedDb

An IndexedDB database. Each call parks the Java thread until the browser answers.

\`\`\`java
final class IndexedDb implements AutoCloseable
    static boolean isSupported();
    static IndexedDb open(String name, int version, String... stores); // makes the stores it lacks
    static IndexedDb open(String name, int version, Upgrade upgrade); // an exception from upgrade aborts it and is thrown here
    static void delete(String name); // once every other connection closes
    String name();
    int version();
    String[] stores();
    Store store(String name); // throws WebException NotFoundError
    void close();
\`\`\`

The database closes itself when another tab opens a newer version.

### IndexedDb.Upgrade

Changes the stores when the database is new or older than the version that \`open\` asks for.

\`\`\`java
interface Upgrade
    void upgrade(Schema schema, int from, int to); // from is 0 for a new database
\`\`\`

### Schema

The stores of a database during its upgrade.

\`\`\`java
final class Schema
    boolean has(String store);
    String[] stores();
    void create(String store);
    void delete(String store);
    void clear(String store);
\`\`\`

### Store

One object store. Its keys are strings, and its values are bytes or strings. Each call is one transaction. A write returns when it commits.

\`\`\`java
final class Store
    String name();
    byte[] get(String key); // null when missing; throws IllegalStateException for a string
    String getString(String key); // null when missing; throws IllegalStateException for bytes
    void put(String key, byte[] value);
    void put(String key, String value);
    boolean has(String key);
    void delete(String key);
    List<String> keys(); // in IndexedDB order
    int size();
    void clear();
\`\`\`

## Web API bindings

The members of these types mirror the web APIs. Their names match the API's names. Dictionaries have a static \`create\` for their required members, and \`@JSProperty\` setters for the rest.

| Package | API | Types |
| --- | --- | --- |
| \`dev.flagon.web.codecs\` | [WebCodecs](https://developer.mozilla.org/docs/Web/API/WebCodecs_API) | \`VideoEncoder\`, \`VideoDecoder\`, \`AudioEncoder\`, \`AudioDecoder\`, \`VideoFrame\`, \`AudioData\`, \`EncodedVideoChunk\`, \`EncodedAudioChunk\`, \`VideoEncoderConfig\`, \`VideoDecoderConfig\`, \`AudioEncoderConfig\`, \`AudioDecoderConfig\`, \`VideoEncoderInit\`, \`VideoDecoderInit\`, \`AudioEncoderInit\`, \`AudioDecoderInit\`, \`VideoEncoderEncodeOptions\`, \`VideoFrameInit\`, \`VideoFrameBufferInit\`, \`AudioDataInit\`, \`AudioDataCopyToOptions\`, \`EncodedChunkInit\`, \`CodecSupport\`, \`EncodedVideoChunkOutputCallback\`, \`EncodedAudioChunkOutputCallback\`, \`VideoFrameOutputCallback\`, \`AudioDataOutputCallback\`, \`WebCodecsErrorCallback\` |
| \`dev.flagon.web.transport\` | [WebTransport](https://developer.mozilla.org/docs/Web/API/WebTransport_API) | \`WebTransport\`, \`WebTransportBidirectionalStream\`, \`WebTransportDatagramDuplexStream\`, \`WebTransportOptions\`, \`WebTransportHash\`, \`WebTransportCloseInfo\` |
| \`dev.flagon.web.streams\` | [Streams](https://developer.mozilla.org/docs/Web/API/Streams_API) | \`WritableStream\`, \`WritableStreamDefaultWriter\`. TeaVM's \`org.teavm.jso.streams\` has the readable side. |
| \`dev.flagon.web.rtc\` | [WebRTC](https://developer.mozilla.org/docs/Web/API/WebRTC_API) | \`RTCPeerConnection\`, \`RTCDataChannel\`, \`RTCConfiguration\`, \`RTCIceServer\`, \`RTCDataChannelInit\`, \`RTCSessionDescriptionInit\`, \`RTCIceCandidateInit\`, \`RTCPeerConnectionIceEvent\`, \`RTCDataChannelEvent\` |
| \`dev.flagon.web.bluetooth\` | [Web Bluetooth](https://developer.mozilla.org/docs/Web/API/Web_Bluetooth_API) | \`Bluetooth\`, \`RequestDeviceOptions\`, \`BluetoothLEScanFilter\`, \`BluetoothDevice\`, \`BluetoothRemoteGATTServer\`, \`BluetoothRemoteGATTService\`, \`BluetoothRemoteGATTCharacteristic\`, \`BluetoothCharacteristicProperties\` |
| \`dev.flagon.web.usb\` | [WebUSB](https://developer.mozilla.org/docs/Web/API/WebUSB_API) | \`USB\`, \`USBDevice\`, \`USBConfiguration\`, \`USBDeviceRequestOptions\`, \`USBDeviceFilter\`, \`USBControlTransferParameters\`, \`USBInTransferResult\`, \`USBOutTransferResult\`, \`USBConnectionEvent\` |

\`Bluetooth.requestDevice\` and \`USB.requestDevice\` show a device chooser. Call them from a click or key press, or they reject with \`SecurityError\`.
`,ke="# Architecture\n\nHow Flagon compiles a program and runs it in a page.\n\n## Layers\n\nEach layer uses only the layer under it.\n\n```text\napplication JAR, unmodified\n        │  compiled ahead of time\nlwjgl/lwjgl2 · lwjgl/lwjgl3     org.lwjgl.* APIs\n        │\nlwjgl/core                        GL and AL state, fixed-function pipeline, GLSL translator\n        │\nplatform/api                      host contracts: graphics, audio, files, input, network, clock\n        │\nplatform/browser                  WebGL 2, Web Audio, OPFS, DOM events, WebSocket tunnel\nplatform/wasi                     headless: WASI preview 1 files, clocks, args and stdio\n```\n\n### Rules\n\n1. **Host isolation.** Only `platform/browser` uses browser APIs, and only `platform/wasi` uses WASI. No other module names `window`, `document` or WebGL.\n2. **JVM tests.** `lwjgl/core` and the compat layers use only `platform/api`. Their tests run on a desktop JVM with recording and in-memory devices: `RecordingGraphicsDevice`, `SilentAudioDevice`, `MemoryFileStore` and `MemoryNetwork`.\n3. **Static access.** LWJGL calls are static, such as `Display.create()` and `GL11.glClear()`. The runtime installs the active `Platform` in `Platforms` at boot, and static code reads it from there.\n\n## Modules\n\nThe Gradle modules live in `modules/`. The project `:platform:api` is the directory `modules/platform/api`, and it publishes as `dev.flagon:platform-api`.\n\n### Runtime\n\n| Module | Role |\n| --- | --- |\n| `platform/api` | Host contracts: `Platform`, `GraphicsDevice`, `DisplaySurface`, `AudioDevice`, `FileStore`, `InputSource`, `Network`, `Clock`, `RuntimeHost`. Also the JDK reroute targets. |\n| `platform/browser` | Implements `platform/api` in the browser. |\n| `platform/wasi` | Implements `platform/api` headless, over WASI, for a [WASI component](packaging.md#wasi). |\n| `runtime` | Boots a platform, picks a program, runs it, and reports to the host. Installs `System.out`, URL handlers and shutdown hooks. |\n| `lwjgl/core` | GL and AL state machines, immediate mode, display lists, the fixed-function shader generator and the GLSL translator. No `org.lwjgl` classes. |\n| `lwjgl/lwjgl2` | The LWJGL 2 API. |\n| `lwjgl/lwjgl3` | The LWJGL 3 API. |\n\n### Class libraries\n\nEach adds JDK classes that TeaVM lacks.\n\n| Module | Adds |\n| --- | --- |\n| `toolchain/jdk-classlib` | Java serialization, `RecordComponent`, `Parameter`, `java.lang.management`, `javax.xml.parsers` with a DOM, `java.sql` dates, `javax.naming`, `Collator` and small stubs. |\n| `toolchain/time-classlib` | `java.time`, with locale data and zone rules from the page's `Intl`. |\n| `toolchain/concurrent-classlib` | `java.util.concurrent` executors, futures, locks and queues on green threads. |\n| `toolchain/socket-classlib` | `java.net` sockets and addresses over the socket proxy. |\n| `toolchain/http-classlib` | `java.net.http` over the page's `fetch`. |\n| `toolchain/nio-classlib` | `FileChannel`, `Selector` and the channel types. |\n| `toolchain/awt-classlib` | `Graphics2D` on an `OffscreenCanvas`, `java.awt.geom`, `BufferedImage`, `ImageIO` and an in-page clipboard. |\n| `toolchain/crypto-classlib` | Digests, HMAC, PBKDF2, AES, RSA, EC keys, ECDSA and ECDH. |\n| `toolchain/sound-classlib` | `javax.sound` types with no devices. |\n\n### Build tools\n\n| Module | Role |\n| --- | --- |\n| `toolchain/jdk-shims` | TeaVM class transformers. They reroute JDK calls that TeaVM lacks to `platform/api`, and write the class-file facts TeaVM drops (generic signatures, record components, parameter names, default `serialVersionUID`) into each reflectable class. |\n| `toolchain/reflect-policy` | `@Reflectable` and the compiler plugin that decides which classes keep their members. |\n| `toolchain/app-entry` | Reads the JARs in `vendor/` and writes the `app` bundle's entry point. |\n| `toolchain/classfile` | A constant-pool reader. It tells whether a classpath has every class and method a class names. |\n| `toolchain/resource-pack` | Writes `<bundle>.pack` and `<bundle>.files/`, and re-encodes Vorbis. |\n| `toolchain/js-types` | Writes `js/<bundle>.d.ts` from the class files of a bundle's `@JSExport` methods and the JSO types they name. |\n| `teavm/core` | `teavm-core` 0.15.0 rebuilt with compiler and class library fixes. `java.text` and `Locale` read their locale data from the page's `Intl`, not from CLDR data in the bundle. |\n| `teavm/jso` | `teavm-jso-impl` 0.15.0 rebuilt so a simple `@JSBody` compiles to backend operations, not text. |\n| `teavm/patches` | Builds `teavm/core` and `teavm/jso` before the main build. The TeaVM plugin loads them in place of the stock JARs. |\n| `build-logic` | The Gradle convention plugins. `flagon.bundle` applies the published `dev.flagon.bundle` from `build-logic/bundle-plugin`. |\n\n### Bundles and examples\n\n| Module | Contents |\n| --- | --- |\n| `bundles/sandbox` | The self-tests the browser tests run, and the LWJGL demos. |\n| `bundles/pausing` | A program that pauses during class setup, for the coroutine backend. |\n| `bundles/wasi` | Headless programs and exports for the WASI component, which `make wasi` runs under wasmtime. |\n| `examples/app`, `examples/thirdparty` | Sample LWJGL 2 programs and LWJGL's own LWJGL 3 sample, built against the real LWJGL. |\n| `examples/bundle` | The `example` bundle of those programs. |\n| `examples/standalone` | A build of its own that applies `dev.flagon.bundle` from the local Maven repository. `make standalone` builds it. |\n| `bundles/app` | The `app` bundle from `vendor/`. Present when `vendor/` has JARs. |\n| `web/playground` | A separate build. Compiles javac and TeaVM 0.17 to WebAssembly for the playground. |\n| `web` | The site, the bundle loader, the socket proxy and the Playwright tests. |\n\n## Build pipeline\n\nTeaVM compiles the bytecode ahead of time. The page has no JVM, class loader or JIT.\n\n```text\nbytecode (.class, .jar)\n  │\n  ├─ jdk-shims         reroute JDK calls, write reflection data\n  ├─ reflect-policy    choose what reflection keeps\n  ▼\nTeaVM (patched 0.15.0)\n  ├─ reachability      remove code nothing calls\n  ├─ async transform   let threads pause\n  │\n  ├─► wasm/<name>.wasm   WebAssembly GC, coroutine pauses\n  ├─► jspi/<name>.wasm   WebAssembly GC, JSPI pauses\n  ├─► js/<name>.js       JavaScript\n  └─► wasi/<name>.wasm   WASI preview 2 component, by the wasi task\nresources ─► <name>.pack, <name>.files/\n```\n\n### Backends\n\n- **WebAssembly GC.** Java objects and arrays live in the GC heap. `JsArrays` copies data out when it crosses into browser APIs, such as a WebGL upload.\n- **JSPI.** The same program without the coroutine rewrite. `WebAssembly.Suspending` parks the whole stack, so the code is smaller and faster.\n- **JavaScript.** Java arrays are typed arrays and cross into browser APIs without a copy.\n- **WASI.** The coroutine build with `flagon.wasi` set. Each JavaScript import TeaVM would make becomes Java over WASI preview 1, and the heap's memory is exported. `wasm-tools` embeds the WIT world of the `@Export` methods and adapts preview 1 to preview 2 with wasmtime's adapter.\n\n### Pausing\n\nBrowser APIs are asynchronous, and Java code expects blocking calls. TeaVM finds every method that can reach an asynchronous call and rewrites it to save and restore its frame. A virtual call is asynchronous when any override can pause, so one pausing override makes every caller of that method pausable. That makes the coroutine bundle bigger. The JSPI backend skips the rewrite.\n\n## Subsystems\n\n### Graphics\n\n- `GLState` tracks the whole GL state and applies only the differences before each draw.\n- Programs switch between fixed-function shaders and application shaders at `glUseProgram`.\n- Immediate mode, client arrays and display lists go into stream buffers.\n- `FixedFunctionPipeline` generates a shader for each combination of lighting, texturing, fog and alpha test in use.\n- `GlslTranslator` parses desktop GLSL 1.10 to 3.30 and writes GLSL ES 1.00 or 3.00.\n\n### Threads\n\n`concurrent-classlib` runs threads as green threads on the page's thread. A thread pauses at I/O, `Thread.sleep`, `Object.wait`, a held monitor or lock, and at the end of each frame. Code that never pauses holds the page until it returns.\n\n### Files\n\n`OpfsFileStore` holds the files in OPFS. `StoreFileSystem`, in `platform/api`, puts `java.io` on any `FileStore`. A file loads into memory when it opens and writes back on `flush` and `close`. `RandomAccessFile` writes through in `rwd` and `rws` modes.\n\n### Audio\n\n`WebAudioDevice` maps OpenAL buffers and sources to Web Audio nodes. `ALState` holds the OpenAL state. Each source has a `PannerNode`. Doppler sets the playback rate, because Web Audio removed its own.\n\n### Network\n\n`ProxyNetwork` opens one WebSocket to the socket proxy per connection. `NetworkSocket` gives `java.net.Socket` its stream. The proxy makes TLS connections for `https` URLs, through a `URLStreamHandlerFactory` the runtime installs.\n\n### Input\n\nDOM events go into bounded queues. `Frames.awaitNextFrame()` drains them once per frame. Keys map from `KeyboardEvent.code`. Mouse Y is flipped to GL's origin.\n\n## Reflection policy\n\nThe compiler removes members nothing calls by name. These settings keep them:\n\n| Need | Setting | Keeps |\n| --- | --- | --- |\n| Class identity | none | `getClass`, `isInstance`, `cast`, `forName` for kept classes. |\n| Members of a class | `@Reflectable`, `flagon.reflect.classes` | Fields, methods, constructors and their metadata. |\n| Members in `vendor/` | `vendor/reflect.txt` | Adds its globs to `flagon.reflect.classes`. |\n| Proxies | `flagon.reflect.proxies` | A proxy class made at compile time. |\n| Constructors in `vendor/` | `META-INF/flagon/constructed.txt` | Constructors of the classes the JARs name by a literal, a string, a resource or a field type. `-Pflagon.app.reflect` narrows them. |\n| Classpath scans | `flagon.preserve`, `vendor/preserve.txt` | Classes that only a scan names. |\n| Serialization | `Serializable` | Fields, the serial methods and a constructor that skips the serializable ones. An enum keeps none, since it goes by name. |\n\nThere is no bytecode in the page. Transform the JARs before the build.\n\n## Host bridge\n\nThe page sets `window.__flagonHost` before it starts a bundle. The runtime reports to it:\n\n```ts\ninterface HostBridge {\n  log(level: string, text: string): void;\n  catalog(ids: string[], descriptions: string[]): void;\n  status(phase: string, detail: string): void;\n  ready(): void;\n  exited(code: number): void;\n  fatal(phase: string, message: string, stack: string): void;\n}\n```\n\n| Call | When |\n| --- | --- |\n| `log` | Each console line. `level` is `DEBUG`, `INFO`, `WARN` or `ERROR`. |\n| `catalog` | At boot, with the bundle's programs. |\n| `status` | At each phase: `platform`, `select`, `program`. |\n| `ready` | Just before the program's `main`. |\n| `exited` | When the program ends or calls `System.exit`. |\n| `fatal` | When a phase throws. |\n\nWithout `window.__flagonHost`, the runtime writes to `console`.\n",Se="# Compatibility\n\nWhat desktop Java and LWJGL code does in a browser. CI runs the self-tests on every backend in Chromium and Firefox.\n\n## Backends\n\nEvery build makes three bundles. The page picks one at load time.\n\n| Backend | Browsers | Notes |\n| --- | --- | --- |\n| WebAssembly GC with JSPI | Chrome 137+ | Pauses the WebAssembly stack. The smallest and fastest. |\n| WebAssembly GC | Chrome 119+, Firefox 120+, Safari 18.2+ | Pauses through a coroutine rewrite. |\n| JavaScript | All others | Java arrays are typed arrays. |\n\nAll bundles compile with TeaVM's strict mode, so a null dereference throws `NullPointerException` and a bad index throws `ArrayIndexOutOfBoundsException`.\n\n## Platform services\n\n| Area | Browser API | Behavior |\n| --- | --- | --- |\n| Wall clock | `Date.now()` | Millisecond resolution. |\n| Monotonic clock | `performance.now()` | The browser clamps the resolution. |\n| `System.out`, `System.err` | `console` | Line-buffered. Also sent to the host page. |\n| Files | Origin Private File System | Persistent per origin. See [Files](#files). |\n| Threads | none | Green threads on one core. See [Threads](#threads). |\n| Bundle workers | Web Workers | `dev.flagon.platform.workers` runs a catalog program on another core and passes it messages. See [Threads](#threads). |\n| Sockets, HTTP | WebSocket | Through the socket proxy. See [Networking](#networking). |\n| `java.net.http` | `fetch` | The page's CORS rules apply. See [Networking](#networking). |\n| Graphics | WebGL 2 | Desktop GL 1.1 to 3.3. See [OpenGL](#opengl). |\n| Audio | Web Audio | OpenAL 1.1. See [Audio](#audio). |\n| Input | DOM events, Pointer Lock | Keyboard, mouse, wheel and one touch point. |\n| Web APIs | WebCodecs, WebTransport, WebRTC, Web Bluetooth, WebUSB, IndexedDB | Bindings in `platform/web`. Firefox has no Web Bluetooth or WebUSB, so `get()` returns null there. See the [API reference](api.md#web-api-bindings). |\n\n## Java runtime\n\n### Not supported\n\n- **Class loading at run time.** There is no bytecode in the page, so `ClassLoader.defineClass` and runtime ASM, Mixin, Forge or Fabric loaders cannot work. Transform the JARs before the build.\n- **Processes and native code.** `Runtime.exec`, `System.loadLibrary` and `System.load` throw `UnsupportedOperationException`.\n- **Listening sockets and UDP.** `ServerSocket`, `DatagramSocket` and `MulticastSocket` throw.\n- **Parallel CPU work with shared memory.** Threads take turns on one core. A bundle worker runs on another core, but shares no heap: it talks by messages.\n\n### Reflection\n\nThe compiler removes every member that code does not call directly. By default:\n\n- `Class.forName`, `isInstance`, `getSuperclass`, `getInterfaces` and `cast` work for every kept class.\n- `getDeclaredFields`, `getDeclaredMethods`, `getDeclaredConstructors` and their public forms return empty arrays. They do not throw.\n- `isRecord`, `getPackageName`, `arrayType`, `componentType`, `descriptorString`, `getAnnotationsByType` and `isAnonymousClass` work.\n- Typed `Field` and `Array` reads and writes (`getInt`, `setLong` and so on) widen as on the JVM.\n\nOpt a class in to keep its fields, methods and constructors:\n\n- Annotate it with `@Reflectable` from `toolchain/reflect-policy`.\n- Or name it in `reflectClasses` (bundle module) or `vendor/reflect.txt`.\n\nAn opted-in class also keeps:\n\n- generic superclass, interfaces and type variable bounds;\n- record components and permitted subclasses;\n- the enclosing method or constructor, when the enclosing class is opted in too;\n- annotation member defaults and `Method.isDefault`;\n- parameter names when compiled with `-parameters`, else `arg0`, `arg1` and so on.\n\nOther settings:\n\n| Setting | Keeps |\n| --- | --- |\n| `reflectProxies` | A proxy class for each matching interface, for `Proxy.newProxyInstance`. |\n| `reflectConverted` | `valueOf(String)` and the `(String)` constructor, which option parsers call. |\n| `flagon.preserve`, `vendor/preserve.txt` | Classes that only a classpath scan names. |\n\nA reflective call to a removed member logs which class to opt in.\n\n### Serialization\n\n`ObjectOutputStream` and `ObjectInputStream` use the JVM's stream format, protocol version 2. The browser writes the same bytes as the JVM for the same objects, and reads streams the JVM wrote.\n\n- **Your classes:** every `Serializable` class is kept and found by name. The build computes the default `serialVersionUID` from the class file.\n- **Features:** `writeObject`, `readObject`, `readObjectNoData`, `writeReplace`, `readResolve`, `Externalizable`, records, enums, arrays, `serialPersistentFields`, `putFields`, `readFields` and `registerValidation`.\n- **JDK classes:** boxed primitives, `String`, `ArrayList`, `LinkedList`, `ArrayDeque`, `HashMap`, `LinkedHashMap`, `HashSet`, `LinkedHashSet`, `TreeMap`, `TreeSet`, `Date`, `UUID`, `List.of`, `Set.of`, `Map.of`, the `Collections` empty, singleton and unmodifiable wrappers, `StackTraceElement`, and `Throwable` with its common subclasses. Other JDK classes throw `NotSerializableException`.\n- **Limits:** no proxy classes. `HashMap` writes a bucket count from its size, not its history. A `LinkedHashMap` in access order reads back in insertion order.\n\n### Resources\n\nThe build puts every non-class file of the JARs into `<bundle>.pack`, gzipped. The page streams the pack with the bundle and keeps it in Cache Storage.\n\n- `getResourceAsStream` reads from the pack. `Class.getResourceAsStream` resolves a relative name beside the class.\n- `getResource` returns a `flagon:` URL for a file in the pack, and `null` for other names. `openStream` on the URL reads the file.\n- `getResources` returns at most one URL. A bundle holds one copy of each name, so a `META-INF/services` scan finds one file.\n- Files of `apartFrom` size or larger, or named in `vendor/apart.txt`, load in the background after the program starts, smallest first. A read of one before it arrives throws `IOException`. Reads never wait, because the reader can hold a lock. On a later visit, the files already in the browser cache are in before the program starts.\n\n### JDK shims\n\nThese JDK calls act differently in a browser:\n\n| API | Behavior |\n| --- | --- |\n| `Runtime.maxMemory()` | 512 MB. |\n| `Runtime.availableProcessors()` | The browser's `navigator.hardwareConcurrency`. |\n| `System.exit`, `Runtime.exit` | Run the shutdown hooks, then stop the program. |\n| `Runtime.halt` | Stops the program without hooks. |\n| Shutdown hooks | Run on exit. Not run when the tab closes or reloads. |\n| `ManagementFactory` | Uptime, start time and heap use from the runtime. |\n| `new UUID(long, long)`, `UUID.fromString` | Work. |\n| `Class.isAnonymousClass` | True when the name after the last `$` is all digits. |\n| `FileInputStream.getChannel`, `FileOutputStream.getChannel` | Return a `FileChannel` over the file. |\n| `Charset.isSupported` | True for the charsets TeaVM carries. |\n| `HexFormat` | Works. |\n\n### Class libraries\n\n| Area | Supported | Not supported |\n| --- | --- | --- |\n| `java.security`, `javax.crypto` | `MessageDigest` MD5, SHA-1, SHA-256, SHA-384 and SHA-512. `Mac` HMAC over each. `SecretKeyFactory` `PBKDF2WithHmac*`. `Cipher` `AES/CFB8/NoPadding` and `RSA/ECB/PKCS1Padding`. `Signature` SHA-1 and SHA-2 with RSA or ECDSA. `KeyAgreement` ECDH. RSA and EC (secp256r1, secp384r1, secp521r1) keys from `KeyPairGenerator` and `KeyFactory`, with X.509 and PKCS#8 encodings. `KeyGenerator` AES. | Other algorithms throw `NoSuchAlgorithmException`. No `KeyStore`. EC math is not constant-time. |\n| `java.awt`, `java.awt.geom` | `Graphics2D` on a `BufferedImage`: shapes, text, transforms, clips, `BasicStroke` and `AlphaComposite`, drawn on an `OffscreenCanvas`. Opaque fills, thin straight lines and unscaled copies are drawn in Java and match the JDK pixel for pixel. Logical fonts map to the browser's `sans-serif`, `serif` and `monospace`. The `geom` shapes, `Path2D` and `AffineTransform`. | Aliased canvas drawing: shapes and text are always antialiased. Paints other than `Color`. `Area`, `Arc2D`, `GlyphVector`, XOR mode. Windows. |\n| `java.awt.image`, `javax.imageio` | `BufferedImage` with the integer, byte and gray types. `ImageIO` reads PNG in Java, and JPEG, GIF, BMP, WebP and interlaced PNG with the browser. It writes PNG. | Writing other formats. `ImageReader` and `ImageWriter`. |\n| `java.awt.datatransfer` | A clipboard inside the page. | The system clipboard. |\n| `java.nio.channels` | `FileChannel`. `Selector.open()` with no keys: `select` waits for its timeout or `wakeup`, so a Netty event loop runs tasks and local channels. | `SocketChannel`, `ServerSocketChannel`, `DatagramChannel`. |\n| `javax.sound.midi` | The types. | Every device: `MidiSystem` throws `MidiUnavailableException`. |\n| `javax.xml.parsers`, `org.w3c.dom` | `DocumentBuilder` parses XML into a DOM. | Schema validation, XSLT. |\n| `java.sql` | `Date`, `Time`, `Timestamp`. | JDBC. |\n| `java.text`, `java.util.Locale` | `NumberFormat`, `DecimalFormat`, `DateFormat`, `SimpleDateFormat`, `DateFormatSymbols`, `Collator`, and locale and currency names, with the page's `Intl` data for each locale. | `RuleBasedCollator`, `BreakIterator`, `Normalizer`. A `CollationKey` compares, but its bytes do not sort. |\n| `java.time` | `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`, `Duration`, `Period`, `ZonedDateTime`, `OffsetDateTime`, `Year`, `YearMonth`, `Clock`, and `ZoneId` with the page's zone rules. `DateTimeFormatter` with the ISO formats, patterns and localized styles. The `ChronoField` and `ChronoUnit` fields and units, and `TemporalAdjusters`. | Other calendars, week fields, `MonthDay`, `OffsetTime`, `DateTimeFormatterBuilder`, `ZoneId.getAvailableZoneIds`. |\n| `javax.naming` | The types. | Every lookup throws `NameNotFoundException`. |\n\n## Files\n\n`java.io` works on the Origin Private File System (OPFS).\n\n- In a private window, or where OPFS refuses access, files live in memory for the session.\n- OPFS calls suspend the calling green thread. Java code sees blocking calls.\n- A file loads fully into memory when it opens.\n- Streams write to OPFS on `flush()` and `close()`.\n- `RandomAccessFile` writes on `close()`, or on every write in `rwd` and `rws` modes.\n\n### Shipped files\n\nFolders beside the JARs, such as `vendor/assets/`, ship as read-only files under `/`.\n\n- Reads never touch OPFS.\n- A program can add files to a shipped folder, but cannot replace a shipped file.\n- A file that loads after start opens at once. Its first read throws `IOException` until it arrives.\n\n## Networking\n\nEach socket goes through a WebSocket to the socket proxy, which opens the TCP or TLS connection.\n\n```text\ngreen thread  ──WebSocket──>  socket proxy  ──TCP/TLS──>  server\n```\n\n| API | Behavior |\n| --- | --- |\n| `Socket` | Works through the proxy. |\n| `URL`, `HttpURLConnection` | Work through the proxy. |\n| `HttpsURLConnection` | The proxy makes the TLS connection. Casts to `javax.net.ssl.HttpsURLConnection` and certificate pinning do not work. |\n| `Socket.setSoTimeout` | Stored. Reads still wait for data. |\n| `InetAddress.getByName` | Returns an unresolved address. The proxy resolves the name. |\n| `ServerSocket` | Throws: a page cannot listen. |\n| `DatagramSocket`, `MulticastSocket` | Throw: the transport carries streams only. |\n\nSee [Socket proxy](packaging.md#socket-proxy) to run one.\n\n### java.net.http\n\n`HttpClient` sends each request with the page's `fetch`, not the socket proxy. `send` pauses the calling green thread. `sendAsync` runs `send` on the client's executor, or on a new thread.\n\n| API | Behavior |\n| --- | --- |\n| Cross-origin requests | The server must allow them with CORS headers. |\n| `Redirect.NEVER`, the default | A redirect throws `IOException`: a browser does not show where it points. Use `NORMAL` or `ALWAYS` to follow it. |\n| Request headers | The browser drops the ones it forbids, such as `Cookie`. |\n| `timeout`, `connectTimeout` | Cover the wait for the response headers. |\n| Response bodies | Arrive whole before a body handler sees them. `ofInputStream` reads from memory. |\n| `version()` of a response | `HTTP_1_1`. The browser does not tell. |\n| Cookies, proxies, TLS settings | The browser handles them. The builder has no `cookieHandler`, `proxy`, `sslContext` or `sslParameters`. |\n| `close`, `shutdownNow` | Stop new requests. Requests already sent still finish. |\n| `WebSocket` | Not supported. |\n\n## Threads\n\nThreads are green threads on the page's one thread. They give concurrency, not parallelism. A thread yields when it waits; code that never waits holds the page until it returns.\n\nFor parallel work, `Platforms.get().workers().spawn(program)` starts the bundle again in a Web Worker and runs that catalog program there. The two sides pass text and bytes over a `Channel`. A worker has no display and no audio output. See [the API reference](api.md#devflagonplatformworkers).\n\n| API | Behavior |\n| --- | --- |\n| `Thread.sleep`, `Object.wait`, `notify` | Suspend the green thread and yield. |\n| `synchronized` | Suspends the thread while another holds the monitor. |\n| `Executors.newFixedThreadPool(n)` | `n` green threads over one queue. |\n| `Executors.newCachedThreadPool()` | A fixed pool of `availableProcessors()` threads. |\n| `Executors.newWorkStealingPool()` | A `ForkJoinPool`. |\n| `ScheduledThreadPoolExecutor` | Works. |\n| `ExecutorService.close()` | Works in try-with-resources. An executor is not an `AutoCloseable`. |\n| `ForkJoinPool`, `RecursiveTask`, `RecursiveAction`, `CountedCompleter` | Work, with `commonPool()`. A worker that joins a task no thread has started runs it itself. |\n| `CompletableFuture` | The whole `CompletionStage` API, `orTimeout`, `completeOnTimeout` and `delayedExecutor`. Failures wrap in `CompletionException` as on the JVM. |\n| `ReentrantLock`, `Condition`, `Semaphore`, `CountDownLatch` | Suspend the thread while blocked. |\n| `CyclicBarrier`, `Phaser`, `Exchanger` | Work, with timeouts and tiered phasers. |\n| `ReentrantReadWriteLock` | Works. Taking the write lock while holding the read lock throws `IllegalStateException`. |\n| `StampedLock` | Works, with optimistic reads and stamp conversion. |\n| `ConcurrentHashMap`, `ConcurrentSkipListMap`, `ConcurrentSkipListSet`, atomics | Work. Iterators of the skip-list types walk a snapshot. |\n| `ConcurrentLinkedQueue`, `LinkedBlockingQueue`, `PriorityBlockingQueue`, `DelayQueue` | Work. |\n\nThe `java.util.concurrent` self-test prints the same lines in the browser as on the JVM.\n\n## WASI\n\nA [WASI component](packaging.md#wasi) runs headless in a WASI preview 2 host. wasmtime 51 and later runs it with `-W gc=y,function-references=y,exceptions=y`.\n\n| Area | Behavior |\n| --- | --- |\n| Arguments | The first picks the program. The rest go to it. |\n| `System.out`, `System.err` | stdout and stderr, as UTF-8. |\n| Clocks, `Thread.sleep` | WASI clocks. A sleep on every thread blocks the component. |\n| Threads | Green threads on one core, as in a page. |\n| Files | `java.io` under the first preopened directory, which is `/`. With none, files are in memory until exit. |\n| Exit status | `System.exit` runs the shutdown hooks. Preview 2 reports success or failure, so a status other than 0 exits with 1. |\n| `@Export` methods | Functions of the component. Primitives only. |\n| Exceptions | The class and message print. There is no stack trace. |\n| Sockets | A connection throws `NetworkException`. |\n| Display, `java.net.http`, `java.awt` drawing, bundle workers | Refused with `UnsupportedCapabilityException`. `ImageIO` throws `IOException`. |\n| Audio | Plays silently. |\n| Locale data | en-US for every locale. Every zone is UTC. |\n\n## LWJGL\n\n| Module | Supplies |\n| --- | --- |\n| `lwjgl/lwjgl2` | `Display`, `GL11` to `GL20` and `GL11C` to `GL20C`, the common ARB and EXT extensions, `Keyboard`, `Mouse`, OpenAL, `BufferUtils`, `GLU`, `util.vector` and `WaveData`. |\n| `lwjgl/lwjgl3` | `GLFW` with its callbacks, `GL30` to `GL33` and `GL30C` to `GL33C`, `MemoryUtil`, `MemoryStack` and `PointerBuffer`. |\n\nAn LWJGL 3 program needs both modules. A class that LWJGL 2 and LWJGL 3 both name is in `lwjgl/lwjgl2` and has both forms. For example, `ALC10` takes an `ALCdevice` or a `long` handle.\n\n### Display\n\n- `Display.update()` and `glfwSwapBuffers()` copy the frame to the canvas, then wait for `requestAnimationFrame`. Drawing goes to a back buffer, so a half-drawn frame never shows.\n- `Display.sync(fps)` waits for frames to hold the rate.\n- `Display.setIcon` and `glfwSetWindowPos` do nothing. The page owns the tab icon and canvas position.\n\n## OpenGL\n\nDesktop GL 1.1 to 3.3, drawn with WebGL 2.\n\n| Feature | Support | Details |\n| --- | --- | --- |\n| Immediate mode | Yes | Each `glBegin`/`glEnd` block goes after the last one in a stream buffer, so a draw never waits on the one before. |\n| `GL_QUADS` | Unindexed only | Drawn as triangle pairs. Indexed quads throw: the caller's index buffer is not rewritten. |\n| Client arrays | Yes | Copied into buffers before the draw. Arrays interleaved in one buffer share one upload. |\n| Display lists | Yes | `glGenLists`, `glNewList`, `glCallList`, `glDeleteLists`. A list's blocks share one buffer per vertex layout. |\n| Lighting | Yes | 8 lights in eye space. One-sided: back faces use the front material. `glColorMaterial` tracks any one material. |\n| Matrix stacks | Yes | `GL_MODELVIEW`, `GL_PROJECTION`, and a `GL_TEXTURE` stack per unit. |\n| Texture environment | Yes | `GL_MODULATE`, `GL_REPLACE`, `GL_ADD`, `GL_DECAL`, `GL_BLEND`, and `GL_COMBINE` without `GL_DOT3`. |\n| Texture coordinate generation | Partly | Object linear, eye linear and sphere map for S, T, R and Q. `GL_NORMAL_MAP` and `GL_REFLECTION_MAP` throw. |\n| Flat shading | Yes | Takes the last vertex's color, as GL does. |\n| Fog, alpha test | Yes | Done in the generated shaders. |\n| Logic ops | No effect | WebGL has no logic op stage. The state is kept and draws use `GL_COPY`. |\n| Polygon modes | `GL_FILL` only | No wireframe or point mode. |\n| Textures | Yes | Byte formats convert to RGBA. No 16-bit integer channels. |\n| Mipmaps | Yes | Through `glGenerateMipmap`. All minification filters. |\n| Framebuffers | One color attachment | `GL_COLOR_ATTACHMENT1` and up throw. |\n| GLSL 1.10 to 3.30 | Translated | See below. |\n| GLSL above 3.30 | No | Throws `UnsupportedOperationException`. |\n\n### Shaders\n\nThe build-time translator rewrites desktop GLSL for WebGL 2:\n\n- A shader with `#version` becomes GLSL ES 3.00. One without becomes GLSL ES 1.00.\n- Names ES 3.00 reserves, such as `sample`, get a `flagon_` prefix.\n- Integer literals in float math become float literals.\n- A constant uniform initializer is set after the program links.\n- Fixed-function built-ins work: the matrices (`gl_ModelViewMatrix` and so on) become uniforms, and `gl_Vertex`, `gl_Color`, `gl_Normal` and `gl_MultiTexCoord*` become attributes.\n- A program with only one stage gets a generated other stage.\n\n## Audio\n\nOpenAL 1.1 on Web Audio.\n\n- Browsers start audio only after a user gesture. Flagon resumes audio on the first click or key press. Source states advance while audio waits, so game logic does not stall.\n- Inverse, linear and exponential distance models map to `PannerNode`. Distances are clamped.\n- Doppler changes the playback rate from the source and listener velocities.\n- Streaming with `alSourceQueueBuffers` and `alSourceUnqueueBuffers` works.\n- OpenAL is the LWJGL 2 and the LWJGL 3 form: `AL10`, `AL11`, `ALC10`, `ALC11`, and the LWJGL 3 `ALC`, `ALCCapabilities` and `ALCapabilities`. The capabilities report OpenAL 1.1 and no extension.\n- `ALC10.alcGetCurrentContext()` returns the LWJGL 2 `ALCcontext`. Java cannot also declare the LWJGL 3 form, which returns a `long`.\n- Not supported: capture (`ALC11`), cone outer gain, and unclamped distance models.\n\n## Input\n\n- Mouse Y is flipped to GL's bottom-left origin.\n- Buttons map from the DOM order (left, middle, right) to the LWJGL order (left, right, middle).\n- Wheel pixels add up into ticks of 120.\n- `Mouse.setGrabbed(true)` and `GLFW_CURSOR_DISABLED` use Pointer Lock. The browser needs a click first.\n- `Mouse.setCursorPosition` does nothing. Browsers do not move the cursor.\n- The first touch point acts as the left mouse button. No multi-touch or on-screen keyboard.\n- Keys map from `KeyboardEvent.code`, so the layout does not change key codes.\n",ye='# Packaging\n\nA browser cannot load a JAR at run time. The build links the application, its libraries and Flagon into one bundle ahead of time.\n\nThere are three ways to make a bundle:\n\n- **`vendor/`**: put the JARs in a folder and run `make app`. No build files.\n- **A bundle module**: a Gradle module in this repository that applies the `flagon.bundle` plugin. Use it for several programs, your own entry point or custom settings.\n- **Your own project**: a Gradle build outside this repository that applies the published `dev.flagon.bundle` plugin. See [From your own project](#from-your-own-project).\n\n## From vendor/\n\n1. Put the application JAR and its runtime dependencies in `vendor/`. Leave out the desktop LWJGL JARs and natives: Flagon supplies `org.lwjgl.*`.\n2. Run `make app`.\n3. Run `make dev` and open `/play/`.\n\nThe build:\n\n1. Merges the JARs in file name order. For a duplicate class, the first JAR wins.\n2. Finds the entry point from the `Main-Class` manifest header, or from a class with a `main` method.\n3. Writes a direct entry point, so the call graph survives tree shaking.\n4. Compiles the `app` bundle for every backend.\n\n### Files beside the JARs\n\nFolders in `vendor/` ship with the bundle. The program reads their files by path from the working directory `/`. They are read-only. See [Shipped files](compatibility.md#shipped-files).\n\n### vendor/preserve.txt, vendor/reflect.txt and vendor/apart.txt\n\nSome libraries find their own classes by scanning the classpath. Log4j 2 finds its pattern converters this way. List the packages such a library scans in `vendor/preserve.txt`.\n\nA library that calls methods or reads fields by name needs its classes in `vendor/reflect.txt`.\n\n`vendor/apart.txt` lists resources that load after the program starts, outside the pack. A glob matches a resource name, or the name a launcher asset index gives an object. Use it for files the program does not read at startup.\n\nThe three files take one glob per line. `*` stops at a package boundary and `**` does not. A line that starts with `#` is a comment.\n\n```text\n# vendor/reflect.txt\ncom.example.config.*\ncom.example.model.**\n```\n\n### Constructors\n\n`make app` keeps the constructors of the classes that the JARs can get a `Class` object of without an instance, so `Class.newInstance` and `Constructor.newInstance` work for them:\n\n- a class literal, such as `Foo.class`;\n- a string that holds a class name, as `Class.forName` takes it;\n- a class name in a file under `META-INF/`, such as a service list;\n- a type argument in a class\'s generic signature, as a Gson `TypeToken` subclass has;\n- a field type of a class above, which Gson constructs.\n\nA class found only by a name built at run time needs a line in `vendor/reflect.txt`. `make app` logs the count. It warns past 10,000 constructors and stops past 50,000. Narrow it to your packages:\n\n```bash\n./gradlew bundles "-Pflagon.app.reflect=com.example.**"\n```\n\n### Audio\n\n`make app` re-encodes Ogg Vorbis files at quality 0 through `ffmpeg`. It keeps the original where the new file saves less than 15%. Files of 256 KiB or more, such as music, load after the program starts, smallest first.\n\n### Minecraft\n\n`make minecraft` fills `vendor/` from a local launcher install: the game, the libraries that run in a browser, the assets in the launcher\'s layout, and the three glob files. Languages other than English and the world preset images load after the game starts.\n\n```bash\nmake minecraft MINECRAFT=1.8.9 LAUNCHER=~/.minecraft\nmake app\n```\n\n## Bundle modules\n\nA bundle module is a Gradle module that applies `flagon.bundle`. `examples/bundle` is a complete example.\n\n### 1. Add the module\n\nAdd the module to `settings.gradle.kts`, and add its path to `bundleProjects` in the root `build.gradle.kts` so `make bundles` copies its output into the site.\n\n```kotlin\n// your-bundle/build.gradle.kts\nplugins {\n    id("flagon.bundle")\n}\n\ndependencies {\n    implementation(project(":runtime"))\n    implementation(project(":platform:browser"))\n    implementation(project(":lwjgl:lwjgl2")) // or :lwjgl:lwjgl3, or both\n\n    implementation("com.example:your-app:1.0.0") {\n        exclude(group = "org.lwjgl")\n    }\n}\n\nbundle {\n    mainClass.set("com.example.BrowserMain")\n    catalogClass.set("com.example.BrowserCatalog")\n}\n```\n\nThe desktop LWJGL JARs must not be on the bundle\'s classpath. They hide Flagon\'s `org.lwjgl.*` classes and break the build.\n\nAdd the class libraries the application needs:\n\n| Module | Adds |\n| --- | --- |\n| `:toolchain:jdk-shims` | Reroutes JDK calls that TeaVM lacks. Add it to every application bundle. |\n| `:toolchain:jdk-classlib` | Java serialization, `java.lang.management`, `javax.xml.parsers`, `java.sql` dates and small stubs. |\n| `:toolchain:concurrent-classlib` | `java.util.concurrent` on green threads. |\n| `:toolchain:socket-classlib` | `java.net.Socket` and `InetAddress` through the socket proxy. |\n| `:toolchain:http-classlib` | `java.net.http.HttpClient` through the page\'s `fetch`. |\n| `:toolchain:nio-classlib` | `FileChannel`, `Selector` and the NIO channel types. |\n| `:toolchain:awt-classlib` | `Graphics2D`, `java.awt.geom`, `BufferedImage`, `ImageIO` and the clipboard. |\n| `:toolchain:crypto-classlib` | `MessageDigest`, `Mac`, `Cipher`, `Signature`, `KeyAgreement`, PBKDF2, RSA and EC keys. |\n| `:toolchain:sound-classlib` | `javax.sound` types that report no device. |\n| `:toolchain:reflect-policy` | `@Reflectable` and the reflection policy. |\n| `:platform:web` | JSO bindings for WebCodecs, WebTransport, WebRTC, Web Bluetooth and WebUSB, and an IndexedDB store. See the [API reference](api.md). |\n\n### 2. Write the entry point\n\nThe entry point registers each program and boots the runtime. Use method references, so the compiler sees the call graph.\n\n```java\npackage com.example;\n\nimport dev.flagon.platform.browser.BrowserPlatform;\nimport dev.flagon.runtime.FlagonRuntime;\nimport dev.flagon.runtime.MainProgram;\nimport dev.flagon.runtime.ProgramRegistry;\n\npublic final class BrowserMain {\n    public static void main(String[] arguments) {\n        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry());\n    }\n\n    public static ProgramRegistry registry() {\n        return new ProgramRegistry()\n                .register(new MainProgram("game", "the game", Game::main))\n                .register(new MainProgram("editor", "the level editor", Editor::main));\n    }\n}\n```\n\nThe page picks a program with `?program=`. With no program, the runtime runs the first one. The rest of the arguments go to the program.\n\n### 3. Write the catalog\n\nThe catalog lists the bundle\'s programs for the page. The build runs the catalog class on the JVM, never in the page.\n\n```java\npublic final class BrowserCatalog {\n    public static void main(String[] arguments) throws IOException {\n        ProgramCatalog.writeTo(BrowserMain.registry(), arguments[0]);\n    }\n}\n```\n\n### Settings\n\n| Setting | Default | Use |\n| --- | --- | --- |\n| `bundleName` | the module name without `bundle-` | The output file names. |\n| `mainClass` | none | The entry point. Required. |\n| `catalogClass` | none | Writes `<name>.catalog.json`. |\n| `resourcesFrom` | the module\'s resources | The folder the resource pack is made from. |\n| `reflectClasses` | empty | Globs of classes that keep their members. Same as `flagon.reflect.classes`. |\n| `reflectProxies` | empty | Globs of interfaces that `Proxy.newProxyInstance` implements. |\n| `reflectConverted` | empty | Classes that keep `valueOf(String)` and their `(String)` constructor, for option parsers. |\n| `compilerMemory` | 1024 | The TeaVM heap in MB. |\n| `vorbisQuality` | unset | Re-encodes Ogg Vorbis at this quality. Unset keeps the files. |\n| `apartFrom` | 0 | Resources this size in bytes or larger load beside the pack. 0 keeps all of them in it. |\n| `apart` | empty | Globs of resources that load beside the pack at any size. The app bundle reads `vendor/apart.txt`. |\n| `preserve` | `-Pflagon.preserve` | Globs of classes that only a classpath scan names. The app bundle reads `vendor/preserve.txt`. |\n| `wasmOpt` | `-Pflagon.wasmOpt`, then the `PATH` | The binaryen `wasm-opt` that release builds run. In this repository, the one in `web/node_modules`. |\n| `shrinkCoroutineWasm` | true | Runs `wasm-opt` on the coroutine WebAssembly in release builds. |\n| `wasiMainClass` | `mainClass` | The entry point of the [WASI component](#wasi). |\n\n### Gradle properties\n\n| Property | Use |\n| --- | --- |\n| `-Pflagon.release` | Release build: optimized, obfuscated, shrunk with `wasm-opt`. |\n| `-Pflagon.debug` | WebAssembly keeps its names and gets a source map and the Java sources. Skips `wasm-opt`. |\n| `-Pflagon.preserve=globs` | Classes that only a classpath scan names. Defaults to `vendor/preserve.txt` in the app bundle. |\n| `-Pflagon.app.reflect=globs` | Narrows the constructors `make app` keeps. |\n| `-Pflagon.wasmOpt=path` | The `wasm-opt` that release builds run. Defaults to the one on `PATH`. |\n| `-Pflagon.wasmTools=path` | The `wasm-tools` the `wasi` task runs. Defaults to the one on `PATH`. |\n| `-Pflagon.wasmtime=path` | The `wasmtime` that `wasiTest` runs. Defaults to the one on `PATH`. |\n\n## From your own project\n\nFlagon publishes its modules and the `dev.flagon.bundle` plugin to GitHub Packages. GitHub Packages needs a token with `read:packages`, also for public packages. Put it in `~/.gradle/gradle.properties` as `flagonUsername` and `flagonPassword`. `examples/standalone` is a complete project. `make standalone` builds it against the local Maven repository.\n\nRun Gradle on JDK 25. The patched TeaVM and the Flagon build tools are Java 25 classes, and they run in the Gradle JVM. Set `toolchainVersion=25` in `gradle/gradle-daemon-jvm.properties`.\n\n```kotlin\n// settings.gradle.kts\npluginManagement {\n    repositories {\n        maven("https://maven.pkg.github.com/KingJayan/flagon") {\n            name = "flagon"\n            credentials(PasswordCredentials::class)\n        }\n        gradlePluginPortal()\n    }\n}\n\ndependencyResolutionManagement {\n    repositories {\n        maven("https://maven.pkg.github.com/KingJayan/flagon") {\n            name = "flagon"\n            credentials(PasswordCredentials::class)\n        }\n        mavenCentral()\n    }\n}\n```\n\n```kotlin\n// build.gradle.kts\nimport dev.flagon.build.FlagonVersions\n\nplugins {\n    id("dev.flagon.bundle") version "x.y.z" // a flagon release\n}\n\ndependencies {\n    implementation("dev.flagon:runtime:${FlagonVersions.flagon}")\n    implementation("dev.flagon:platform-browser:${FlagonVersions.flagon}")\n    implementation("dev.flagon:lwjgl2-compat:${FlagonVersions.flagon}")\n}\n\nbundle {\n    mainClass.set("com.example.BrowserMain")\n    catalogClass.set("com.example.BrowserCatalog")\n}\n```\n\n`FlagonVersions.flagon` is the plugin\'s version, so the modules match it. A module published from `:toolchain:<name>` is `dev.flagon:<name>`, and one from `:platform:<name>` is `dev.flagon:platform-<name>`. `:lwjgl:lwjgl2` and `:lwjgl:lwjgl3` are `lwjgl2-compat` and `lwjgl3-compat`.\n\nThe entry point, the catalog and the settings are the same as in a [bundle module](#bundle-modules). The tasks are `bundle` and `bundleFast`.\n\n## Output\n\nEach bundle writes these files to `build/flagon/`. `make bundles` copies them to `web/public/flagon/`.\n\n| File | Contents |\n| --- | --- |\n| `wasm/<name>.wasm` | WebAssembly GC. Pauses with a coroutine rewrite. |\n| `jspi/<name>.wasm` | WebAssembly GC. Pauses with JSPI, so it is smaller and faster. |\n| `wasm/<name>.wasm.map`, `wasm/src/` | The source map and the Java sources it points to, in `-Pflagon.debug` builds. The same for `jspi/`. |\n| `js/<name>.js` | JavaScript, for browsers without WebAssembly GC. |\n| `<name>.pack` | The resource pack: every non-class file, gzipped. |\n| `<name>.files/` | Resources of `apartFrom` size or larger, or named in `vendor/apart.txt`, by SHA-256. |\n| `<name>.catalog.json` | The program list, when `catalogClass` is set. |\n\nServe all of them. The page picks the backend at load time.\n\n## WASI\n\nThe `wasi` task builds a bundle module as a WASI preview 2 component, with no JavaScript in it. It needs [wasm-tools](https://github.com/bytecodealliance/wasm-tools). `bundles/wasi` is a complete example, and `make wasi` builds it and runs its programs under wasmtime.\n\nThe entry point boots on `WasiPlatform` and exits with the status `boot` returns:\n\n```kotlin\ndependencies {\n    implementation(project(":runtime"))\n    implementation(project(":platform:wasi"))\n    implementation(project(":toolchain:jdk-shims"))\n}\n\nbundle {\n    mainClass.set("com.example.ToolMain")\n}\n```\n\n```java\npublic static void main(String[] arguments) {\n    System.exit(FlagonRuntime.boot(WasiPlatform.create(arguments), registry()));\n}\n```\n\n`./gradlew :your-bundle:wasi` writes `build/wasi/<name>.wasm`. Run it with the WebAssembly proposals it uses, and preopen a directory for `java.io`:\n\n```bash\nwasmtime run -W gc=y,function-references=y,exceptions=y --dir ./data::/ build/wasi/tool.wasm game --verbose\n```\n\nThe first argument picks the program, as `?program=` does in a page. A static method with `@Export` that takes and returns only primitives is a function of the component, which runs with no program started:\n\n```java\n@Export(name = "checksum")\npublic static long checksum(int seed) { ... }\n```\n\n```bash\nwasmtime run -W gc=y,function-references=y,exceptions=y --invoke \'checksum(7)\' build/wasi/tool.wasm\n```\n\nExport names are kebab case. An export that takes or returns an object fails the build. A bundle that reaches browser code also fails the build, which names the methods that reach it. See [WASI](compatibility.md#wasi) for what runs.\n\n## Deploy\n\nThe output is static. Any static host serves it. A program that opens sockets also needs the socket proxy.\n\n### Socket proxy\n\nA page cannot open TCP connections. Flagon sends each socket over a WebSocket to `/__flagon/tcp` on the page\'s origin. The proxy opens the TCP or TLS connection.\n\n`make dev` includes a proxy that reaches only loopback addresses. For production, serve the site and the proxy together:\n\n```bash\nmake build-site\ncd web && bun run proxy -- --root dist --host 0.0.0.0 --port 8080 \\\n  --token "$FLAGON_PROXY_TOKEN" \\\n  --allow game.example.com:25565\n```\n\nOpen the page with `?proxytoken=<token>`.\n\n| Option | Default | Use |\n| --- | --- | --- |\n| `--root` | `dist` | The folder to serve. |\n| `--host` | `127.0.0.1` | The bind address. A non-loopback address needs `--token`. |\n| `--port` | `8080` | The port. |\n| `--token` | none | The token each connection must send. |\n| `--allow` | `localhost:*`, `127.0.0.1:*`, `[::1]:*` | Destinations as `host:port`. `*` matches any port. Repeat or separate with commas. |\n| `--origin` | the page\'s own origin | Extra origins that may connect. |\n| `--allowed-host` | the bind address | Extra `Host` header values to accept. |\n| `--tls-ca` | none | Extra trusted CA certificates (PEM) for TLS destinations. |\n\n### Embedding\n\nA host page loads a bundle and sets `window.__flagonHost` to receive its events. See [Host bridge](architecture.md#host-bridge).\n\nThe `@Export` methods of a WebAssembly bundle are on `globalThis.__flagonExports` once it starts. They take and return numbers, with no JavaScript between, so `__flagonExports.add(40, 2)` calls Java directly.\n\nA JavaScript bundle is an ES module. It exports `main`, the static `@JSExport` methods of the main class, and each class with `@JSExport` members. `js/<name>.d.ts` declares these for TypeScript, with the JSO interfaces they name:\n\n```ts\nimport { greet } from \'./flagon/js/sandbox.js\';\n\ngreet(\'flagon\'); // "Hello, flagon!"\n```\n\nA method cannot have both `@Export` and `@JSExport`. The WebAssembly bundle would export its name twice and fail to load.\n\n## Stable API\n\nThese are stable for downstream use. Everything else may change.\n\n| Surface | Use |\n| --- | --- |\n| `org.lwjgl.*` | The LWJGL 2 and LWJGL 3 APIs Flagon supports. |\n| `FlagonRuntime.boot` | Starts the runtime. |\n| `Program`, `ProgramRegistry`, `MainProgram`, `ProgramCatalog` | Registers programs. |\n| `BrowserPlatform.create` | Makes the browser platform. |\n| `@Reflectable`, `flagon.reflect.*` | Controls what reflection keeps. |\n| `window.__flagonHost` | The host page bridge. |\n| `dev.flagon:*` | Published module coordinates. |\n| `dev.flagon.bundle` | The bundle plugin and its `bundle` settings. |\n',xe="# Getting started\n\nFlagon compiles desktop Java bytecode ahead of time into WebAssembly GC and JavaScript. It runs unmodified LWJGL 2 and LWJGL 3 programs in a browser tab. There is no JVM in the page.\n\n## Requirements\n\n- JDK 25 or later.\n- [Bun](https://bun.sh).\n- `ffmpeg`, only for a JAR that ships Ogg Vorbis audio.\n- A browser with WebAssembly GC: Chrome 119+, Firefox 120+ or Safari 18.2+. Other browsers use the JavaScript bundle.\n\n## Build and run\n\n```bash\nmake setup   # install web dependencies and Playwright browsers\nmake bundles # compile the bundles\nmake dev     # serve the site at http://127.0.0.1:5273\n```\n\nThe site has three pages:\n\n| Page | Path | Use |\n| --- | --- | --- |\n| Demos | `/` | Runs the sample bundles. On a local server it also lists every test program. |\n| Playground | `/playground/` | Compiles and runs Java source in the browser. |\n| Docs | `/docs/` | This documentation. |\n\n## Run your own JAR\n\n1. Put the application JAR and its runtime dependencies in `vendor/`.\n2. Run `make app`.\n3. Run `make dev` and open `/play/`.\n\nSee [Packaging](packaging.md) for the options.\n\n## Page parameters\n\nThe Demos page and `/play/` read these query parameters:\n\n| Parameter | Value | Default |\n| --- | --- | --- |\n| `bundle` | `sandbox`, `example` or `app` | `app` on `/play/`; on Demos, `sandbox` on a local host, else the first demo |\n| `backend` | `wasm` or `js` | `wasm` when the browser has WebAssembly GC |\n| `program` | a program id from the bundle's catalog | the first program |\n| `arg` | one program argument; repeat for more | none |\n| `proxytoken` | the token of a [socket proxy](packaging.md#socket-proxy) | none |\n| `view` | `all` shows every test program; `demos` shows only the demos (Demos page only) | `all` on a local or private host |\n\nThe `wasm` backend loads the JSPI build when the browser has `WebAssembly.Suspending`, and the coroutine build otherwise.\n\n## Make targets\n\n| Target | Does |\n| --- | --- |\n| `make setup` | Installs web dependencies and Playwright browsers. |\n| `make bundles` | Compiles every bundle. |\n| `make fast` | Compiles only the WebAssembly bundles, for quicker development builds. |\n| `make debug` | Compiles every bundle with WebAssembly source maps, so browser devtools step through the Java sources. |\n| `make bundles-release` | Compiles release bundles and checks their size budgets. |\n| `make app` | Compiles the JARs in `vendor/` into the `app` bundle. |\n| `make minecraft` | Fills `vendor/` from a local launcher install. Set `MINECRAFT` and `LAUNCHER` to change the version or install. |\n| `make playground` | Compiles javac and TeaVM to WebAssembly for the playground. |\n| `make dev` | Starts the development server on port 5273. |\n| `make serve` | Serves the built site with the socket proxy. |\n| `make build-site` | Builds the site for deployment. |\n| `make test` | Runs the JVM unit tests. |\n| `make browser` | Runs the Playwright tests in Chromium and Firefox. |\n| `make lint`, `make typecheck` | Runs the Java and web linters and the type checker. |\n| `make check` | Runs the CI checks. |\n| `make clean` | Removes build and test output. |\n\nRun `make help` for the full list.\n\n## Playground\n\nThe playground runs javac and TeaVM 0.17 in a Web Worker. No server compiles code. Limits:\n\n- 64 KB of source.\n- 10 seconds per run.\n- 2,000 lines of output.\n\nThe program runs in a second worker, and **Stop** ends it. The playground uses the stock TeaVM class library, not Flagon's, so LWJGL is not available there.\n\n## Next\n\n- [Packaging](packaging.md): build a bundle and ship it.\n- [Compatibility](compatibility.md): what works in a browser.\n- [Architecture](architecture.md): how the build and runtime fit together.\n- [API reference](api.md): the public classes.\n";function U(){return{async:!1,breaks:!1,extensions:null,gfm:!0,hooks:null,pedantic:!1,renderer:null,silent:!1,tokenizer:null,walkTokens:null}}var x=U();function oe(t){x=t}var R={exec:()=>null};function u(t,e=""){let n=typeof t=="string"?t:t.source;const s={replace:(a,i)=>{let r=typeof i=="string"?i:i.source;return r=r.replace(b.caret,"$1"),n=n.replace(a,r),s},getRegex:()=>new RegExp(n,e)};return s}var b={codeRemoveIndent:/^(?: {1,4}| {0,3}\t)/gm,outputLinkReplace:/\\([\[\]])/g,indentCodeCompensation:/^(\s+)(?:```)/,beginningSpace:/^\s+/,endingHash:/#$/,startingSpaceChar:/^ /,endingSpaceChar:/ $/,nonSpaceChar:/[^ ]/,newLineCharGlobal:/\n/g,tabCharGlobal:/\t/g,multipleSpaceGlobal:/\s+/g,blankLine:/^[ \t]*$/,doubleBlankLine:/\n[ \t]*\n[ \t]*$/,blockquoteStart:/^ {0,3}>/,blockquoteSetextReplace:/\n {0,3}((?:=+|-+) *)(?=\n|$)/g,blockquoteSetextReplace2:/^ {0,3}>[ \t]?/gm,listReplaceTabs:/^\t+/,listReplaceNesting:/^ {1,4}(?=( {4})*[^ ])/g,listIsTask:/^\[[ xX]\] /,listReplaceTask:/^\[[ xX]\] +/,anyLine:/\n.*\n/,hrefBrackets:/^<(.*)>$/,tableDelimiter:/[:|]/,tableAlignChars:/^\||\| *$/g,tableRowBlankLine:/\n[ \t]*$/,tableAlignRight:/^ *-+: *$/,tableAlignCenter:/^ *:-+: *$/,tableAlignLeft:/^ *:-+ *$/,startATag:/^<a /i,endATag:/^<\/a>/i,startPreScriptTag:/^<(pre|code|kbd|script)(\s|>)/i,endPreScriptTag:/^<\/(pre|code|kbd|script)(\s|>)/i,startAngleBracket:/^</,endAngleBracket:/>$/,pedanticHrefTitle:/^([^'"]*[^\s])\s+(['"])(.*)\2/,unicodeAlphaNumeric:/[\p{L}\p{N}]/u,escapeTest:/[&<>"']/,escapeReplace:/[&<>"']/g,escapeTestNoEncode:/[<>"']|&(?!(#\d{1,7}|#[Xx][a-fA-F0-9]{1,6}|\w+);)/,escapeReplaceNoEncode:/[<>"']|&(?!(#\d{1,7}|#[Xx][a-fA-F0-9]{1,6}|\w+);)/g,unescapeTest:/&(#(?:\d+)|(?:#x[0-9A-Fa-f]+)|(?:\w+));?/ig,caret:/(^|[^\[])\^/g,percentDecode:/%25/g,findPipe:/\|/g,splitPipe:/ \|/,slashPipe:/\\\|/g,carriageReturn:/\r\n|\r/g,spaceLine:/^ +$/gm,notSpaceStart:/^\S*/,endingNewline:/\n$/,listItemRegex:t=>new RegExp(`^( {0,3}${t})((?:[	 ][^\\n]*)?(?:\\n|$))`),nextBulletRegex:t=>new RegExp(`^ {0,${Math.min(3,t-1)}}(?:[*+-]|\\d{1,9}[.)])((?:[ 	][^\\n]*)?(?:\\n|$))`),hrRegex:t=>new RegExp(`^ {0,${Math.min(3,t-1)}}((?:- *){3,}|(?:_ *){3,}|(?:\\* *){3,})(?:\\n+|$)`),fencesBeginRegex:t=>new RegExp(`^ {0,${Math.min(3,t-1)}}(?:\`\`\`|~~~)`),headingBeginRegex:t=>new RegExp(`^ {0,${Math.min(3,t-1)}}#`),htmlBeginRegex:t=>new RegExp(`^ {0,${Math.min(3,t-1)}}<(?:[a-z].*>|!--)`,"i")},Ae=/^(?:[ \t]*(?:\n|$))+/,Te=/^((?: {4}| {0,3}\t)[^\n]+(?:\n(?:[ \t]*(?:\n|$))*)?)+/,Ce=/^ {0,3}(`{3,}(?=[^`\n]*(?:\n|$))|~{3,})([^\n]*)(?:\n|$)(?:|([\s\S]*?)(?:\n|$))(?: {0,3}\1[~`]* *(?=\n|$)|$)/,E=/^ {0,3}((?:-[\t ]*){3,}|(?:_[ \t]*){3,}|(?:\*[ \t]*){3,})(?:\n+|$)/,Re=/^ {0,3}(#{1,6})(?=\s|$)(.*)(?:\n+|$)/,J=/(?:[*+-]|\d{1,9}[.)])/,le=/^(?!bull |blockCode|fences|blockquote|heading|html|table)((?:.|\n(?!\s*?\n|bull |blockCode|fences|blockquote|heading|html|table))+?)\n {0,3}(=+|-+) *(?:\n+|$)/,ce=u(le).replace(/bull/g,J).replace(/blockCode/g,/(?: {4}| {0,3}\t)/).replace(/fences/g,/ {0,3}(?:`{3,}|~{3,})/).replace(/blockquote/g,/ {0,3}>/).replace(/heading/g,/ {0,3}#{1,6}/).replace(/html/g,/ {0,3}<[^\n>]+>\n/).replace(/\|table/g,"").getRegex(),Ee=u(le).replace(/bull/g,J).replace(/blockCode/g,/(?: {4}| {0,3}\t)/).replace(/fences/g,/ {0,3}(?:`{3,}|~{3,})/).replace(/blockquote/g,/ {0,3}>/).replace(/heading/g,/ {0,3}#{1,6}/).replace(/html/g,/ {0,3}<[^\n>]+>\n/).replace(/table/g,/ {0,3}\|?(?:[:\- ]*\|)+[\:\- ]*\n/).getRegex(),z=/^([^\n]+(?:\n(?!hr|heading|lheading|blockquote|fences|list|html|table| +\n)[^\n]+)*)/,Pe=/^[^\n]+/,H=/(?!\s*\])(?:\\.|[^\[\]\\])+/,Le=u(/^ {0,3}\[(label)\]: *(?:\n[ \t]*)?([^<\s][^\s]*|<.*?>)(?:(?: +(?:\n[ \t]*)?| *\n[ \t]*)(title))? *(?:\n+|$)/).replace("label",H).replace("title",/(?:"(?:\\"?|[^"\\])*"|'[^'\n]*(?:\n[^'\n]+)*\n?'|\([^()]*\))/).getRegex(),Ie=u(/^( {0,3}bull)([ \t][^\n]+?)?(?:\n|$)/).replace(/bull/g,J).getRegex(),O="address|article|aside|base|basefont|blockquote|body|caption|center|col|colgroup|dd|details|dialog|dir|div|dl|dt|fieldset|figcaption|figure|footer|form|frame|frameset|h[1-6]|head|header|hr|html|iframe|legend|li|link|main|menu|menuitem|meta|nav|noframes|ol|optgroup|option|p|param|search|section|summary|table|tbody|td|tfoot|th|thead|title|tr|track|ul",$=/<!--(?:-?>|[\s\S]*?(?:-->|$))/,Fe=u("^ {0,3}(?:<(script|pre|style|textarea)[\\s>][\\s\\S]*?(?:</\\1>[^\\n]*\\n+|$)|comment[^\\n]*(\\n+|$)|<\\?[\\s\\S]*?(?:\\?>\\n*|$)|<![A-Z][\\s\\S]*?(?:>\\n*|$)|<!\\[CDATA\\[[\\s\\S]*?(?:\\]\\]>\\n*|$)|</?(tag)(?: +|\\n|/?>)[\\s\\S]*?(?:(?:\\n[ 	]*)+\\n|$)|<(?!script|pre|style|textarea)([a-z][\\w-]*)(?:attribute)*? */?>(?=[ \\t]*(?:\\n|$))[\\s\\S]*?(?:(?:\\n[ 	]*)+\\n|$)|</(?!script|pre|style|textarea)[a-z][\\w-]*\\s*>(?=[ \\t]*(?:\\n|$))[\\s\\S]*?(?:(?:\\n[ 	]*)+\\n|$))","i").replace("comment",$).replace("tag",O).replace("attribute",/ +[a-zA-Z:_][\w.:-]*(?: *= *"[^"\n]*"| *= *'[^'\n]*'| *= *[^\s"'=<>`]+)?/).getRegex(),de=u(z).replace("hr",E).replace("heading"," {0,3}#{1,6}(?:\\s|$)").replace("|lheading","").replace("|table","").replace("blockquote"," {0,3}>").replace("fences"," {0,3}(?:`{3,}(?=[^`\\n]*\\n)|~{3,})[^\\n]*\\n").replace("list"," {0,3}(?:[*+-]|1[.)]) ").replace("html","</?(?:tag)(?: +|\\n|/?>)|<(?:script|pre|style|textarea|!--)").replace("tag",O).getRegex(),De=u(/^( {0,3}> ?(paragraph|[^\n]*)(?:\n|$))+/).replace("paragraph",de).getRegex(),V={blockquote:De,code:Te,def:Le,fences:Ce,heading:Re,hr:E,html:Fe,lheading:ce,list:Ie,newline:Ae,paragraph:de,table:R,text:Pe},ne=u("^ *([^\\n ].*)\\n {0,3}((?:\\| *)?:?-+:? *(?:\\| *:?-+:? *)*(?:\\| *)?)(?:\\n((?:(?! *\\n|hr|heading|blockquote|code|fences|list|html).*(?:\\n|$))*)\\n*|$)").replace("hr",E).replace("heading"," {0,3}#{1,6}(?:\\s|$)").replace("blockquote"," {0,3}>").replace("code","(?: {4}| {0,3}	)[^\\n]").replace("fences"," {0,3}(?:`{3,}(?=[^`\\n]*\\n)|~{3,})[^\\n]*\\n").replace("list"," {0,3}(?:[*+-]|1[.)]) ").replace("html","</?(?:tag)(?: +|\\n|/?>)|<(?:script|pre|style|textarea|!--)").replace("tag",O).getRegex(),Oe={...V,lheading:Ee,table:ne,paragraph:u(z).replace("hr",E).replace("heading"," {0,3}#{1,6}(?:\\s|$)").replace("|lheading","").replace("table",ne).replace("blockquote"," {0,3}>").replace("fences"," {0,3}(?:`{3,}(?=[^`\\n]*\\n)|~{3,})[^\\n]*\\n").replace("list"," {0,3}(?:[*+-]|1[.)]) ").replace("html","</?(?:tag)(?: +|\\n|/?>)|<(?:script|pre|style|textarea|!--)").replace("tag",O).getRegex()},Me={...V,html:u(`^ *(?:comment *(?:\\n|\\s*$)|<(tag)[\\s\\S]+?</\\1> *(?:\\n{2,}|\\s*$)|<tag(?:"[^"]*"|'[^']*'|\\s[^'"/>\\s]*)*?/?> *(?:\\n{2,}|\\s*$))`).replace("comment",$).replace(/tag/g,"(?!(?:a|em|strong|small|s|cite|q|dfn|abbr|data|time|code|var|samp|kbd|sub|sup|i|b|u|mark|ruby|rt|rp|bdi|bdo|span|br|wbr|ins|del|img)\\b)\\w+(?!:|[^\\w\\s@]*@)\\b").getRegex(),def:/^ *\[([^\]]+)\]: *<?([^\s>]+)>?(?: +(["(][^\n]+[")]))? *(?:\n+|$)/,heading:/^(#{1,6})(.*)(?:\n+|$)/,fences:R,lheading:/^(.+?)\n {0,3}(=+|-+) *(?:\n+|$)/,paragraph:u(z).replace("hr",E).replace("heading",` *#{1,6} *[^
]`).replace("lheading",ce).replace("|table","").replace("blockquote"," {0,3}>").replace("|fences","").replace("|list","").replace("|html","").replace("|tag","").getRegex()},je=/^\\([!"#$%&'()*+,\-./:;<=>?@\[\]\\^_`{|}~])/,Ne=/^(`+)([^`]|[^`][\s\S]*?[^`])\1(?!`)/,he=/^( {2,}|\\)\n(?!\s*$)/,_e=/^(`+|[^`])(?:(?= {2,}\n)|[\s\S]*?(?:(?=[\\<!\[`*_]|\b_|$)|[^ ](?= {2,}\n)))/,M=/[\p{P}\p{S}]/u,q=/[\s\p{P}\p{S}]/u,ue=/[^\s\p{P}\p{S}]/u,Be=u(/^((?![*_])punctSpace)/,"u").replace(/punctSpace/g,q).getRegex(),pe=/(?!~)[\p{P}\p{S}]/u,We=/(?!~)[\s\p{P}\p{S}]/u,Ge=/(?:[^\s\p{P}\p{S}]|~)/u,Ue=/\[[^[\]]*?\]\((?:\\.|[^\\\(\)]|\((?:\\.|[^\\\(\)])*\))*\)|`[^`]*?`|<[^<>]*?>/g,fe=/^(?:\*+(?:((?!\*)punct)|[^\s*]))|^_+(?:((?!_)punct)|([^\s_]))/,Je=u(fe,"u").replace(/punct/g,M).getRegex(),ze=u(fe,"u").replace(/punct/g,pe).getRegex(),me="^[^_*]*?__[^_*]*?\\*[^_*]*?(?=__)|[^*]+(?=[^*])|(?!\\*)punct(\\*+)(?=[\\s]|$)|notPunctSpace(\\*+)(?!\\*)(?=punctSpace|$)|(?!\\*)punctSpace(\\*+)(?=notPunctSpace)|[\\s](\\*+)(?!\\*)(?=punct)|(?!\\*)punct(\\*+)(?!\\*)(?=punct)|notPunctSpace(\\*+)(?=notPunctSpace)",He=u(me,"gu").replace(/notPunctSpace/g,ue).replace(/punctSpace/g,q).replace(/punct/g,M).getRegex(),$e=u(me,"gu").replace(/notPunctSpace/g,Ge).replace(/punctSpace/g,We).replace(/punct/g,pe).getRegex(),Ve=u("^[^_*]*?\\*\\*[^_*]*?_[^_*]*?(?=\\*\\*)|[^_]+(?=[^_])|(?!_)punct(_+)(?=[\\s]|$)|notPunctSpace(_+)(?!_)(?=punctSpace|$)|(?!_)punctSpace(_+)(?=notPunctSpace)|[\\s](_+)(?!_)(?=punct)|(?!_)punct(_+)(?!_)(?=punct)","gu").replace(/notPunctSpace/g,ue).replace(/punctSpace/g,q).replace(/punct/g,M).getRegex(),qe=u(/\\(punct)/,"gu").replace(/punct/g,M).getRegex(),Ke=u(/^<(scheme:[^\s\x00-\x1f<>]*|email)>/).replace("scheme",/[a-zA-Z][a-zA-Z0-9+.-]{1,31}/).replace("email",/[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+(@)[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+(?![-_])/).getRegex(),Xe=u($).replace("(?:-->|$)","-->").getRegex(),Ye=u("^comment|^</[a-zA-Z][\\w:-]*\\s*>|^<[a-zA-Z][\\w-]*(?:attribute)*?\\s*/?>|^<\\?[\\s\\S]*?\\?>|^<![a-zA-Z]+\\s[\\s\\S]*?>|^<!\\[CDATA\\[[\\s\\S]*?\\]\\]>").replace("comment",Xe).replace("attribute",/\s+[a-zA-Z:_][\w.:-]*(?:\s*=\s*"[^"]*"|\s*=\s*'[^']*'|\s*=\s*[^\s"'=<>`]+)?/).getRegex(),I=/(?:\[(?:\\.|[^\[\]\\])*\]|\\.|`[^`]*`|[^\[\]\\`])*?/,Ze=u(/^!?\[(label)\]\(\s*(href)(?:(?:[ \t]*(?:\n[ \t]*)?)(title))?\s*\)/).replace("label",I).replace("href",/<(?:\\.|[^\n<>\\])+>|[^ \t\n\x00-\x1f]*/).replace("title",/"(?:\\"?|[^"\\])*"|'(?:\\'?|[^'\\])*'|\((?:\\\)?|[^)\\])*\)/).getRegex(),ge=u(/^!?\[(label)\]\[(ref)\]/).replace("label",I).replace("ref",H).getRegex(),be=u(/^!?\[(ref)\](?:\[\])?/).replace("ref",H).getRegex(),Qe=u("reflink|nolink(?!\\()","g").replace("reflink",ge).replace("nolink",be).getRegex(),K={_backpedal:R,anyPunctuation:qe,autolink:Ke,blockSkip:Ue,br:he,code:Ne,del:R,emStrongLDelim:Je,emStrongRDelimAst:He,emStrongRDelimUnd:Ve,escape:je,link:Ze,nolink:be,punctuation:Be,reflink:ge,reflinkSearch:Qe,tag:Ye,text:_e,url:R},en={...K,link:u(/^!?\[(label)\]\((.*?)\)/).replace("label",I).getRegex(),reflink:u(/^!?\[(label)\]\s*\[([^\]]*)\]/).replace("label",I).getRegex()},B={...K,emStrongRDelimAst:$e,emStrongLDelim:ze,url:u(/^((?:ftp|https?):\/\/|www\.)(?:[a-zA-Z0-9\-]+\.?)+[^\s<]*|^email/,"i").replace("email",/[A-Za-z0-9._+-]+(@)[a-zA-Z0-9-_]+(?:\.[a-zA-Z0-9-_]*[a-zA-Z0-9])+(?![-_])/).getRegex(),_backpedal:/(?:[^?!.,:;*_'"~()&]+|\([^)]*\)|&(?![a-zA-Z0-9]+;$)|[?!.,:;*_'"~)]+(?!$))+/,del:/^(~~?)(?=[^\s~])((?:\\.|[^\\])*?(?:\\.|[^\s~\\]))\1(?=[^~]|$)/,text:/^([`~]+|[^`~])(?:(?= {2,}\n)|(?=[a-zA-Z0-9.!#$%&'*+\/=?_`{\|}~-]+@)|[\s\S]*?(?:(?=[\\<!\[`*~_]|\b_|https?:\/\/|ftp:\/\/|www\.|$)|[^ ](?= {2,}\n)|[^a-zA-Z0-9.!#$%&'*+\/=?_`{\|}~-](?=[a-zA-Z0-9.!#$%&'*+\/=?_`{\|}~-]+@)))/},nn={...B,br:u(he).replace("{2,}","*").getRegex(),text:u(B.text).replace("\\b_","\\b_| {2,}\\n").replace(/\{2,\}/g,"*").getRegex()},P={normal:V,gfm:Oe,pedantic:Me},T={normal:K,gfm:B,breaks:nn,pedantic:en},tn={"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"},te=t=>tn[t];function w(t,e){if(e){if(b.escapeTest.test(t))return t.replace(b.escapeReplace,te)}else if(b.escapeTestNoEncode.test(t))return t.replace(b.escapeReplaceNoEncode,te);return t}function ae(t){try{t=encodeURI(t).replace(b.percentDecode,"%")}catch{return null}return t}function se(t,e){const n=t.replace(b.findPipe,(i,r,o)=>{let c=!1,l=r;for(;--l>=0&&o[l]==="\\";)c=!c;return c?"|":" |"}),s=n.split(b.splitPipe);let a=0;if(s[0].trim()||s.shift(),s.length>0&&!s.at(-1)?.trim()&&s.pop(),e)if(s.length>e)s.splice(e);else for(;s.length<e;)s.push("");for(;a<s.length;a++)s[a]=s[a].trim().replace(b.slashPipe,"|");return s}function C(t,e,n){const s=t.length;if(s===0)return"";let a=0;for(;a<s&&t.charAt(s-a-1)===e;)a++;return t.slice(0,s-a)}function an(t,e){if(t.indexOf(e[1])===-1)return-1;let n=0;for(let s=0;s<t.length;s++)if(t[s]==="\\")s++;else if(t[s]===e[0])n++;else if(t[s]===e[1]&&(n--,n<0))return s;return n>0?-2:-1}function ie(t,e,n,s,a){const i=e.href,r=e.title||null,o=t[1].replace(a.other.outputLinkReplace,"$1");s.state.inLink=!0;const c={type:t[0].charAt(0)==="!"?"image":"link",raw:n,href:i,title:r,text:o,tokens:s.inlineTokens(o)};return s.state.inLink=!1,c}function sn(t,e,n){const s=t.match(n.other.indentCodeCompensation);if(s===null)return e;const a=s[1];return e.split(`
`).map(i=>{const r=i.match(n.other.beginningSpace);if(r===null)return i;const[o]=r;return o.length>=a.length?i.slice(a.length):i}).join(`
`)}var F=class{options;rules;lexer;constructor(t){this.options=t||x}space(t){const e=this.rules.block.newline.exec(t);if(e&&e[0].length>0)return{type:"space",raw:e[0]}}code(t){const e=this.rules.block.code.exec(t);if(e){const n=e[0].replace(this.rules.other.codeRemoveIndent,"");return{type:"code",raw:e[0],codeBlockStyle:"indented",text:this.options.pedantic?n:C(n,`
`)}}}fences(t){const e=this.rules.block.fences.exec(t);if(e){const n=e[0],s=sn(n,e[3]||"",this.rules);return{type:"code",raw:n,lang:e[2]?e[2].trim().replace(this.rules.inline.anyPunctuation,"$1"):e[2],text:s}}}heading(t){const e=this.rules.block.heading.exec(t);if(e){let n=e[2].trim();if(this.rules.other.endingHash.test(n)){const s=C(n,"#");(this.options.pedantic||!s||this.rules.other.endingSpaceChar.test(s))&&(n=s.trim())}return{type:"heading",raw:e[0],depth:e[1].length,text:n,tokens:this.lexer.inline(n)}}}hr(t){const e=this.rules.block.hr.exec(t);if(e)return{type:"hr",raw:C(e[0],`
`)}}blockquote(t){const e=this.rules.block.blockquote.exec(t);if(e){let n=C(e[0],`
`).split(`
`),s="",a="";const i=[];for(;n.length>0;){let r=!1;const o=[];let c;for(c=0;c<n.length;c++)if(this.rules.other.blockquoteStart.test(n[c]))o.push(n[c]),r=!0;else if(!r)o.push(n[c]);else break;n=n.slice(c);const l=o.join(`
`),d=l.replace(this.rules.other.blockquoteSetextReplace,`
    $1`).replace(this.rules.other.blockquoteSetextReplace2,"");s=s?`${s}
${l}`:l,a=a?`${a}
${d}`:d;const f=this.lexer.state.top;if(this.lexer.state.top=!0,this.lexer.blockTokens(d,i,!0),this.lexer.state.top=f,n.length===0)break;const h=i.at(-1);if(h?.type==="code")break;if(h?.type==="blockquote"){const g=h,m=g.raw+`
`+n.join(`
`),v=this.blockquote(m);i[i.length-1]=v,s=s.substring(0,s.length-g.raw.length)+v.raw,a=a.substring(0,a.length-g.text.length)+v.text;break}else if(h?.type==="list"){const g=h,m=g.raw+`
`+n.join(`
`),v=this.list(m);i[i.length-1]=v,s=s.substring(0,s.length-h.raw.length)+v.raw,a=a.substring(0,a.length-g.raw.length)+v.raw,n=m.substring(i.at(-1).raw.length).split(`
`);continue}}return{type:"blockquote",raw:s,tokens:i,text:a}}}list(t){let e=this.rules.block.list.exec(t);if(e){let n=e[1].trim();const s=n.length>1,a={type:"list",raw:"",ordered:s,start:s?+n.slice(0,-1):"",loose:!1,items:[]};n=s?`\\d{1,9}\\${n.slice(-1)}`:`\\${n}`,this.options.pedantic&&(n=s?n:"[*+-]");const i=this.rules.other.listItemRegex(n);let r=!1;for(;t;){let c=!1,l="",d="";if(!(e=i.exec(t))||this.rules.block.hr.test(t))break;l=e[0],t=t.substring(l.length);let f=e[2].split(`
`,1)[0].replace(this.rules.other.listReplaceTabs,j=>" ".repeat(3*j.length)),h=t.split(`
`,1)[0],g=!f.trim(),m=0;if(this.options.pedantic?(m=2,d=f.trimStart()):g?m=e[1].length+1:(m=e[2].search(this.rules.other.nonSpaceChar),m=m>4?1:m,d=f.slice(m),m+=e[1].length),g&&this.rules.other.blankLine.test(h)&&(l+=h+`
`,t=t.substring(h.length+1),c=!0),!c){const j=this.rules.other.nextBulletRegex(m),Z=this.rules.other.hrRegex(m),Q=this.rules.other.fencesBeginRegex(m),ee=this.rules.other.headingBeginRegex(m),ve=this.rules.other.htmlBeginRegex(m);for(;t;){const N=t.split(`
`,1)[0];let A;if(h=N,this.options.pedantic?(h=h.replace(this.rules.other.listReplaceNesting,"  "),A=h):A=h.replace(this.rules.other.tabCharGlobal,"    "),Q.test(h)||ee.test(h)||ve.test(h)||j.test(h)||Z.test(h))break;if(A.search(this.rules.other.nonSpaceChar)>=m||!h.trim())d+=`
`+A.slice(m);else{if(g||f.replace(this.rules.other.tabCharGlobal,"    ").search(this.rules.other.nonSpaceChar)>=4||Q.test(f)||ee.test(f)||Z.test(f))break;d+=`
`+h}!g&&!h.trim()&&(g=!0),l+=N+`
`,t=t.substring(N.length+1),f=A.slice(m)}}a.loose||(r?a.loose=!0:this.rules.other.doubleBlankLine.test(l)&&(r=!0));let v=null,Y;this.options.gfm&&(v=this.rules.other.listIsTask.exec(d),v&&(Y=v[0]!=="[ ] ",d=d.replace(this.rules.other.listReplaceTask,""))),a.items.push({type:"list_item",raw:l,task:!!v,checked:Y,loose:!1,text:d,tokens:[]}),a.raw+=l}const o=a.items.at(-1);if(o)o.raw=o.raw.trimEnd(),o.text=o.text.trimEnd();else return;a.raw=a.raw.trimEnd();for(let c=0;c<a.items.length;c++)if(this.lexer.state.top=!1,a.items[c].tokens=this.lexer.blockTokens(a.items[c].text,[]),!a.loose){const l=a.items[c].tokens.filter(f=>f.type==="space"),d=l.length>0&&l.some(f=>this.rules.other.anyLine.test(f.raw));a.loose=d}if(a.loose)for(let c=0;c<a.items.length;c++)a.items[c].loose=!0;return a}}html(t){const e=this.rules.block.html.exec(t);if(e)return{type:"html",block:!0,raw:e[0],pre:e[1]==="pre"||e[1]==="script"||e[1]==="style",text:e[0]}}def(t){const e=this.rules.block.def.exec(t);if(e){const n=e[1].toLowerCase().replace(this.rules.other.multipleSpaceGlobal," "),s=e[2]?e[2].replace(this.rules.other.hrefBrackets,"$1").replace(this.rules.inline.anyPunctuation,"$1"):"",a=e[3]?e[3].substring(1,e[3].length-1).replace(this.rules.inline.anyPunctuation,"$1"):e[3];return{type:"def",tag:n,raw:e[0],href:s,title:a}}}table(t){const e=this.rules.block.table.exec(t);if(!e||!this.rules.other.tableDelimiter.test(e[2]))return;const n=se(e[1]),s=e[2].replace(this.rules.other.tableAlignChars,"").split("|"),a=e[3]?.trim()?e[3].replace(this.rules.other.tableRowBlankLine,"").split(`
`):[],i={type:"table",raw:e[0],header:[],align:[],rows:[]};if(n.length===s.length){for(const r of s)this.rules.other.tableAlignRight.test(r)?i.align.push("right"):this.rules.other.tableAlignCenter.test(r)?i.align.push("center"):this.rules.other.tableAlignLeft.test(r)?i.align.push("left"):i.align.push(null);for(let r=0;r<n.length;r++)i.header.push({text:n[r],tokens:this.lexer.inline(n[r]),header:!0,align:i.align[r]});for(const r of a)i.rows.push(se(r,i.header.length).map((o,c)=>({text:o,tokens:this.lexer.inline(o),header:!1,align:i.align[c]})));return i}}lheading(t){const e=this.rules.block.lheading.exec(t);if(e)return{type:"heading",raw:e[0],depth:e[2].charAt(0)==="="?1:2,text:e[1],tokens:this.lexer.inline(e[1])}}paragraph(t){const e=this.rules.block.paragraph.exec(t);if(e){const n=e[1].charAt(e[1].length-1)===`
`?e[1].slice(0,-1):e[1];return{type:"paragraph",raw:e[0],text:n,tokens:this.lexer.inline(n)}}}text(t){const e=this.rules.block.text.exec(t);if(e)return{type:"text",raw:e[0],text:e[0],tokens:this.lexer.inline(e[0])}}escape(t){const e=this.rules.inline.escape.exec(t);if(e)return{type:"escape",raw:e[0],text:e[1]}}tag(t){const e=this.rules.inline.tag.exec(t);if(e)return!this.lexer.state.inLink&&this.rules.other.startATag.test(e[0])?this.lexer.state.inLink=!0:this.lexer.state.inLink&&this.rules.other.endATag.test(e[0])&&(this.lexer.state.inLink=!1),!this.lexer.state.inRawBlock&&this.rules.other.startPreScriptTag.test(e[0])?this.lexer.state.inRawBlock=!0:this.lexer.state.inRawBlock&&this.rules.other.endPreScriptTag.test(e[0])&&(this.lexer.state.inRawBlock=!1),{type:"html",raw:e[0],inLink:this.lexer.state.inLink,inRawBlock:this.lexer.state.inRawBlock,block:!1,text:e[0]}}link(t){const e=this.rules.inline.link.exec(t);if(e){const n=e[2].trim();if(!this.options.pedantic&&this.rules.other.startAngleBracket.test(n)){if(!this.rules.other.endAngleBracket.test(n))return;const i=C(n.slice(0,-1),"\\");if((n.length-i.length)%2===0)return}else{const i=an(e[2],"()");if(i===-2)return;if(i>-1){const o=(e[0].indexOf("!")===0?5:4)+e[1].length+i;e[2]=e[2].substring(0,i),e[0]=e[0].substring(0,o).trim(),e[3]=""}}let s=e[2],a="";if(this.options.pedantic){const i=this.rules.other.pedanticHrefTitle.exec(s);i&&(s=i[1],a=i[3])}else a=e[3]?e[3].slice(1,-1):"";return s=s.trim(),this.rules.other.startAngleBracket.test(s)&&(this.options.pedantic&&!this.rules.other.endAngleBracket.test(n)?s=s.slice(1):s=s.slice(1,-1)),ie(e,{href:s&&s.replace(this.rules.inline.anyPunctuation,"$1"),title:a&&a.replace(this.rules.inline.anyPunctuation,"$1")},e[0],this.lexer,this.rules)}}reflink(t,e){let n;if((n=this.rules.inline.reflink.exec(t))||(n=this.rules.inline.nolink.exec(t))){const s=(n[2]||n[1]).replace(this.rules.other.multipleSpaceGlobal," "),a=e[s.toLowerCase()];if(!a){const i=n[0].charAt(0);return{type:"text",raw:i,text:i}}return ie(n,a,n[0],this.lexer,this.rules)}}emStrong(t,e,n=""){let s=this.rules.inline.emStrongLDelim.exec(t);if(!s||s[3]&&n.match(this.rules.other.unicodeAlphaNumeric))return;if(!(s[1]||s[2]||"")||!n||this.rules.inline.punctuation.exec(n)){const i=[...s[0]].length-1;let r,o,c=i,l=0;const d=s[0][0]==="*"?this.rules.inline.emStrongRDelimAst:this.rules.inline.emStrongRDelimUnd;for(d.lastIndex=0,e=e.slice(-1*t.length+i);(s=d.exec(e))!=null;){if(r=s[1]||s[2]||s[3]||s[4]||s[5]||s[6],!r)continue;if(o=[...r].length,s[3]||s[4]){c+=o;continue}else if((s[5]||s[6])&&i%3&&!((i+o)%3)){l+=o;continue}if(c-=o,c>0)continue;o=Math.min(o,o+c+l);const f=[...s[0]][0].length,h=t.slice(0,i+s.index+f+o);if(Math.min(i,o)%2){const m=h.slice(1,-1);return{type:"em",raw:h,text:m,tokens:this.lexer.inlineTokens(m)}}const g=h.slice(2,-2);return{type:"strong",raw:h,text:g,tokens:this.lexer.inlineTokens(g)}}}}codespan(t){const e=this.rules.inline.code.exec(t);if(e){let n=e[2].replace(this.rules.other.newLineCharGlobal," ");const s=this.rules.other.nonSpaceChar.test(n),a=this.rules.other.startingSpaceChar.test(n)&&this.rules.other.endingSpaceChar.test(n);return s&&a&&(n=n.substring(1,n.length-1)),{type:"codespan",raw:e[0],text:n}}}br(t){const e=this.rules.inline.br.exec(t);if(e)return{type:"br",raw:e[0]}}del(t){const e=this.rules.inline.del.exec(t);if(e)return{type:"del",raw:e[0],text:e[2],tokens:this.lexer.inlineTokens(e[2])}}autolink(t){const e=this.rules.inline.autolink.exec(t);if(e){let n,s;return e[2]==="@"?(n=e[1],s="mailto:"+n):(n=e[1],s=n),{type:"link",raw:e[0],text:n,href:s,tokens:[{type:"text",raw:n,text:n}]}}}url(t){let e;if(e=this.rules.inline.url.exec(t)){let n,s;if(e[2]==="@")n=e[0],s="mailto:"+n;else{let a;do a=e[0],e[0]=this.rules.inline._backpedal.exec(e[0])?.[0]??"";while(a!==e[0]);n=e[0],e[1]==="www."?s="http://"+e[0]:s=e[0]}return{type:"link",raw:e[0],text:n,href:s,tokens:[{type:"text",raw:n,text:n}]}}}inlineText(t){const e=this.rules.inline.text.exec(t);if(e){const n=this.lexer.state.inRawBlock;return{type:"text",raw:e[0],text:e[0],escaped:n}}}},k=class W{tokens;options;state;tokenizer;inlineQueue;constructor(e){this.tokens=[],this.tokens.links=Object.create(null),this.options=e||x,this.options.tokenizer=this.options.tokenizer||new F,this.tokenizer=this.options.tokenizer,this.tokenizer.options=this.options,this.tokenizer.lexer=this,this.inlineQueue=[],this.state={inLink:!1,inRawBlock:!1,top:!0};const n={other:b,block:P.normal,inline:T.normal};this.options.pedantic?(n.block=P.pedantic,n.inline=T.pedantic):this.options.gfm&&(n.block=P.gfm,this.options.breaks?n.inline=T.breaks:n.inline=T.gfm),this.tokenizer.rules=n}static get rules(){return{block:P,inline:T}}static lex(e,n){return new W(n).lex(e)}static lexInline(e,n){return new W(n).inlineTokens(e)}lex(e){e=e.replace(b.carriageReturn,`
`),this.blockTokens(e,this.tokens);for(let n=0;n<this.inlineQueue.length;n++){const s=this.inlineQueue[n];this.inlineTokens(s.src,s.tokens)}return this.inlineQueue=[],this.tokens}blockTokens(e,n=[],s=!1){for(this.options.pedantic&&(e=e.replace(b.tabCharGlobal,"    ").replace(b.spaceLine,""));e;){let a;if(this.options.extensions?.block?.some(r=>(a=r.call({lexer:this},e,n))?(e=e.substring(a.raw.length),n.push(a),!0):!1))continue;if(a=this.tokenizer.space(e)){e=e.substring(a.raw.length);const r=n.at(-1);a.raw.length===1&&r!==void 0?r.raw+=`
`:n.push(a);continue}if(a=this.tokenizer.code(e)){e=e.substring(a.raw.length);const r=n.at(-1);r?.type==="paragraph"||r?.type==="text"?(r.raw+=`
`+a.raw,r.text+=`
`+a.text,this.inlineQueue.at(-1).src=r.text):n.push(a);continue}if(a=this.tokenizer.fences(e)){e=e.substring(a.raw.length),n.push(a);continue}if(a=this.tokenizer.heading(e)){e=e.substring(a.raw.length),n.push(a);continue}if(a=this.tokenizer.hr(e)){e=e.substring(a.raw.length),n.push(a);continue}if(a=this.tokenizer.blockquote(e)){e=e.substring(a.raw.length),n.push(a);continue}if(a=this.tokenizer.list(e)){e=e.substring(a.raw.length),n.push(a);continue}if(a=this.tokenizer.html(e)){e=e.substring(a.raw.length),n.push(a);continue}if(a=this.tokenizer.def(e)){e=e.substring(a.raw.length);const r=n.at(-1);r?.type==="paragraph"||r?.type==="text"?(r.raw+=`
`+a.raw,r.text+=`
`+a.raw,this.inlineQueue.at(-1).src=r.text):this.tokens.links[a.tag]||(this.tokens.links[a.tag]={href:a.href,title:a.title});continue}if(a=this.tokenizer.table(e)){e=e.substring(a.raw.length),n.push(a);continue}if(a=this.tokenizer.lheading(e)){e=e.substring(a.raw.length),n.push(a);continue}let i=e;if(this.options.extensions?.startBlock){let r=1/0;const o=e.slice(1);let c;this.options.extensions.startBlock.forEach(l=>{c=l.call({lexer:this},o),typeof c=="number"&&c>=0&&(r=Math.min(r,c))}),r<1/0&&r>=0&&(i=e.substring(0,r+1))}if(this.state.top&&(a=this.tokenizer.paragraph(i))){const r=n.at(-1);s&&r?.type==="paragraph"?(r.raw+=`
`+a.raw,r.text+=`
`+a.text,this.inlineQueue.pop(),this.inlineQueue.at(-1).src=r.text):n.push(a),s=i.length!==e.length,e=e.substring(a.raw.length);continue}if(a=this.tokenizer.text(e)){e=e.substring(a.raw.length);const r=n.at(-1);r?.type==="text"?(r.raw+=`
`+a.raw,r.text+=`
`+a.text,this.inlineQueue.pop(),this.inlineQueue.at(-1).src=r.text):n.push(a);continue}if(e){const r="Infinite loop on byte: "+e.charCodeAt(0);if(this.options.silent){console.error(r);break}else throw new Error(r)}}return this.state.top=!0,n}inline(e,n=[]){return this.inlineQueue.push({src:e,tokens:n}),n}inlineTokens(e,n=[]){let s=e,a=null;if(this.tokens.links){const o=Object.keys(this.tokens.links);if(o.length>0)for(;(a=this.tokenizer.rules.inline.reflinkSearch.exec(s))!=null;)o.includes(a[0].slice(a[0].lastIndexOf("[")+1,-1))&&(s=s.slice(0,a.index)+"["+"a".repeat(a[0].length-2)+"]"+s.slice(this.tokenizer.rules.inline.reflinkSearch.lastIndex))}for(;(a=this.tokenizer.rules.inline.anyPunctuation.exec(s))!=null;)s=s.slice(0,a.index)+"++"+s.slice(this.tokenizer.rules.inline.anyPunctuation.lastIndex);for(;(a=this.tokenizer.rules.inline.blockSkip.exec(s))!=null;)s=s.slice(0,a.index)+"["+"a".repeat(a[0].length-2)+"]"+s.slice(this.tokenizer.rules.inline.blockSkip.lastIndex);let i=!1,r="";for(;e;){i||(r=""),i=!1;let o;if(this.options.extensions?.inline?.some(l=>(o=l.call({lexer:this},e,n))?(e=e.substring(o.raw.length),n.push(o),!0):!1))continue;if(o=this.tokenizer.escape(e)){e=e.substring(o.raw.length),n.push(o);continue}if(o=this.tokenizer.tag(e)){e=e.substring(o.raw.length),n.push(o);continue}if(o=this.tokenizer.link(e)){e=e.substring(o.raw.length),n.push(o);continue}if(o=this.tokenizer.reflink(e,this.tokens.links)){e=e.substring(o.raw.length);const l=n.at(-1);o.type==="text"&&l?.type==="text"?(l.raw+=o.raw,l.text+=o.text):n.push(o);continue}if(o=this.tokenizer.emStrong(e,s,r)){e=e.substring(o.raw.length),n.push(o);continue}if(o=this.tokenizer.codespan(e)){e=e.substring(o.raw.length),n.push(o);continue}if(o=this.tokenizer.br(e)){e=e.substring(o.raw.length),n.push(o);continue}if(o=this.tokenizer.del(e)){e=e.substring(o.raw.length),n.push(o);continue}if(o=this.tokenizer.autolink(e)){e=e.substring(o.raw.length),n.push(o);continue}if(!this.state.inLink&&(o=this.tokenizer.url(e))){e=e.substring(o.raw.length),n.push(o);continue}let c=e;if(this.options.extensions?.startInline){let l=1/0;const d=e.slice(1);let f;this.options.extensions.startInline.forEach(h=>{f=h.call({lexer:this},d),typeof f=="number"&&f>=0&&(l=Math.min(l,f))}),l<1/0&&l>=0&&(c=e.substring(0,l+1))}if(o=this.tokenizer.inlineText(c)){e=e.substring(o.raw.length),o.raw.slice(-1)!=="_"&&(r=o.raw.slice(-1)),i=!0;const l=n.at(-1);l?.type==="text"?(l.raw+=o.raw,l.text+=o.text):n.push(o);continue}if(e){const l="Infinite loop on byte: "+e.charCodeAt(0);if(this.options.silent){console.error(l);break}else throw new Error(l)}}return n}},D=class{options;parser;constructor(t){this.options=t||x}space(t){return""}code({text:t,lang:e,escaped:n}){const s=(e||"").match(b.notSpaceStart)?.[0],a=t.replace(b.endingNewline,"")+`
`;return s?'<pre><code class="language-'+w(s)+'">'+(n?a:w(a,!0))+`</code></pre>
`:"<pre><code>"+(n?a:w(a,!0))+`</code></pre>
`}blockquote({tokens:t}){return`<blockquote>
${this.parser.parse(t)}</blockquote>
`}html({text:t}){return t}heading({tokens:t,depth:e}){return`<h${e}>${this.parser.parseInline(t)}</h${e}>
`}hr(t){return`<hr>
`}list(t){const e=t.ordered,n=t.start;let s="";for(let r=0;r<t.items.length;r++){const o=t.items[r];s+=this.listitem(o)}const a=e?"ol":"ul",i=e&&n!==1?' start="'+n+'"':"";return"<"+a+i+`>
`+s+"</"+a+`>
`}listitem(t){let e="";if(t.task){const n=this.checkbox({checked:!!t.checked});t.loose?t.tokens[0]?.type==="paragraph"?(t.tokens[0].text=n+" "+t.tokens[0].text,t.tokens[0].tokens&&t.tokens[0].tokens.length>0&&t.tokens[0].tokens[0].type==="text"&&(t.tokens[0].tokens[0].text=n+" "+w(t.tokens[0].tokens[0].text),t.tokens[0].tokens[0].escaped=!0)):t.tokens.unshift({type:"text",raw:n+" ",text:n+" ",escaped:!0}):e+=n+" "}return e+=this.parser.parse(t.tokens,!!t.loose),`<li>${e}</li>
`}checkbox({checked:t}){return"<input "+(t?'checked="" ':"")+'disabled="" type="checkbox">'}paragraph({tokens:t}){return`<p>${this.parser.parseInline(t)}</p>
`}table(t){let e="",n="";for(let a=0;a<t.header.length;a++)n+=this.tablecell(t.header[a]);e+=this.tablerow({text:n});let s="";for(let a=0;a<t.rows.length;a++){const i=t.rows[a];n="";for(let r=0;r<i.length;r++)n+=this.tablecell(i[r]);s+=this.tablerow({text:n})}return s&&(s=`<tbody>${s}</tbody>`),`<table>
<thead>
`+e+`</thead>
`+s+`</table>
`}tablerow({text:t}){return`<tr>
${t}</tr>
`}tablecell(t){const e=this.parser.parseInline(t.tokens),n=t.header?"th":"td";return(t.align?`<${n} align="${t.align}">`:`<${n}>`)+e+`</${n}>
`}strong({tokens:t}){return`<strong>${this.parser.parseInline(t)}</strong>`}em({tokens:t}){return`<em>${this.parser.parseInline(t)}</em>`}codespan({text:t}){return`<code>${w(t,!0)}</code>`}br(t){return"<br>"}del({tokens:t}){return`<del>${this.parser.parseInline(t)}</del>`}link({href:t,title:e,tokens:n}){const s=this.parser.parseInline(n),a=ae(t);if(a===null)return s;t=a;let i='<a href="'+t+'"';return e&&(i+=' title="'+w(e)+'"'),i+=">"+s+"</a>",i}image({href:t,title:e,text:n,tokens:s}){s&&(n=this.parser.parseInline(s,this.parser.textRenderer));const a=ae(t);if(a===null)return w(n);t=a;let i=`<img src="${t}" alt="${n}"`;return e&&(i+=` title="${w(e)}"`),i+=">",i}text(t){return"tokens"in t&&t.tokens?this.parser.parseInline(t.tokens):"escaped"in t&&t.escaped?t.text:w(t.text)}},X=class{strong({text:t}){return t}em({text:t}){return t}codespan({text:t}){return t}del({text:t}){return t}html({text:t}){return t}text({text:t}){return t}link({text:t}){return""+t}image({text:t}){return""+t}br(){return""}},S=class G{options;renderer;textRenderer;constructor(e){this.options=e||x,this.options.renderer=this.options.renderer||new D,this.renderer=this.options.renderer,this.renderer.options=this.options,this.renderer.parser=this,this.textRenderer=new X}static parse(e,n){return new G(n).parse(e)}static parseInline(e,n){return new G(n).parseInline(e)}parse(e,n=!0){let s="";for(let a=0;a<e.length;a++){const i=e[a];if(this.options.extensions?.renderers?.[i.type]){const o=i,c=this.options.extensions.renderers[o.type].call({parser:this},o);if(c!==!1||!["space","hr","heading","code","table","blockquote","list","html","paragraph","text"].includes(o.type)){s+=c||"";continue}}const r=i;switch(r.type){case"space":{s+=this.renderer.space(r);continue}case"hr":{s+=this.renderer.hr(r);continue}case"heading":{s+=this.renderer.heading(r);continue}case"code":{s+=this.renderer.code(r);continue}case"table":{s+=this.renderer.table(r);continue}case"blockquote":{s+=this.renderer.blockquote(r);continue}case"list":{s+=this.renderer.list(r);continue}case"html":{s+=this.renderer.html(r);continue}case"paragraph":{s+=this.renderer.paragraph(r);continue}case"text":{let o=r,c=this.renderer.text(o);for(;a+1<e.length&&e[a+1].type==="text";)o=e[++a],c+=`
`+this.renderer.text(o);n?s+=this.renderer.paragraph({type:"paragraph",raw:c,text:c,tokens:[{type:"text",raw:c,text:c,escaped:!0}]}):s+=c;continue}default:{const o='Token with "'+r.type+'" type was not found.';if(this.options.silent)return console.error(o),"";throw new Error(o)}}}return s}parseInline(e,n=this.renderer){let s="";for(let a=0;a<e.length;a++){const i=e[a];if(this.options.extensions?.renderers?.[i.type]){const o=this.options.extensions.renderers[i.type].call({parser:this},i);if(o!==!1||!["escape","html","link","image","strong","em","codespan","br","del","text"].includes(i.type)){s+=o||"";continue}}const r=i;switch(r.type){case"escape":{s+=n.text(r);break}case"html":{s+=n.html(r);break}case"link":{s+=n.link(r);break}case"image":{s+=n.image(r);break}case"strong":{s+=n.strong(r);break}case"em":{s+=n.em(r);break}case"codespan":{s+=n.codespan(r);break}case"br":{s+=n.br(r);break}case"del":{s+=n.del(r);break}case"text":{s+=n.text(r);break}default:{const o='Token with "'+r.type+'" type was not found.';if(this.options.silent)return console.error(o),"";throw new Error(o)}}}return s}},L=class{options;block;constructor(t){this.options=t||x}static passThroughHooks=new Set(["preprocess","postprocess","processAllTokens"]);preprocess(t){return t}postprocess(t){return t}processAllTokens(t){return t}provideLexer(){return this.block?k.lex:k.lexInline}provideParser(){return this.block?S.parse:S.parseInline}},rn=class{defaults=U();options=this.setOptions;parse=this.parseMarkdown(!0);parseInline=this.parseMarkdown(!1);Parser=S;Renderer=D;TextRenderer=X;Lexer=k;Tokenizer=F;Hooks=L;constructor(...t){this.use(...t)}walkTokens(t,e){let n=[];for(const s of t)switch(n=n.concat(e.call(this,s)),s.type){case"table":{const a=s;for(const i of a.header)n=n.concat(this.walkTokens(i.tokens,e));for(const i of a.rows)for(const r of i)n=n.concat(this.walkTokens(r.tokens,e));break}case"list":{const a=s;n=n.concat(this.walkTokens(a.items,e));break}default:{const a=s;this.defaults.extensions?.childTokens?.[a.type]?this.defaults.extensions.childTokens[a.type].forEach(i=>{const r=a[i].flat(1/0);n=n.concat(this.walkTokens(r,e))}):a.tokens&&(n=n.concat(this.walkTokens(a.tokens,e)))}}return n}use(...t){const e=this.defaults.extensions||{renderers:{},childTokens:{}};return t.forEach(n=>{const s={...n};if(s.async=this.defaults.async||s.async||!1,n.extensions&&(n.extensions.forEach(a=>{if(!a.name)throw new Error("extension name required");if("renderer"in a){const i=e.renderers[a.name];i?e.renderers[a.name]=function(...r){let o=a.renderer.apply(this,r);return o===!1&&(o=i.apply(this,r)),o}:e.renderers[a.name]=a.renderer}if("tokenizer"in a){if(!a.level||a.level!=="block"&&a.level!=="inline")throw new Error("extension level must be 'block' or 'inline'");const i=e[a.level];i?i.unshift(a.tokenizer):e[a.level]=[a.tokenizer],a.start&&(a.level==="block"?e.startBlock?e.startBlock.push(a.start):e.startBlock=[a.start]:a.level==="inline"&&(e.startInline?e.startInline.push(a.start):e.startInline=[a.start]))}"childTokens"in a&&a.childTokens&&(e.childTokens[a.name]=a.childTokens)}),s.extensions=e),n.renderer){const a=this.defaults.renderer||new D(this.defaults);for(const i in n.renderer){if(!(i in a))throw new Error(`renderer '${i}' does not exist`);if(["options","parser"].includes(i))continue;const r=i,o=n.renderer[r],c=a[r];a[r]=(...l)=>{let d=o.apply(a,l);return d===!1&&(d=c.apply(a,l)),d||""}}s.renderer=a}if(n.tokenizer){const a=this.defaults.tokenizer||new F(this.defaults);for(const i in n.tokenizer){if(!(i in a))throw new Error(`tokenizer '${i}' does not exist`);if(["options","rules","lexer"].includes(i))continue;const r=i,o=n.tokenizer[r],c=a[r];a[r]=(...l)=>{let d=o.apply(a,l);return d===!1&&(d=c.apply(a,l)),d}}s.tokenizer=a}if(n.hooks){const a=this.defaults.hooks||new L;for(const i in n.hooks){if(!(i in a))throw new Error(`hook '${i}' does not exist`);if(["options","block"].includes(i))continue;const r=i,o=n.hooks[r],c=a[r];L.passThroughHooks.has(i)?a[r]=l=>{if(this.defaults.async)return Promise.resolve(o.call(a,l)).then(f=>c.call(a,f));const d=o.call(a,l);return c.call(a,d)}:a[r]=(...l)=>{let d=o.apply(a,l);return d===!1&&(d=c.apply(a,l)),d}}s.hooks=a}if(n.walkTokens){const a=this.defaults.walkTokens,i=n.walkTokens;s.walkTokens=function(r){let o=[];return o.push(i.call(this,r)),a&&(o=o.concat(a.call(this,r))),o}}this.defaults={...this.defaults,...s}}),this}setOptions(t){return this.defaults={...this.defaults,...t},this}lexer(t,e){return k.lex(t,e??this.defaults)}parser(t,e){return S.parse(t,e??this.defaults)}parseMarkdown(t){return(n,s)=>{const a={...s},i={...this.defaults,...a},r=this.onError(!!i.silent,!!i.async);if(this.defaults.async===!0&&a.async===!1)return r(new Error("marked(): The async option was set to true by an extension. Remove async: false from the parse options object to return a Promise."));if(typeof n>"u"||n===null)return r(new Error("marked(): input parameter is undefined or null"));if(typeof n!="string")return r(new Error("marked(): input parameter is of type "+Object.prototype.toString.call(n)+", string expected"));i.hooks&&(i.hooks.options=i,i.hooks.block=t);const o=i.hooks?i.hooks.provideLexer():t?k.lex:k.lexInline,c=i.hooks?i.hooks.provideParser():t?S.parse:S.parseInline;if(i.async)return Promise.resolve(i.hooks?i.hooks.preprocess(n):n).then(l=>o(l,i)).then(l=>i.hooks?i.hooks.processAllTokens(l):l).then(l=>i.walkTokens?Promise.all(this.walkTokens(l,i.walkTokens)).then(()=>l):l).then(l=>c(l,i)).then(l=>i.hooks?i.hooks.postprocess(l):l).catch(r);try{i.hooks&&(n=i.hooks.preprocess(n));let l=o(n,i);i.hooks&&(l=i.hooks.processAllTokens(l)),i.walkTokens&&this.walkTokens(l,i.walkTokens);let d=c(l,i);return i.hooks&&(d=i.hooks.postprocess(d)),d}catch(l){return r(l)}}}onError(t,e){return n=>{if(n.message+=`
Please report this to https://github.com/markedjs/marked.`,t){const s="<p>An error occurred:</p><pre>"+w(n.message+"",!0)+"</pre>";return e?Promise.resolve(s):s}if(e)return Promise.reject(n);throw n}}},y=new rn;function p(t,e){return y.parse(t,e)}p.options=p.setOptions=function(t){return y.setOptions(t),p.defaults=y.defaults,oe(p.defaults),p};p.getDefaults=U;p.defaults=x;p.use=function(...t){return y.use(...t),p.defaults=y.defaults,oe(p.defaults),p};p.walkTokens=function(t,e){return y.walkTokens(t,e)};p.parseInline=y.parseInline;p.Parser=S;p.parser=S.parse;p.Renderer=D;p.TextRenderer=X;p.Lexer=k;p.lexer=k.lex;p.Tokenizer=F;p.Hooks=L;p.parse=p;p.options;p.setOptions;p.use;p.walkTokens;p.parseInline;S.parse;k.lex;const _=[{id:"start",title:"Getting started"},{id:"packaging",title:"Packaging"},{id:"compatibility",title:"Compatibility"},{id:"architecture",title:"Architecture"},{id:"api",title:"API reference"}],on=Object.assign({"../docs/api.md":we,"../docs/architecture.md":ke,"../docs/compatibility.md":Se,"../docs/packaging.md":ye,"../docs/start.md":xe});function ln(t){return t.toLowerCase().replace(/<[^>]+>/g,"").replace(/[^\p{L}\p{N}\s-]/gu,"").trim().replace(/\s+/g,"-")}function re(t,e=""){return`?page=${t}${e?`#${e}`:""}`}function cn(){const t=new URLSearchParams(location.search).get("page"),e=_.find(i=>i.id===t)??_[0],n=document.getElementById("doc");n.innerHTML=p.parse(on[`../docs/${e.id}.md`]??"",{async:!1}),document.title=`${e.title} · Flagon docs`;const s=new Map;for(const i of n.querySelectorAll("h1, h2, h3, h4")){const r=ln(i.textContent??""),o=s.get(r)??0;s.set(r,o+1),i.id=o?`${r}-${o}`:r}for(const i of n.querySelectorAll("a[href]")){const r=i.getAttribute("href").match(/^([\w-]+)\.md(?:#(.*))?$/);r&&(i.href=re(r[1],r[2]))}for(const i of n.querySelectorAll("table")){const r=document.createElement("div");r.className="table",i.replaceWith(r),r.append(i)}const a=document.getElementById("pages");for(const i of _){const r=document.createElement("li"),o=document.createElement("a");if(o.href=re(i.id),o.textContent=i.title,r.append(o),i===e){o.setAttribute("aria-current","page");const c=document.createElement("ul");for(const l of n.querySelectorAll("h2")){const d=document.createElement("li"),f=document.createElement("a");f.href=`#${l.id}`,f.textContent=l.textContent,d.append(f),c.append(d)}r.append(c)}a.append(r)}location.hash&&document.getElementById(decodeURIComponent(location.hash.slice(1)))?.scrollIntoView()}cn();
//# sourceMappingURL=docs-0i3UMuqs.js.map
