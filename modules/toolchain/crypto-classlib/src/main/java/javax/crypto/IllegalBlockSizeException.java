package javax.crypto;

import java.security.GeneralSecurityException;

/** thrown when a block cipher is handed a length it cannot cut into blocks */
public class IllegalBlockSizeException extends GeneralSecurityException {

    public IllegalBlockSizeException() {
        super();
    }

    public IllegalBlockSizeException(String message) {
        super(message);
    }
}
