package javax.naming;

import java.util.Hashtable;

public class InitialContext implements Context {

    public InitialContext() {
    }

    public InitialContext(Hashtable<?, ?> environment) {
    }

    @Override
    public Object lookup(String name) throws NamingException {
        throw new NameNotFoundException(name + " is not in a page. see flagon-nu.vercel.app/docs");
    }

    @Override
    public void close() {
    }
}
