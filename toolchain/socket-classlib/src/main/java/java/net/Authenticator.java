package java.net;

/**
 where my password at bro
 */
public abstract class Authenticator {

    public enum RequestorType {
        PROXY,
        SERVER
    }

    private static Authenticator installed;

    private String host;
    private InetAddress address;
    private int port;
    private String protocol;
    private String prompt;
    private String scheme;
    private RequestorType type;

    public static void setDefault(Authenticator authenticator) {
        installed = authenticator;
    }

    public static PasswordAuthentication requestPasswordAuthentication(
            String host, InetAddress address, int port, String protocol,
            String prompt, String scheme) {
        return requestPasswordAuthentication(host, address, port, protocol, prompt, scheme,
                RequestorType.SERVER);
    }

    public static PasswordAuthentication requestPasswordAuthentication(
            InetAddress address, int port, String protocol, String prompt, String scheme) {
        return requestPasswordAuthentication(null, address, port, protocol, prompt, scheme,
                RequestorType.SERVER);
    }

    public static synchronized PasswordAuthentication requestPasswordAuthentication(
            String host, InetAddress address, int port, String protocol, String prompt,
            String scheme, RequestorType type) {
        Authenticator asked = installed;
        if (asked == null) {
            return null;
        }
        asked.host = host;
        asked.address = address;
        asked.port = port;
        asked.protocol = protocol;
        asked.prompt = prompt;
        asked.scheme = scheme;
        asked.type = type;
        return asked.getPasswordAuthentication();
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return null;
    }

    protected final String getRequestingHost() {
        return host;
    }

    protected final InetAddress getRequestingSite() {
        return address;
    }

    protected final int getRequestingPort() {
        return port;
    }

    protected final String getRequestingProtocol() {
        return protocol;
    }

    protected final String getRequestingPrompt() {
        return prompt;
    }

    protected final String getRequestingScheme() {
        return scheme;
    }

    protected final RequestorType getRequestorType() {
        return type;
    }
}
