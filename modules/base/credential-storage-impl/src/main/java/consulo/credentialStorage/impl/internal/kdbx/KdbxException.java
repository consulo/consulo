package consulo.credentialStorage.impl.internal.kdbx;

/**
 * Exception thrown for errors related to KDBX operations.
 */
public class KdbxException extends RuntimeException {
    public KdbxException(String message) {
        super(message);
    }
}
