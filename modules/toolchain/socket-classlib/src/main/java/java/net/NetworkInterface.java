package java.net;

import java.util.Collections;
import java.util.Enumeration;

public final class NetworkInterface {

    private static final NetworkInterface LOOPBACK = new NetworkInterface();

    private NetworkInterface() {
    }

    public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        return Collections.enumeration(Collections.singletonList(LOOPBACK));
    }

    public static NetworkInterface getByName(String name) throws SocketException {
        return "lo".equals(name) ? LOOPBACK : null;
    }

    public static NetworkInterface getByInetAddress(InetAddress address) throws SocketException {
        return address != null && address.isLoopbackAddress() ? LOOPBACK : null;
    }

    public String getName() {
        return "lo";
    }

    public String getDisplayName() {
        return "loopback";
    }

    public Enumeration<InetAddress> getInetAddresses() {
        return Collections.enumeration(
                Collections.singletonList(InetAddress.getLoopbackAddress()));
    }

    public byte[] getHardwareAddress() {
        return null;
    }

    public int getMTU() {
        return 0;
    }

    public boolean isLoopback() {
        return true;
    }

    public boolean isUp() {
        return true;
    }

    public boolean supportsMulticast() {
        return false;
    }
}
