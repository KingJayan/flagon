package javax.naming;

public interface Context {

    String INITIAL_CONTEXT_FACTORY = "java.naming.factory.initial";

    String PROVIDER_URL = "java.naming.provider.url";

    Object lookup(String name) throws NamingException;

    void close() throws NamingException;
}
