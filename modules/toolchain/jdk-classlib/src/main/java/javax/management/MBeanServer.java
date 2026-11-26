package javax.management;

import java.util.Set;

public interface MBeanServer {

    ObjectInstance registerMBean(Object object, ObjectName name)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException;

    void unregisterMBean(ObjectName name) throws MBeanRegistrationException;

    Set<ObjectName> queryNames(ObjectName name, QueryExp query);

    boolean isRegistered(ObjectName name);
}
