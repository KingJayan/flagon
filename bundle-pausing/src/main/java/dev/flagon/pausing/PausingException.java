package dev.flagon.pausing;

/** its stack trace pauses, so TeaVM lets every exception constructor pause */
final class PausingException extends RuntimeException {

    @Override
    public Throwable fillInStackTrace() {
        try {
            Thread.sleep(0);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return this;
    }
}
