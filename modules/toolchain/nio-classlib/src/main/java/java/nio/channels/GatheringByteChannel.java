package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface GatheringByteChannel extends WritableByteChannel {

    long write(ByteBuffer[] buffers, int offset, int length) throws IOException;

    long write(ByteBuffer[] buffers) throws IOException;
}
