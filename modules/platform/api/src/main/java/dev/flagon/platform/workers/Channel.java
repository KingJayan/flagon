package dev.flagon.platform.workers;

/** one end of the message channel between a bundle and a worker; messages keep their order */
public interface Channel extends AutoCloseable {

    void send(Message message);

    default void send(String text) {
        send(new Message(text, null));
    }

    default void send(byte[] data) {
        send(new Message(null, data));
    }

    /**
     * waits for the next message, pausing only the calling thread.
     * @return null once the other end has closed and every message is read
     */
    Message receive();

    /** @return the next message, or null when none has arrived yet */
    Message poll();

    /** @return false once the other end has closed or the worker has exited */
    boolean isOpen();

    /** ends the channel; closing a spawned worker's channel stops the worker */
    @Override
    void close();
}
