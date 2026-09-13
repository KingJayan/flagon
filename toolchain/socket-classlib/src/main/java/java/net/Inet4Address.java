package java.net;

/** an ipv4 address that keeps its four bytes */
public final class Inet4Address extends InetAddress {

    private final byte[] address;

    Inet4Address(byte[] address) {
        super(dotted(address));
        this.address = address.clone();
    }

    private static String dotted(byte[] address) {
        StringBuilder host = new StringBuilder();
        for (byte part : address) {
            host.append(host.isEmpty() ? "" : ".").append(part & 0xff);
        }
        return host.toString();
    }

    @Override
    public byte[] getAddress() {
        return address.clone();
    }

    @Override
    public boolean isAnyLocalAddress() {
        return (address[0] | address[1] | address[2] | address[3]) == 0;
    }

    @Override
    public boolean isLoopbackAddress() {
        return address[0] == 127;
    }
}
