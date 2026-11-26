package javax.management;

public class ObjectName {

    private final String name;

    public ObjectName(String name) throws MalformedObjectNameException {
        if (name == null || name.isEmpty()) {
            throw new MalformedObjectNameException("a managed object needs a name");
        }
        this.name = name;
    }

    public String getCanonicalName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ObjectName named && name.equals(named.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
