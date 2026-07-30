package dev.flagon.platform;

/** thrown when a host genuinely cannot provide a capability, rather than stubbing it out. */
public class UnsupportedCapabilityException extends RuntimeException {

    public UnsupportedCapabilityException(String message) {
        super(message);
    }
}
