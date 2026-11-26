package java.io;

import dev.flagon.classlib.KeptData;
import dev.flagon.serial.Unconstructed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * a class as java serialization describes it: its name, serialVersionUID, flags and fields, and,
 * for a class here, how to reach its fields and its serialization methods. jdk classes, whose
 * browser classlib differs from the jdk's inside, are described by {@link JdkForms}.
 */
public class ObjectStreamClass implements Serializable {

    public static final ObjectStreamField[] NO_FIELDS = new ObjectStreamField[0];

    private static final Map<Class<?>, ObjectStreamClass> LOCAL = new HashMap<>();

    String name;
    long suid;
    byte flags;
    ObjectStreamField[] fields = NO_FIELDS;
    ObjectStreamClass superDesc;

    Class<?> cl;
    /** the jdk form behind this level, or null for a class of the application */
    JdkForms.Level form;
    Field[] slots;
    Method writeObjectMethod;
    Method readObjectMethod;
    Method readObjectNoDataMethod;
    Method writeReplaceMethod;
    Method readResolveMethod;
    Constructor<?> constructor;
    boolean serializable;
    boolean externalizable;
    boolean isEnum;
    boolean isRecord;

    ObjectStreamClass() {
    }

    /** the descriptor of a serializable class, or null for any other */
    public static ObjectStreamClass lookup(Class<?> cl) {
        ObjectStreamClass desc = lookupAny(cl);
        return desc.serializable || desc.externalizable ? desc : null;
    }

    public static ObjectStreamClass lookupAny(Class<?> cl) {
        ObjectStreamClass desc = LOCAL.get(cl);
        if (desc == null) {
            desc = describe(cl);
            LOCAL.put(cl, desc);
        }
        return desc;
    }

    public String getName() {
        return name;
    }

    public long getSerialVersionUID() {
        return suid;
    }

    public Class<?> forClass() {
        return cl;
    }

    public ObjectStreamField[] getFields() {
        return fields.clone();
    }

    public ObjectStreamField getField(String fieldName) {
        for (ObjectStreamField field : fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name + ": static final long serialVersionUID = " + suid + "L;";
    }

    boolean hasWriteObjectData() {
        return (flags & ObjectStreamConstants.SC_WRITE_METHOD) != 0;
    }

    boolean hasBlockExternalData() {
        return (flags & ObjectStreamConstants.SC_BLOCK_DATA) != 0;
    }

    /** this class and its serializable superclasses, the topmost first */
    List<ObjectStreamClass> hierarchy() {
        List<ObjectStreamClass> levels = new ArrayList<>();
        for (ObjectStreamClass at = this; at != null; at = at.superDesc) {
            levels.add(0, at);
        }
        return levels;
    }

    private static ObjectStreamClass describe(Class<?> cl) {
        ObjectStreamClass desc = new ObjectStreamClass();
        desc.cl = cl;
        desc.name = cl.getName();
        if (cl.isArray()) {
            desc.serializable = true;
            desc.flags = ObjectStreamConstants.SC_SERIALIZABLE;
            desc.suid = arrayId(cl);
            return desc;
        }
        Class<?> enumeration = enumOf(cl);
        if (enumeration != null && enumeration != cl) {
            return lookupAny(enumeration);
        }
        if (enumeration != null) {
            desc.name = enumeration.getName();
            desc.isEnum = true;
            desc.serializable = true;
            desc.flags = ObjectStreamConstants.SC_SERIALIZABLE | ObjectStreamConstants.SC_ENUM;
            desc.superDesc = enumeration == Enum.class ? null : lookupAny(Enum.class);
            return desc;
        }
        ObjectStreamClass jdk = JdkForms.describe(cl);
        if (jdk != null) {
            return jdk;
        }
        String className = cl.getName();
        if (className.startsWith("java.") || className.startsWith("javax.")) {
            return desc;
        }
        desc.serializable = Serializable.class.isAssignableFrom(cl);
        desc.externalizable = Externalizable.class.isAssignableFrom(cl);
        if (!desc.serializable) {
            return desc;
        }
        Class<?> parent = cl.getSuperclass();
        if (parent != null && Serializable.class.isAssignableFrom(parent)) {
            desc.superDesc = lookupAny(parent);
        }
        desc.isRecord = parent == Record.class;
        desc.suid = serialId(cl, desc.isRecord);
        if (desc.externalizable) {
            desc.flags = ObjectStreamConstants.SC_EXTERNALIZABLE
                    | ObjectStreamConstants.SC_BLOCK_DATA;
            desc.constructor = find(() -> cl.getDeclaredConstructor());
            return desc;
        }
        desc.flags = ObjectStreamConstants.SC_SERIALIZABLE;
        desc.writeObjectMethod = privateMethod(cl, "writeObject", ObjectOutputStream.class);
        desc.readObjectMethod = privateMethod(cl, "readObject", ObjectInputStream.class);
        desc.readObjectNoDataMethod = privateMethod(cl, "readObjectNoData");
        if (desc.writeObjectMethod != null) {
            desc.flags |= ObjectStreamConstants.SC_WRITE_METHOD;
        }
        desc.writeReplaceMethod = inherited(cl, "writeReplace");
        desc.readResolveMethod = inherited(cl, "readResolve");
        fields(desc);
        if (desc.isRecord) {
            desc.constructor = canonical(cl);
        } else {
            desc.constructor = find(() -> cl.getDeclaredConstructor(Unconstructed.class));
        }
        return desc;
    }

    private static void fields(ObjectStreamClass desc) {
        Class<?> cl = desc.cl;
        ObjectStreamField[] declared = persistentFields(cl);
        List<ObjectStreamField> found = new ArrayList<>();
        List<Field> reached = new ArrayList<>();
        if (declared != null) {
            for (ObjectStreamField field : declared) {
                found.add(new ObjectStreamField(field.getName(), field.getType(),
                        field.isUnshared()));
            }
        } else {
            for (Field field : cl.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                    found.add(new ObjectStreamField(field.getName(), field.getType()));
                }
            }
        }
        ObjectStreamField[] sorted = found.toArray(new ObjectStreamField[0]);
        Arrays.sort(sorted);
        for (ObjectStreamField field : sorted) {
            Field slot = find(() -> cl.getDeclaredField(field.getName()));
            if (slot != null) {
                slot.setAccessible(true);
            }
            reached.add(slot);
        }
        desc.fields = sorted;
        desc.slots = reached.toArray(new Field[0]);
    }

    private static ObjectStreamField[] persistentFields(Class<?> cl) {
        Field declared = find(() -> cl.getDeclaredField("serialPersistentFields"));
        if (declared == null || !Modifier.isStatic(declared.getModifiers())) {
            return null;
        }
        try {
            declared.setAccessible(true);
            return (ObjectStreamField[]) declared.get(null);
        } catch (IllegalAccessException | ClassCastException unread) {
            return null;
        }
    }

    /** the declared serialVersionUID, or the default the build computed from the class file */
    private static long serialId(Class<?> cl, boolean record) {
        Field declared = find(() -> cl.getDeclaredField("serialVersionUID"));
        if (declared != null && Modifier.isStatic(declared.getModifiers())) {
            try {
                declared.setAccessible(true);
                return ((Number) declared.get(null)).longValue();
            } catch (IllegalAccessException | ClassCastException unread) {
                // falls back to the default
            }
        }
        if (record) {
            return 0L;
        }
        for (String[] line : KeptData.of(cl).lines('U')) {
            return Long.parseLong(line[0]);
        }
        return 0L;
    }

    /** an array's default serialVersionUID: its name and modifiers, which is all the jdk hashes */
    private static long arrayId(Class<?> cl) {
        Class<?> item = cl;
        while (item.isArray()) {
            item = item.getComponentType();
        }
        int access = item.isPrimitive() ? Modifier.PUBLIC
                : item.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED);
        int modifiers = (access | Modifier.FINAL | Modifier.ABSTRACT)
                & (Modifier.PUBLIC | Modifier.FINAL | Modifier.INTERFACE | Modifier.ABSTRACT);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF(cl.getName());
            out.writeInt(modifiers);
            out.flush();
            byte[] hash = Sha1.digest(bytes.toByteArray());
            long id = 0;
            for (int at = 7; at >= 0; at--) {
                id = (id << 8) | (hash[at] & 0xFF);
            }
            return id;
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Class<?> enumOf(Class<?> cl) {
        if (cl == Enum.class) {
            return cl;
        }
        for (Class<?> at = cl; at != null; at = at.getSuperclass()) {
            if (at.getSuperclass() == Enum.class) {
                return at;
            }
        }
        return null;
    }

    private static Constructor<?> canonical(Class<?> cl) {
        RecordComponent[] components = RecordComponent.of(cl);
        Class<?>[] types = new Class<?>[components.length];
        for (int at = 0; at < types.length; at++) {
            types[at] = components[at].getType();
        }
        return find(() -> cl.getDeclaredConstructor(types));
    }

    private static Method privateMethod(Class<?> cl, String method, Class<?>... parameters) {
        Method found = find(() -> cl.getDeclaredMethod(method, parameters));
        if (found == null || !Modifier.isPrivate(found.getModifiers())
                || Modifier.isStatic(found.getModifiers())) {
            return null;
        }
        found.setAccessible(true);
        return found;
    }

    /** writeReplace or readResolve, which a subclass inherits unless the method is private */
    private static Method inherited(Class<?> cl, String method) {
        for (Class<?> at = cl; at != null; at = at.getSuperclass()) {
            Class<?> owner = at;
            Method found = find(() -> owner.getDeclaredMethod(method));
            if (found != null && !Modifier.isStatic(found.getModifiers())) {
                if (owner != cl && Modifier.isPrivate(found.getModifiers())) {
                    return null;
                }
                found.setAccessible(true);
                return found;
            }
        }
        return null;
    }

    Object invoke(Method method, Object target, Object... arguments) throws IOException {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException thrown) {
            Throwable cause = thrown.getCause();
            if (cause instanceof IOException io) {
                throw io;
            } else if (cause instanceof RuntimeException runtime) {
                throw runtime;
            } else if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException(cause);
        } catch (IllegalAccessException denied) {
            throw new InvalidClassException(name, denied.toString());
        }
    }

    private interface Lookup<T> {
        T find() throws ReflectiveOperationException;
    }

    private static <T> T find(Lookup<T> lookup) {
        try {
            return lookup.find();
        } catch (ReflectiveOperationException | RuntimeException absent) {
            return null;
        }
    }
}
