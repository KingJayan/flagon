package javax.naming;

import java.util.Hashtable;

/** refuses every name, because a page has no jndi provider */
public class InitialContext implements Context {

    public InitialContext() {
    }

    public InitialContext(Hashtable<?, ?> environment) {
    }

    @Override
    public Object lookup(String name) throws NamingException {
        throw new NameNotFoundException(name + " is not in a page. see docs/COMPATIBILITY.md");
    }

    @Override
    public void close() {
    }
}
