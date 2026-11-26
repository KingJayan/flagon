package java.io;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * java serialization, reading the stream format the jvm writes. classes of the application are
 * found by name, which the build allows for every serializable class.
 */
public class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {

    private final InputStream in;
    private final boolean enableOverride;
    private int peeked = -2;
    private boolean blkmode;
    /** bytes left in the current block, or -1 at the end of block data */
    private int unread;
    private boolean defaultDataEnd;
    private final List<Object> handles = new ArrayList<>();
    private int depth;
    private boolean enableResolve;
    private final List<Validation> validations = new ArrayList<>();

    private Object curObj;
    private ObjectStreamClass curDesc;
    private ObjectStreamClass curLocal;

    private record Validation(ObjectInputValidation callback, int priority) {
    }

    public ObjectInputStream(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        this.in = in;
        this.enableOverride = false;
        readStreamHeader();
        setBlockDataMode(true);
    }

    protected ObjectInputStream() throws IOException {
        this.in = null;
        this.enableOverride = true;
    }

    @Override
    public final Object readObject() throws IOException, ClassNotFoundException {
        if (enableOverride) {
            return readObjectOverride();
        }
        return readTop(false);
    }

    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return null;
    }

    public Object readUnshared() throws IOException, ClassNotFoundException {
        return readTop(true);
    }

    private Object readTop(boolean unshared) throws IOException, ClassNotFoundException {
        Object obj = readObject0(unshared);
        if (depth == 0 && !validations.isEmpty()) {
            List<Validation> pending = new ArrayList<>(validations);
            validations.clear();
            pending.sort((left, right) -> Integer.compare(right.priority(), left.priority()));
            for (Validation validation : pending) {
                validation.callback().validateObject();
            }
        }
        return obj;
    }

    public void defaultReadObject() throws IOException, ClassNotFoundException {
        if (curObj == null || curDesc == null) {
            throw new NotActiveException("not in call to readObject");
        }
        boolean old = setBlockDataMode(false);
        assignFields(curObj, curLocal, readValues(curDesc));
        setBlockDataMode(old);
        if (!curDesc.hasWriteObjectData()) {
            defaultDataEnd = true;
        }
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
        if (curObj == null || curDesc == null) {
            throw new NotActiveException("not in call to readObject");
        }
        boolean old = setBlockDataMode(false);
        Map<String, Object> values = readValues(curDesc);
        setBlockDataMode(old);
        if (!curDesc.hasWriteObjectData()) {
            defaultDataEnd = true;
        }
        return new GetFieldImpl(curDesc, values);
    }

    public void registerValidation(ObjectInputValidation obj, int prio)
            throws NotActiveException, InvalidObjectException {
        if (depth == 0) {
            throw new NotActiveException("stream inactive");
        }
        if (obj == null) {
            throw new InvalidObjectException("null callback");
        }
        validations.add(new Validation(obj, prio));
    }

    protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        return classNamed(desc.getName());
    }

    protected Class<?> resolveProxyClass(String[] interfaces)
            throws IOException, ClassNotFoundException {
        throw new ClassNotFoundException("proxy classes are not serialized here");
    }

    protected Object resolveObject(Object obj) throws IOException {
        return obj;
    }

    protected boolean enableResolveObject(boolean enable) throws SecurityException {
        boolean old = enableResolve;
        enableResolve = enable;
        return old;
    }

    protected void readStreamHeader() throws IOException, StreamCorruptedException {
        short magic = readShort();
        short version = readShort();
        if (magic != STREAM_MAGIC || version != STREAM_VERSION) {
            throw new StreamCorruptedException(String.format("invalid stream header: %04X%04X",
                    magic, version));
        }
    }

    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass desc = new ObjectStreamClass();
        desc.name = readUTF();
        desc.suid = readLong();
        desc.flags = readByte();
        int count = readShort();
        ObjectStreamField[] fields = new ObjectStreamField[count];
        for (int at = 0; at < count; at++) {
            char code = (char) readByte();
            String name = readUTF();
            String signature = code == 'L' || code == '[' ? readTypeString()
                    : String.valueOf(code);
            fields[at] = new ObjectStreamField(name, signature, false);
        }
        desc.fields = fields;
        return desc;
    }

    @Override
    public int read() throws IOException {
        if (!blkmode) {
            return readRaw();
        }
        if (!refill()) {
            return -1;
        }
        unread--;
        return readRaw();
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > buf.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        if (!blkmode) {
            return readRawInto(buf, off, len);
        }
        if (!refill()) {
            return -1;
        }
        int count = readRawInto(buf, off, Math.min(len, unread));
        if (count > 0) {
            unread -= count;
        }
        return count;
    }

    @Override
    public int available() throws IOException {
        return blkmode ? Math.max(unread, 0) : 0;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return readUnsignedByte() != 0;
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) readUnsignedByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        int value = read();
        if (value < 0) {
            throw new EOFException();
        }
        return value;
    }

    @Override
    public char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    @Override
    public short readShort() throws IOException {
        return (short) readUnsignedShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        int high = readUnsignedByte();
        return (high << 8) | readUnsignedByte();
    }

    @Override
    public int readInt() throws IOException {
        return (readUnsignedShort() << 16) | readUnsignedShort();
    }

    @Override
    public long readLong() throws IOException {
        return ((long) readInt() << 32) | (readInt() & 0xFFFFFFFFL);
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public void readFully(byte[] buf) throws IOException {
        readFully(buf, 0, buf.length);
    }

    @Override
    public void readFully(byte[] buf, int off, int len) throws IOException {
        while (len > 0) {
            int count = read(buf, off, len);
            if (count < 0) {
                throw new EOFException();
            }
            off += count;
            len -= count;
        }
    }

    @Override
    public int skipBytes(int len) throws IOException {
        int skipped = 0;
        while (skipped < len && read() >= 0) {
            skipped++;
        }
        return skipped;
    }

    @Override
    @Deprecated
    public String readLine() throws IOException {
        StringBuilder line = new StringBuilder();
        int value;
        while ((value = read()) >= 0 && value != '\n') {
            if (value != '\r') {
                line.append((char) value);
            }
        }
        return value < 0 && line.length() == 0 ? null : line.toString();
    }

    @Override
    public String readUTF() throws IOException {
        return readUtfBody(readUnsignedShort());
    }

    boolean setBlockDataMode(boolean mode) throws IOException {
        if (blkmode == mode) {
            return blkmode;
        }
        if (mode) {
            unread = 0;
        } else if (unread > 0) {
            throw new IllegalStateException("unread block data");
        }
        blkmode = mode;
        return !blkmode;
    }

    /** whether block data remains, reading the next block header when the last ran out */
    private boolean refill() throws IOException {
        while (unread == 0) {
            if (defaultDataEnd) {
                unread = -1;
                break;
            }
            int code = peekRaw();
            if (code == TC_BLOCKDATA) {
                readRaw();
                unread = readRaw();
            } else if (code == TC_BLOCKDATALONG) {
                readRaw();
                unread = rawInt();
            } else if (code == TC_RESET) {
                readRaw();
                handles.clear();
            } else {
                if (code >= 0 && (code < TC_BASE || code > TC_MAX)) {
                    throw new StreamCorruptedException(String.format("invalid type code: %02X",
                            code));
                }
                unread = -1;
            }
        }
        return unread > 0;
    }

    private Object readObject0(boolean unshared) throws IOException, ClassNotFoundException {
        boolean oldMode = blkmode;
        if (oldMode) {
            if (unread > 0) {
                throw new OptionalDataException(unread);
            } else if (defaultDataEnd) {
                throw new OptionalDataException(true);
            }
            if (unread == 0 && refill()) {
                throw new OptionalDataException(unread);
            }
            unread = 0;
            blkmode = false;
        }
        int code;
        while ((code = peekRaw()) == TC_RESET) {
            readRaw();
            handles.clear();
        }
        depth++;
        try {
            return switch (code) {
                case TC_NULL -> {
                    readRaw();
                    yield null;
                }
                case TC_REFERENCE -> readHandle();
                case TC_CLASS -> {
                    readRaw();
                    ObjectStreamClass desc = readClassDesc();
                    Class<?> cl = desc == null ? null : desc.cl;
                    handles.add(cl);
                    yield cl;
                }
                case TC_CLASSDESC, TC_PROXYCLASSDESC -> readClassDesc();
                case TC_STRING, TC_LONGSTRING -> checkResolve(readString());
                case TC_ARRAY -> checkResolve(readArray());
                case TC_ENUM -> checkResolve(readEnum());
                case TC_OBJECT -> checkResolve(readOrdinaryObject());
                case TC_EXCEPTION -> {
                    readRaw();
                    handles.clear();
                    Object thrown = readObject0(false);
                    handles.clear();
                    throw new WriteAbortedException("writing aborted", (Exception) thrown);
                }
                case TC_BLOCKDATA, TC_BLOCKDATALONG -> {
                    if (oldMode) {
                        blkmode = true;
                        refill();
                        throw new OptionalDataException(unread);
                    }
                    throw new StreamCorruptedException("unexpected block data");
                }
                case TC_ENDBLOCKDATA -> {
                    if (oldMode) {
                        throw new OptionalDataException(true);
                    }
                    throw new StreamCorruptedException("unexpected end of block data");
                }
                case -1 -> throw new EOFException();
                default -> throw new StreamCorruptedException(String.format(
                        "invalid type code: %02X", code));
            };
        } finally {
            depth--;
            if (oldMode) {
                blkmode = true;
                unread = 0;
            }
        }
    }

    private Object checkResolve(Object obj) throws IOException {
        if (!enableResolve) {
            return obj;
        }
        Object resolved = resolveObject(obj);
        if (resolved != obj) {
            int at = handles.lastIndexOf(obj);
            if (at >= 0) {
                handles.set(at, resolved);
            }
        }
        return resolved;
    }

    private Object readHandle() throws IOException {
        readRaw();
        int handle = rawInt() - baseWireHandle;
        if (handle < 0 || handle >= handles.size()) {
            throw new StreamCorruptedException(String.format("invalid handle value: %08X",
                    handle + baseWireHandle));
        }
        return handles.get(handle);
    }

    private ObjectStreamClass readClassDesc() throws IOException, ClassNotFoundException {
        int code = peekRaw();
        if (code == TC_NULL) {
            readRaw();
            return null;
        } else if (code == TC_REFERENCE) {
            return (ObjectStreamClass) readHandle();
        } else if (code == TC_PROXYCLASSDESC) {
            throw new InvalidClassException("proxy classes are not serialized here");
        } else if (code != TC_CLASSDESC) {
            throw new StreamCorruptedException(String.format("invalid type code: %02X", code));
        }
        readRaw();
        int handle = reserve();
        ObjectStreamClass desc = readClassDescriptor();
        Class<?> cl = null;
        ClassNotFoundException missing = null;
        try {
            cl = resolveClass(desc);
        } catch (ClassNotFoundException absent) {
            missing = absent;
        }
        skipCustomData();
        desc.superDesc = readClassDesc();
        handles.set(handle, desc);
        if (missing != null) {
            throw missing;
        }
        link(desc, cl);
        return desc;
    }

    /** joins a descriptor from the stream to the class here, and checks they agree */
    private void link(ObjectStreamClass desc, Class<?> cl) throws InvalidClassException {
        desc.cl = cl;
        desc.form = JdkForms.levelNamed(desc.name);
        if (cl == null || desc.form != null) {
            return;
        }
        ObjectStreamClass local = ObjectStreamClass.lookupAny(cl);
        boolean enumerated = (desc.flags & SC_ENUM) != 0;
        if (enumerated != local.isEnum) {
            throw new InvalidClassException(desc.name, enumerated
                    ? "cannot bind enum descriptor to a non-enum class"
                    : "cannot bind non-enum descriptor to an enum class");
        }
        if (!cl.isArray() && !enumerated && desc.suid != local.suid) {
            throw new InvalidClassException(desc.name, "local class incompatible: "
                    + "stream classdesc serialVersionUID = " + desc.suid
                    + ", local class serialVersionUID = " + local.suid);
        }
        desc.serializable = local.serializable;
        desc.externalizable = local.externalizable;
        desc.isEnum = local.isEnum;
        desc.isRecord = local.isRecord;
        desc.constructor = local.constructor;
        desc.readObjectMethod = local.readObjectMethod;
        desc.readObjectNoDataMethod = local.readObjectNoDataMethod;
        desc.readResolveMethod = local.readResolveMethod;
    }

    private String readTypeString() throws IOException, ClassNotFoundException {
        int code = peekRaw();
        if (code == TC_NULL) {
            readRaw();
            return null;
        } else if (code == TC_REFERENCE) {
            return (String) readHandle();
        }
        return readString();
    }

    private String readString() throws IOException {
        int code = readRaw();
        String text = code == TC_LONGSTRING ? readUtfBody(rawLong()) : readUtfBody(rawShort());
        handles.add(text);
        return text;
    }

    private Object readArray() throws IOException, ClassNotFoundException {
        readRaw();
        ObjectStreamClass desc = readClassDesc();
        int length = rawInt();
        Class<?> item = desc.cl.getComponentType();
        Object array = Array.newInstance(item, length);
        handles.add(array);
        if (item.isPrimitive()) {
            for (int at = 0; at < length; at++) {
                Array.set(array, at, readPrimitive(ObjectStreamField.signatureOf(item).charAt(0)));
            }
        } else {
            Object[] items = (Object[]) array;
            for (int at = 0; at < length; at++) {
                items[at] = readObject0(false);
            }
        }
        return array;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object readEnum() throws IOException, ClassNotFoundException {
        readRaw();
        ObjectStreamClass desc = readClassDesc();
        int handle = reserve();
        String name = (String) readObject0(false);
        Object constant = Enum.valueOf((Class) desc.cl, name);
        handles.set(handle, constant);
        return constant;
    }

    private Object readOrdinaryObject() throws IOException, ClassNotFoundException {
        readRaw();
        ObjectStreamClass desc = readClassDesc();
        if (desc.form != null) {
            return desc.form.read(this, desc);
        }
        if (desc.isRecord) {
            int handle = reserve();
            Object made = readRecord(desc);
            handles.set(handle, made);
            return resolved(desc, made, handle);
        }
        if (desc.constructor == null) {
            throw new InvalidClassException(desc.name, desc.externalizable
                    ? "no public no-arg constructor" : desc.serializable
                    ? "no valid constructor" : "class is not serializable");
        }
        Object obj = construct(desc);
        int handle = handles.size();
        handles.add(obj);
        if (desc.externalizable) {
            readExternalData((Externalizable) obj, desc);
        } else {
            readSerialData(obj, desc);
        }
        return resolved(desc, obj, handle);
    }

    private Object resolved(ObjectStreamClass desc, Object obj, int handle) throws IOException {
        if (desc.readResolveMethod == null) {
            return obj;
        }
        Object replaced = desc.invoke(desc.readResolveMethod, obj);
        handles.set(handle, replaced);
        return replaced;
    }

    private Object construct(ObjectStreamClass desc) throws IOException {
        try {
            desc.constructor.setAccessible(true);
            return desc.externalizable ? desc.constructor.newInstance()
                    : desc.constructor.newInstance((Object) null);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException
                 | IllegalArgumentException failed) {
            throw new InvalidClassException(desc.name, "cannot construct: " + failed);
        }
    }

    private Object readRecord(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        Map<String, Object> values = new HashMap<>();
        for (ObjectStreamClass level : desc.hierarchy()) {
            values.putAll(readValues(level));
            if (level.hasWriteObjectData()) {
                skipCustomData();
            }
        }
        java.lang.reflect.RecordComponent[] components = java.lang.reflect.RecordComponent.of(
                desc.cl);
        Object[] arguments = new Object[components.length];
        for (int at = 0; at < components.length; at++) {
            Object value = values.get(components[at].getName());
            Class<?> type = components[at].getType();
            arguments[at] = value == null && type.isPrimitive()
                    ? ObjectOutputStream.zero(ObjectStreamField.signatureOf(type).charAt(0))
                    : value;
        }
        try {
            return desc.constructor.newInstance(arguments);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException
                 | IllegalArgumentException failed) {
            throw new InvalidObjectException(desc.name + ": " + failed);
        }
    }

    private void readExternalData(Externalizable obj, ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        Object savedObj = curObj;
        ObjectStreamClass savedDesc = curDesc;
        ObjectStreamClass savedLocal = curLocal;
        curObj = obj;
        curDesc = null;
        curLocal = null;
        boolean blocked = desc.hasBlockExternalData();
        if (blocked) {
            setBlockDataMode(true);
        }
        obj.readExternal(this);
        if (blocked) {
            skipCustomData();
        }
        curObj = savedObj;
        curDesc = savedDesc;
        curLocal = savedLocal;
    }

    /** each class's data, topmost first, matched to the classes here by name */
    void readSerialData(Object obj, ObjectStreamClass desc) throws IOException,
            ClassNotFoundException {
        List<ObjectStreamClass> local = new ArrayList<>();
        for (Class<?> at = obj.getClass(); at != null; at = at.getSuperclass()) {
            ObjectStreamClass here = ObjectStreamClass.lookupAny(at);
            // a jdk class with a form counts, though its browser classlib may not say serializable
            if (here.form == null && !Serializable.class.isAssignableFrom(at)) {
                break;
            }
            local.add(0, here);
        }
        List<ObjectStreamClass> streamed = desc.hierarchy();
        for (ObjectStreamClass here : local) {
            ObjectStreamClass level = null;
            for (ObjectStreamClass candidate : streamed) {
                if (candidate.name.equals(here.name)) {
                    level = candidate;
                }
            }
            if (level == null) {
                if (here.readObjectNoDataMethod != null) {
                    here.invoke(here.readObjectNoDataMethod, obj);
                }
                continue;
            }
            readLevel(obj, level, here);
        }
        for (ObjectStreamClass level : streamed) {
            boolean matched = false;
            for (ObjectStreamClass here : local) {
                matched |= here.name.equals(level.name);
            }
            if (!matched) {
                readValues(level);
                if (level.hasWriteObjectData()) {
                    skipCustomData();
                }
            }
        }
    }

    private void readLevel(Object obj, ObjectStreamClass level, ObjectStreamClass here)
            throws IOException, ClassNotFoundException {
        if (level.form != null) {
            if (level.hasWriteObjectData()) {
                setBlockDataMode(true);
                level.form.fill(this, level, obj);
                skipCustomData();
            } else {
                level.form.fill(this, level, obj);
            }
            return;
        }
        if (here.readObjectMethod != null) {
            Object savedObj = curObj;
            ObjectStreamClass savedDesc = curDesc;
            ObjectStreamClass savedLocal = curLocal;
            curObj = obj;
            curDesc = level;
            curLocal = here;
            setBlockDataMode(true);
            here.invoke(here.readObjectMethod, obj, this);
            defaultDataEnd = false;
            curObj = savedObj;
            curDesc = savedDesc;
            curLocal = savedLocal;
            if (level.hasWriteObjectData()) {
                skipCustomData();
            } else {
                setBlockDataMode(false);
            }
            return;
        }
        assignFields(obj, here, readValues(level));
        if (level.hasWriteObjectData()) {
            skipCustomData();
        }
    }

    /** a level's field values by name: the primitives, then each object, in stream order */
    Map<String, Object> readValues(ObjectStreamClass level)
            throws IOException, ClassNotFoundException {
        boolean old = setBlockDataMode(false);
        Map<String, Object> values = new HashMap<>();
        for (ObjectStreamField field : level.fields) {
            if (field.isPrimitive()) {
                values.put(field.getName(), readPrimitive(field.getTypeCode()));
            }
        }
        for (ObjectStreamField field : level.fields) {
            if (!field.isPrimitive()) {
                values.put(field.getName(), readObject0(field.isUnshared()));
            }
        }
        setBlockDataMode(old);
        return values;
    }

    private void assignFields(Object obj, ObjectStreamClass here, Map<String, Object> values)
            throws InvalidClassException {
        if (here == null || here.slots == null) {
            return;
        }
        for (int at = 0; at < here.fields.length; at++) {
            Field slot = here.slots[at];
            String name = here.fields[at].getName();
            if (slot == null || !values.containsKey(name)) {
                continue;
            }
            Object value = values.get(name);
            try {
                if (value != null || !slot.getType().isPrimitive()) {
                    slot.set(obj, value);
                }
            } catch (IllegalAccessException | IllegalArgumentException mismatch) {
                throw new InvalidClassException(here.name, "incompatible types for field "
                        + name);
            }
        }
    }

    /** reads to the end of a class's custom data, passing over its blocks and objects */
    void skipCustomData() throws IOException, ClassNotFoundException {
        while (true) {
            if (blkmode) {
                while (refill()) {
                    skipBytes(unread);
                }
                blkmode = false;
                unread = 0;
            }
            int code = peekRaw();
            if (code == TC_BLOCKDATA || code == TC_BLOCKDATALONG) {
                blkmode = true;
                unread = 0;
            } else if (code == TC_ENDBLOCKDATA) {
                readRaw();
                defaultDataEnd = false;
                return;
            } else {
                readObject0(false);
            }
        }
    }

    /** a place for an object that is read before it is made */
    int reserve() {
        handles.add(null);
        return handles.size() - 1;
    }

    void fill(int handle, Object obj) {
        handles.set(handle, obj);
    }

    Object readNested() throws IOException, ClassNotFoundException {
        return readObject0(false);
    }

    private Object readPrimitive(char code) throws IOException {
        return switch (code) {
            case 'Z' -> readBoolean();
            case 'B' -> readByte();
            case 'C' -> readChar();
            case 'S' -> readShort();
            case 'I' -> readInt();
            case 'J' -> readLong();
            case 'F' -> readFloat();
            case 'D' -> readDouble();
            default -> throw new InvalidClassException("illegal field type code " + code);
        };
    }

    static Class<?> classNamed(String name) throws ClassNotFoundException {
        Class<?> jdk = JdkForms.classOf(name);
        if (jdk != null) {
            return jdk;
        }
        if (name.startsWith("[")) {
            return arrayNamed(name);
        }
        return Class.forName(name);
    }

    private static Class<?> arrayNamed(String name) throws ClassNotFoundException {
        String item = name.substring(1);
        Class<?> component = switch (item.charAt(0)) {
            case 'Z' -> boolean.class;
            case 'B' -> byte.class;
            case 'C' -> char.class;
            case 'S' -> short.class;
            case 'I' -> int.class;
            case 'J' -> long.class;
            case 'F' -> float.class;
            case 'D' -> double.class;
            case '[' -> arrayNamed(item);
            case 'L' -> classNamed(item.substring(1, item.length() - 1));
            default -> throw new ClassNotFoundException(name);
        };
        return Array.newInstance(component, 0).getClass();
    }

    private int peekRaw() throws IOException {
        if (peeked == -2) {
            peeked = in.read();
        }
        return peeked;
    }

    private int readRaw() throws IOException {
        int value;
        if (peeked != -2) {
            value = peeked;
            peeked = -2;
        } else {
            value = in.read();
        }
        if (value < 0) {
            throw new EOFException();
        }
        return value;
    }

    private int readRawInto(byte[] buf, int off, int len) throws IOException {
        if (peeked != -2) {
            if (peeked < 0) {
                return -1;
            }
            buf[off] = (byte) readRaw();
            return 1;
        }
        return in.read(buf, off, len);
    }

    private int rawShort() throws IOException {
        return (readRaw() << 8) | readRaw();
    }

    private int rawInt() throws IOException {
        return (rawShort() << 16) | rawShort();
    }

    private long rawLong() throws IOException {
        return ((long) rawInt() << 32) | (rawInt() & 0xFFFFFFFFL);
    }

    private String readUtfBody(long length) throws IOException {
        StringBuilder text = new StringBuilder();
        long left = length;
        while (left > 0) {
            int first = readUnsignedByteAny();
            left--;
            if (first < 0x80) {
                text.append((char) first);
            } else if ((first & 0xE0) == 0xC0) {
                int second = readUnsignedByteAny();
                left--;
                text.append((char) (((first & 0x1F) << 6) | (second & 0x3F)));
            } else if ((first & 0xF0) == 0xE0) {
                int second = readUnsignedByteAny();
                int third = readUnsignedByteAny();
                left -= 2;
                text.append((char) (((first & 0x0F) << 12) | ((second & 0x3F) << 6)
                        | (third & 0x3F)));
            } else {
                throw new UTFDataFormatException();
            }
        }
        return text.toString();
    }

    private int readUnsignedByteAny() throws IOException {
        return blkmode ? readUnsignedByte() : readRaw();
    }

    public abstract static class GetField {

        public GetField() {
        }

        public abstract ObjectStreamClass getObjectStreamClass();

        public abstract boolean defaulted(String name) throws IOException;

        public abstract boolean get(String name, boolean val) throws IOException;

        public abstract byte get(String name, byte val) throws IOException;

        public abstract char get(String name, char val) throws IOException;

        public abstract short get(String name, short val) throws IOException;

        public abstract int get(String name, int val) throws IOException;

        public abstract long get(String name, long val) throws IOException;

        public abstract float get(String name, float val) throws IOException;

        public abstract double get(String name, double val) throws IOException;

        public abstract Object get(String name, Object val) throws IOException;
    }

    private static final class GetFieldImpl extends GetField {

        private final ObjectStreamClass desc;
        private final Map<String, Object> values;

        GetFieldImpl(ObjectStreamClass desc, Map<String, Object> values) {
            this.desc = desc;
            this.values = values;
        }

        private Object value(String name, Object otherwise) {
            return values.containsKey(name) ? values.get(name) : otherwise;
        }

        @Override
        public ObjectStreamClass getObjectStreamClass() {
            return desc;
        }

        @Override
        public boolean defaulted(String name) {
            return !values.containsKey(name);
        }

        @Override
        public boolean get(String name, boolean val) {
            return (Boolean) value(name, val);
        }

        @Override
        public byte get(String name, byte val) {
            return (Byte) value(name, val);
        }

        @Override
        public char get(String name, char val) {
            return (Character) value(name, val);
        }

        @Override
        public short get(String name, short val) {
            return (Short) value(name, val);
        }

        @Override
        public int get(String name, int val) {
            return (Integer) value(name, val);
        }

        @Override
        public long get(String name, long val) {
            return (Long) value(name, val);
        }

        @Override
        public float get(String name, float val) {
            return (Float) value(name, val);
        }

        @Override
        public double get(String name, double val) {
            return (Double) value(name, val);
        }

        @Override
        public Object get(String name, Object val) {
            return value(name, val);
        }
    }
}
