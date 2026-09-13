package java.net;

/** an ipv6 address that keeps its sixteen bytes */
public final class Inet6Address extends InetAddress {

    private final byte[] address;

    Inet6Address(byte[] address) {
        super(colons(address));
        this.address = address.clone();
    }

    private static String colons(byte[] address) {
        StringBuilder host = new StringBuilder();
        for (int i = 0; i < 16; i += 2) {
            int group = (address[i] & 0xff) << 8 | address[i + 1] & 0xff;
            host.append(i == 0 ? "" : ":").append(Integer.toHexString(group));
        }
        return host.toString();
    }

    @Override
    public byte[] getAddress() {
        return address.clone();
    }

    public int getScopeId() {
        return 0;
    }

    @Override
    public boolean isAnyLocalAddress() {
        for (byte part : address) {
            if (part != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isLoopbackAddress() {
        for (int i = 0; i < 15; i++) {
            if (address[i] != 0) {
                return false;
            }
        }
        return address[15] == 1;
    }
}
