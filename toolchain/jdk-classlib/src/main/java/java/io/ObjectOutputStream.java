package java.io;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * java serialization, writing the stream format the jvm writes, byte for byte where the objects
 * are the same: the same block data, handles and class descriptors.
 */
public class ObjectOutputStream extends OutputStream
        implements ObjectOutput, ObjectStreamConstants {

    private static final int MAX_BLOCK_SIZE = 1024;

    private final OutputStream out;
    private final boolean enableOverride;
    private final byte[] buf = new byte[MAX_BLOCK_SIZE];
    private int pos;
    private boolean blkmode;
    private final Map<Object, Integer> handles = new IdentityHashMap<>();
    private final Map<Object, Object> subs = new IdentityHashMap<>();
    /** one copy per wrapper, so a jdk form names it once and refers back after */
    private final Map<Object, Object> copies = new IdentityHashMap<>();
    private int depth;
    private boolean enableReplace;

    private Object curObj;
    private ObjectStreamClass curDesc;
    private PutFieldImpl curPut;

    public ObjectOutputStream(OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }
        this.out = out;
        this.enableOverride = false;
        writeStreamHeader();
        setBlockDataMode(true);
    }

    protected ObjectOutputStream() throws IOException {
        this.out = null;
        this.enableOverride = true;
    }

    public void useProtocolVersion(int version) throws IOException {
        if (version != PROTOCOL_VERSION_2) {
            throw new IllegalArgumentException("only protocol version 2 is written here");
        }
    }

    @Override
    public final void writeObject(Object obj) throws IOException {
        if (enableOverride) {
            writeObjectOverride(obj);
            return;
        }
        writeObject0(obj, false);
    }

    protected void writeObjectOverride(Object obj) throws IOException {
    }

    public void writeUnshared(Object obj) throws IOException {
        writeObject0(obj, true);
    }

    public void defaultWriteObject() throws IOException {
        if (curObj == null || curDesc == null) {
            throw new NotActiveException("not in call to writeObject");
        }
        boolean old = setBlockDataMode(false);
        defaultWriteFields(curObj, curDesc);
        setBlockDataMode(old);
    }

    public PutField putFields() throws IOException {
        if (curDesc == null) {
            throw new NotActiveException("not in call to writeObject");
        }
        if (curPut == null) {
            curPut = new PutFieldImpl(curDesc);
        }
        return curPut;
    }

    public void writeFields() throws IOException {
        if (curPut == null) {
            throw new NotActiveException("no current PutField object");
        }
        boolean old = setBlockDataMode(false);
        writeValues(curPut.desc, curPut.values);
        setBlockDataMode(old);
    }

    public void reset() throws IOException {
        if (depth != 0) {
            throw new IOException("stream active");
        }
        boolean old = setBlockDataMode(false);
        writeRaw(TC_RESET);
        handles.clear();
        subs.clear();
        copies.clear();
        setBlockDataMode(old);
    }

    protected void annotateClass(Class<?> cl) throws IOException {
    }

    protected void annotateProxyClass(Class<?> cl) throws IOException {
    }

    protected Object replaceObject(Object obj) throws IOException {
        return obj;
    }

    protected boolean enableReplaceObject(boolean enable) throws SecurityException {
        boolean old = enableReplace;
        enableReplace = enable;
        return old;
    }

    protected void writeStreamHeader() throws IOException {
        writeRawShort(STREAM_MAGIC);
        writeRawShort(STREAM_VERSION);
    }

    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        writeUTF(desc.name);
        writeLong(desc.suid);
        writeByte(desc.flags);
        writeShort(desc.fields.length);
        for (ObjectStreamField field : desc.fields) {
            writeByte(field.getTypeCode());
            writeUTF(field.getName());
            if (!field.isPrimitive()) {
                writeTypeString(field.signature());
            }
        }
    }

    @Override
    public void write(int val) throws IOException {
        if (pos >= MAX_BLOCK_SIZE) {
            drain();
        }
        buf[pos++] = (byte) val;
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        while (len > 0) {
            if (pos >= MAX_BLOCK_SIZE) {
                drain();
            }
            int count = Math.min(len, MAX_BLOCK_SIZE - pos);
            System.arraycopy(b, off, buf, pos, count);
            pos += count;
            off += count;
            len -= count;
        }
    }

    @Override
    public void flush() throws IOException {
        drain();
        out.flush();
    }

    protected void drain() throws IOException {
        if (pos == 0) {
            return;
        }
        if (blkmode) {
            if (pos <= 0xFF) {
                out.write(TC_BLOCKDATA);
                out.write(pos);
            } else {
                out.write(TC_BLOCKDATALONG);
                out.write(pos >>> 24);
                out.write(pos >>> 16);
                out.write(pos >>> 8);
                out.write(pos);
            }
        }
        out.write(buf, 0, pos);
        pos = 0;
    }

    @Override
    public void close() throws IOException {
        flush();
        out.close();
    }

    @Override
    public void writeBoolean(boolean val) throws IOException {
        write(val ? 1 : 0);
    }

    @Override
    public void writeByte(int val) throws IOException {
        write(val);
    }

    @Override
    public void writeShort(int val) throws IOException {
        write(val >>> 8);
        write(val);
    }

    @Override
    public void writeChar(int val) throws IOException {
        writeShort(val);
    }

    @Override
    public void writeInt(int val) throws IOException {
        write(val >>> 24);
        write(val >>> 16);
        write(val >>> 8);
        write(val);
    }

    @Override
    public void writeLong(long val) throws IOException {
        writeInt((int) (val >>> 32));
        writeInt((int) val);
    }

    @Override
    public void writeFloat(float val) throws IOException {
        writeInt(Float.floatToIntBits(val));
    }

    @Override
    public void writeDouble(double val) throws IOException {
        writeLong(Double.doubleToLongBits(val));
    }

    @Override
    public void writeBytes(String str) throws IOException {
        for (int at = 0; at < str.length(); at++) {
            write(str.charAt(at));
        }
    }

    @Override
    public void writeChars(String str) throws IOException {
        for (int at = 0; at < str.length(); at++) {
            writeChar(str.charAt(at));
        }
    }

    @Override
    public void writeUTF(String str) throws IOException {
        long length = utfLength(str);
        if (length > 0xFFFF) {
            throw new UTFDataFormatException();
        }
        writeShort((int) length);
        writeUtfBody(str);
    }

    /** switches block data on or off, writing out what was held for the mode it leaves */
    boolean setBlockDataMode(boolean mode) throws IOException {
        if (blkmode == mode) {
            return blkmode;
        }
        drain();
        blkmode = mode;
        return !blkmode;
    }

    private void writeRaw(int val) throws IOException {
        write(val);
    }

    private void writeRawShort(int val) throws IOException {
        writeShort(val);
    }

    private void writeObject0(Object obj, boolean unshared) throws IOException {
        boolean old = setBlockDataMode(false);
        depth++;
        try {
            Object current = subs.containsKey(obj) ? subs.get(obj) : obj;
            if (written(current, unshared)) {
                return;
            }
            Object orig = current;
            Class<?> cl = current.getClass();
            ObjectStreamClass desc;
            while (true) {
                desc = ObjectStreamClass.lookupAny(cl);
                if (desc.writeReplaceMethod == null) {
                    break;
                }
                Object replaced = desc.invoke(desc.writeReplaceMethod, current);
                if (replaced == null || replaced.getClass() == cl) {
                    current = replaced;
                    break;
                }
                current = replaced;
                cl = replaced.getClass();
            }
            if (enableReplace && current != null) {
                Object replaced = replaceObject(current);
                if (replaced != current && replaced != null) {
                    desc = ObjectStreamClass.lookupAny(replaced.getClass());
                }
                current = replaced;
            }
            if (current != orig) {
                subs.put(orig, current);
                if (written(current, unshared)) {
                    return;
                }
                cl = current.getClass();
                desc = ObjectStreamClass.lookupAny(cl);
            }

            if (current instanceof String text) {
                writeString(text, unshared);
            } else if (cl.isArray()) {
                writeArray(current, desc, unshared);
            } else if (desc.isEnum) {
                writeEnum((Enum<?>) current, desc, unshared);
            } else if (desc.serializable || desc.externalizable) {
                writeOrdinaryObject(current, desc, unshared);
            } else {
                throw new NotSerializableException(cl.getName() + JdkForms.whyNot(cl));
            }
        } finally {
            depth--;
            setBlockDataMode(old);
        }
    }

    /** writes null, a back reference, a class or a descriptor, and says whether it did */
    private boolean written(Object obj, boolean unshared) throws IOException {
        if (obj == null) {
            writeRaw(TC_NULL);
            return true;
        }
        Integer handle = unshared ? null : handles.get(obj);
        if (handle != null) {
            writeHandle(handle);
            return true;
        }
        if (obj instanceof Class<?> cl) {
            writeRaw(TC_CLASS);
            writeClassDesc(ObjectStreamClass.lookupAny(cl), false);
            assign(cl);
            return true;
        }
        if (obj instanceof ObjectStreamClass desc) {
            writeClassDesc(desc, unshared);
            return true;
        }
        return false;
    }

    private void writeHandle(int handle) throws IOException {
        writeRaw(TC_REFERENCE);
        writeInt(baseWireHandle + handle);
    }

    private void assign(Object obj) {
        handles.put(obj, handles.size());
    }

    private void writeClassDesc(ObjectStreamClass desc, boolean unshared) throws IOException {
        if (desc == null) {
            writeRaw(TC_NULL);
            return;
        }
        Integer handle = unshared ? null : handles.get(desc);
        if (handle != null) {
            writeHandle(handle);
            return;
        }
        writeRaw(TC_CLASSDESC);
        assign(desc);
        writeClassDescriptor(desc);
        setBlockDataMode(true);
        if (desc.cl != null) {
            annotateClass(desc.cl);
        }
        setBlockDataMode(false);
        writeRaw(TC_ENDBLOCKDATA);
        writeClassDesc(desc.superDesc, false);
    }

    private void writeTypeString(String text) throws IOException {
        Integer handle = handles.get(text);
        if (handle != null) {
            writeHandle(handle);
        } else {
            writeString(text, false);
        }
    }

    private void writeString(String text, boolean unshared) throws IOException {
        assign(text);
        long length = utfLength(text);
        if (length <= 0xFFFF) {
            writeRaw(TC_STRING);
            writeShort((int) length);
        } else {
            writeRaw(TC_LONGSTRING);
            writeLong(length);
        }
        writeUtfBody(text);
    }

    private void writeArray(Object array, ObjectStreamClass desc, boolean unshared)
            throws IOException {
        writeRaw(TC_ARRAY);
        writeClassDesc(desc, false);
        assign(array);
        int length = Array.getLength(array);
        writeInt(length);
        Class<?> item = array.getClass().getComponentType();
        if (!item.isPrimitive()) {
            Object[] items = (Object[]) array;
            for (Object element : items) {
                writeObject0(element, false);
            }
            return;
        }
        for (int at = 0; at < length; at++) {
            writePrimitive(Array.get(array, at));
        }
    }

    private void writeEnum(Enum<?> constant, ObjectStreamClass desc, boolean unshared)
            throws IOException {
        writeRaw(TC_ENUM);
        writeClassDesc(desc, false);
        assign(constant);
        writeString(constant.name(), false);
    }

    private void writeOrdinaryObject(Object obj, ObjectStreamClass desc, boolean unshared)
            throws IOException {
        writeRaw(TC_OBJECT);
        writeClassDesc(desc, false);
        assign(obj);
        if (desc.externalizable && desc.form == null) {
            writeExternalData((Externalizable) obj);
        } else {
            writeSerialData(obj, desc);
        }
    }

    private void writeExternalData(Externalizable obj) throws IOException {
        Object savedObj = curObj;
        ObjectStreamClass savedDesc = curDesc;
        PutFieldImpl savedPut = curPut;
        curObj = obj;
        curDesc = null;
        curPut = null;
        setBlockDataMode(true);
        obj.writeExternal(this);
        setBlockDataMode(false);
        writeRaw(TC_ENDBLOCKDATA);
        curObj = savedObj;
        curDesc = savedDesc;
        curPut = savedPut;
    }

    private void writeSerialData(Object obj, ObjectStreamClass desc) throws IOException {
        for (ObjectStreamClass level : desc.hierarchy()) {
            if (level.form != null) {
                if (level.hasWriteObjectData()) {
                    setBlockDataMode(true);
                    level.form.write(this, level, obj);
                    setBlockDataMode(false);
                    writeRaw(TC_ENDBLOCKDATA);
                } else {
                    level.form.write(this, level, obj);
                }
            } else if (level.writeObjectMethod != null) {
                Object savedObj = curObj;
                ObjectStreamClass savedDesc = curDesc;
                PutFieldImpl savedPut = curPut;
                curObj = obj;
                curDesc = level;
                curPut = null;
                setBlockDataMode(true);
                level.invoke(level.writeObjectMethod, obj, this);
                setBlockDataMode(false);
                writeRaw(TC_ENDBLOCKDATA);
                curObj = savedObj;
                curDesc = savedDesc;
                curPut = savedPut;
            } else {
                defaultWriteFields(obj, level);
            }
        }
    }

    private void defaultWriteFields(Object obj, ObjectStreamClass level) throws IOException {
        Object[] values = new Object[level.fields.length];
        for (int at = 0; at < values.length; at++) {
            Field slot = level.slots == null ? null : level.slots[at];
            try {
                values[at] = slot == null ? null : slot.get(obj);
            } catch (IllegalAccessException denied) {
                throw new InvalidClassException(level.name, denied.toString());
            }
        }
        writeValues(level, values);
    }

    /** a level's field values, in its field order: primitives as bytes, then each object */
    void writeValues(ObjectStreamClass level, Object[] values) throws IOException {
        boolean old = setBlockDataMode(false);
        for (int at = 0; at < values.length; at++) {
            ObjectStreamField field = level.fields[at];
            if (field.isPrimitive()) {
                Object value = values[at];
                writePrimitive(value != null ? value : zero(field.getTypeCode()));
            }
        }
        for (int at = 0; at < values.length; at++) {
            ObjectStreamField field = level.fields[at];
            if (!field.isPrimitive()) {
                writeObject0(values[at], field.isUnshared());
            }
        }
        setBlockDataMode(old);
    }

    private void writePrimitive(Object value) throws IOException {
        if (value instanceof Integer number) {
            writeInt(number);
        } else if (value instanceof Long number) {
            writeLong(number);
        } else if (value instanceof Boolean flag) {
            writeBoolean(flag);
        } else if (value instanceof Byte number) {
            writeByte(number);
        } else if (value instanceof Character unit) {
            writeChar(unit);
        } else if (value instanceof Short number) {
            writeShort(number);
        } else if (value instanceof Float number) {
            writeFloat(number);
        } else if (value instanceof Double number) {
            writeDouble(number);
        }
    }

    static Object zero(char code) {
        return switch (code) {
            case 'Z' -> Boolean.FALSE;
            case 'B' -> (byte) 0;
            case 'C' -> (char) 0;
            case 'S' -> (short) 0;
            case 'I' -> 0;
            case 'J' -> 0L;
            case 'F' -> 0f;
            default -> 0d;
        };
    }

    private static long utfLength(String text) {
        long length = 0;
        for (int at = 0; at < text.length(); at++) {
            char unit = text.charAt(at);
            length += unit >= 0x0001 && unit <= 0x007F ? 1 : unit > 0x07FF ? 3 : 2;
        }
        return length;
    }

    private void writeUtfBody(String text) throws IOException {
        for (int at = 0; at < text.length(); at++) {
            char unit = text.charAt(at);
            if (unit >= 0x0001 && unit <= 0x007F) {
                write(unit);
            } else if (unit > 0x07FF) {
                write(0xE0 | ((unit >> 12) & 0x0F));
                write(0x80 | ((unit >> 6) & 0x3F));
                write(0x80 | (unit & 0x3F));
            } else {
                write(0xC0 | ((unit >> 6) & 0x1F));
                write(0x80 | (unit & 0x3F));
            }
        }
    }

    public abstract static class PutField {

        public PutField() {
        }

        public abstract void put(String name, boolean val);

        public abstract void put(String name, byte val);

        public abstract void put(String name, char val);

        public abstract void put(String name, short val);

        public abstract void put(String name, int val);

        public abstract void put(String name, long val);

        public abstract void put(String name, float val);

        public abstract void put(String name, double val);

        public abstract void put(String name, Object val);

        @Deprecated
        public abstract void write(ObjectOutput out) throws IOException;
    }

    private final class PutFieldImpl extends PutField {

        private final ObjectStreamClass desc;
        private final Object[] values;
        private final Map<String, Integer> index = new HashMap<>();

        PutFieldImpl(ObjectStreamClass desc) {
            this.desc = desc;
            this.values = new Object[desc.fields.length];
            for (int at = 0; at < desc.fields.length; at++) {
                index.put(desc.fields[at].getName(), at);
            }
        }

        private void set(String name, Object value) {
            Integer at = index.get(name);
            if (at == null) {
                throw new IllegalArgumentException("no such field " + name);
            }
            values[at] = value;
        }

        @Override
        public void put(String name, boolean val) {
            set(name, val);
        }

        @Override
        public void put(String name, byte val) {
            set(name, val);
        }

        @Override
        public void put(String name, char val) {
            set(name, val);
        }

        @Override
        public void put(String name, short val) {
            set(name, val);
        }

        @Override
        public void put(String name, int val) {
            set(name, val);
        }

        @Override
        public void put(String name, long val) {
            set(name, val);
        }

        @Override
        public void put(String name, float val) {
            set(name, val);
        }

        @Override
        public void put(String name, double val) {
            set(name, val);
        }

        @Override
        public void put(String name, Object val) {
            set(name, val);
        }

        @Override
        public void write(ObjectOutput out) throws IOException {
            if (out != ObjectOutputStream.this) {
                throw new IllegalArgumentException("wrong stream");
            }
            writeFields();
        }
    }

    Object copyOf(Object wrapper, java.util.function.Supplier<Object> copy) {
        return copies.computeIfAbsent(wrapper, ignored -> copy.get());
    }

    /** the objects a list or map holds, for the jdk forms */
    static List<Object> entries(Map<?, ?> map) {
        List<Object> flat = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            flat.add(entry.getKey());
            flat.add(entry.getValue());
        }
        return flat;
    }
}
