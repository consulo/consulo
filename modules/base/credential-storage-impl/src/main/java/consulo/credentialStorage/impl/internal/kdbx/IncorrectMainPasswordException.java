package consulo.credentialStorage.impl.internal.kdbx;

/**
 * Thrown when the main password for the KeePass database is incorrect.
 */
public class IncorrectMainPasswordException extends RuntimeException {
    private final boolean isFileMissed;

    public IncorrectMainPasswordException() {
        this(false);
    }

    public IncorrectMainPasswordException(boolean isFileMissed) {
        super();
        this.isFileMissed = isFileMissed;
    }

    public boolean isFileMissed() {
        return isFileMissed;
    }
}
