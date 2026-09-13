package java.net;

import java.io.IOException;

public class MulticastSocket extends DatagramSocket {

    public MulticastSocket() throws SocketException {
        super();
    }

    public MulticastSocket(int port) throws SocketException {
        super(port);
    }

    public MulticastSocket(SocketAddress address) throws SocketException {
        super(address);
    }

    public void joinGroup(InetAddress group) throws IOException {
        throw refuseWith();
    }

    public void leaveGroup(InetAddress group) throws IOException {
        throw refuseWith();
    }

    public void setTimeToLive(int hops) throws IOException {
    }

    public void setLoopbackMode(boolean off) throws SocketException {
    }
}
