package javax.management;

public class MBeanNotificationInfo {

    private final String[] types;
    private final String name;
    private final String description;

    public MBeanNotificationInfo(String[] types, String name, String description) {
        this.types = types == null ? new String[0] : types.clone();
        this.name = name;
        this.description = description;
    }

    public String[] getNotifTypes() {
        return types.clone();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
