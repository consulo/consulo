package consulo.credentialStorage;

/**
 * Utility class for generating service names.
 */
public final class CredentialAttributesUtil {
    public static final String SERVICE_NAME_PREFIX = "Consulo";

    private CredentialAttributesUtil() {
    }

    /**
     * The combined name of your service and name of service that requires authentication.
     * <p>
     * Can be specified in:
     * * a reverse-DNS format: `com.apple.facetime: registrationV1`
     * * a prefixed human-readable format: `Consulo Settings Repository — github.com`,
     * where `Consulo` prefix **is mandatory**.
     */
    public static String generateServiceName(String subsystem, String key) {
        // Produces strings like "ConsuloSettings Repository — github.com"
        return SERVICE_NAME_PREFIX + " " + subsystem + " — " + key;
    }
}
