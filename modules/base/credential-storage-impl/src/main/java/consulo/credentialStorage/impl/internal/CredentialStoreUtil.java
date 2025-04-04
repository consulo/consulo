package consulo.credentialStorage.impl.internal;

import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.OneTimeString;
import consulo.credentialStorage.impl.internal.PasswordSafeSettings;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for credential store related operations.
 */
public final class CredentialStoreUtil {
    // Logger for the credential store; associated with PasswordSafeSettings class.
    // Adjust the logger initialization as needed.
    private static final Logger LOG = Logger.getInstance(PasswordSafeSettings.class);

    // Escaping character used in parsing.
    private static final char ESCAPING_CHAR = '\\';

    private CredentialStoreUtil() {
    }

    /**
     * Joins the user and password data into a single UTF-8 encoded byte array.
     * If both user and password are null, returns null.
     *
     * @param user     the user name, may be null
     * @param password the OneTimeString password, may be null
     * @return the joined byte array, or null if both inputs are null
     */
    public static byte[] joinData(String user, OneTimeString password) {
        if (user == null && password == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(Objects.requireNonNullElse(user, ""));
        // Escape '\' and '@' characters
        StringUtil.escapeChar(builder, '\\');
        StringUtil.escapeChar(builder, '@');
        if (password != null) {
            builder.append('@');
            password.appendTo(builder);
        }
        // Encode the builder's content as UTF-8 bytes
        byte[] result = builder.toString().getBytes(StandardCharsets.UTF_8);
        // Clear builder (to clear password from memory)
        builder.setLength(0);
        return result;
    }

    /**
     * Splits the provided data string into Credentials.
     *
     * @param data the data string to split
     * @return a Credentials instance, or null if data is null or empty
     */
    public static Credentials splitData(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        List<String> parts = parseString(data, '@');
        String user = parts.size() > 0 ? parts.get(0) : null;
        String password = parts.size() > 1 ? parts.get(1) : null;
        return new Credentials(user, password);
    }

    /**
     * Parses a string by splitting it on the given delimiter while handling escaping.
     *
     * @param data      the string to parse
     * @param delimiter the delimiter character
     * @return a list of parts resulting from the parse
     */
    private static List<String> parseString(String data, char delimiter) {
        List<String> result = new ArrayList<>(2);
        StringBuilder part = new StringBuilder();
        int i = 0;
        while (i < data.length()) {
            char c = data.charAt(i++);
            if (c != delimiter) {
                if (c == ESCAPING_CHAR) {
                    if (i < data.length()) {
                        c = data.charAt(i++);
                    }
                }
                part.append(c);
            }
            else {
                // Delimiter encountered: add current part and the remainder of the string as the second part.
                result.add(part.toString());
                part.setLength(0);
                if (i < data.length()) {
                    result.add(data.substring(i));
                }
                return result;
            }
        }
        // If loop ends without encountering a delimiter, add remaining part.
        result.add(part.toString());
        return result;
    }

    /**
     * Creates a new instance of SecureRandom.
     *
     * @return a new SecureRandom instance
     */
    public static SecureRandom createSecureRandom() {
        // Do not use SecureRandom.getInstanceStrong() to avoid blocking.
        return new SecureRandom();
    }

    /**
     * Generates a byte array of the given size using the provided SecureRandom instance.
     *
     * @param random the SecureRandom instance to use
     * @param size   the size of the byte array to generate
     * @return a byte array filled with random bytes
     */
    public static byte[] generateBytes(SecureRandom random, int size) {
        byte[] result = new byte[size];
        random.nextBytes(result);
        return result;
    }

    /**
     * Returns the logger instance.
     *
     * @return the Logger
     */
    public static Logger getLogger() {
        return LOG;
    }
}
