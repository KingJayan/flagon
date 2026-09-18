package javax.management;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public class NotificationBroadcasterSupport {

    private final List<NotificationListener> listeners = new CopyOnWriteArrayList<>();
    private final MBeanNotificationInfo[] about;

    public NotificationBroadcasterSupport() {
        this(null, new MBeanNotificationInfo[0]);
    }

    public NotificationBroadcasterSupport(Executor sender, MBeanNotificationInfo... about) {
        this.about = about == null ? new MBeanNotificationInfo[0] : about.clone();
    }

    public void addNotificationListener(NotificationListener listener, Object filter,
            Object handback) {
        listeners.add(listener);
    }

    public void removeNotificationListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    public void removeNotificationListener(NotificationListener listener, Object filter,
            Object handback) {
        listeners.remove(listener);
    }

    public void sendNotification(Notification notification) {
        for (NotificationListener listener : listeners) {
            listener.handleNotification(notification, null);
        }
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return about.clone();
    }
}
