package consulo.credentialStorage;

import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

/**
 * Utility methods for evaluating Credentials.
 */
public final class CredentialUtils {

    private CredentialUtils() {
    }

    @Contract("null -> false")
    public static boolean isFulfilled(@Nullable Credentials credentials) {
        return credentials != null &&
            credentials.getUserName() != null &&
            !isNullOrEmpty(credentials.getPassword());
    }

    @Contract("null -> false")
    public static boolean hasOnlyUserName(@Nullable Credentials credentials) {
        return credentials != null &&
            credentials.getUserName() != null &&
            isNullOrEmpty(credentials.getPassword());
    }

    public static boolean isEmpty(@Nullable Credentials credentials) {
        return credentials == null ||
            (credentials.getUserName() == null && isNullOrEmpty(credentials.getPassword()));
    }

    private static boolean isNullOrEmpty(@Nullable OneTimeString password) {
        return password == null || password.length() == 0;
    }
}
