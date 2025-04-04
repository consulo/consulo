package consulo.credentialStorage;

import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;

/**
 * Represents a pair of user and password.
 *
 * @param user     the account name or path to an SSH key file.
 * @param password the associated password (can be empty).
 */
public final class Credentials {
    private final String userName;
    private final OneTimeString password;

    public static final Credentials ACCESS_TO_KEY_CHAIN_DENIED = new Credentials(null, (OneTimeString) null);
    public static final Credentials CANNOT_UNLOCK_KEYCHAIN = new Credentials(null, (OneTimeString) null);

    public Credentials(@Nullable String user, @Nullable OneTimeString password) {
        this.userName = StringUtil.nullize(user);
        this.password = password;
    }

    public Credentials(@Nullable String user) {
        this(user, (OneTimeString) null);
    }

    public Credentials(@Nullable String user, @Nullable String password) {
        this(user, password != null ? new OneTimeString(password) : null);
    }

    public Credentials(@Nullable String user, @Nullable char[] password) {
        this(user, password != null ? new OneTimeString(password) : null);
    }

    public Credentials(@Nullable String user, @Nullable byte[] password) throws Exception {
        this(user, password != null ? OneTimeString.fromByteArray(password) : null);
    }

    @Deprecated
    public Credentials(@Nullable String user, @Nullable OneTimeString password, int i, Object m) {
        this(user, password);
    }

    public String getUserName() {
        return userName;
    }

    public String getPasswordAsString() {
        return password != null ? password.toString() : null;
    }

    public OneTimeString getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Credentials)) return false;
        Credentials other = (Credentials) obj;
        if (userName == null) {
            if (other.userName != null) return false;
        }
        else if (!userName.equals(other.userName)) return false;
        if (password == null) {
            return other.password == null;
        }
        else {
            return password.equals(other.password);
        }
    }

    @Override
    public int hashCode() {
        int result = (userName != null ? userName.hashCode() : 0);
        result = result * 37 + (password != null ? password.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        int passwordLength = password != null ? password.length() : 0;
        return "userName: " + userName + ", password size: " + passwordLength;
    }
}
