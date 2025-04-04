package consulo.credentialStorage;

import jakarta.annotation.Nullable;

/**
 * Represents the combined service name and user information used for authentication.
 */
public final class CredentialAttributes {
    private final String serviceName;
    private final String userName;
    private final boolean isPasswordMemoryOnly;
    private final boolean cacheDeniedItems;

    public CredentialAttributes(String serviceName, @Nullable String userName, boolean isPasswordMemoryOnly, boolean cacheDeniedItems) {
        this.serviceName = serviceName;
        this.userName = userName;
        this.isPasswordMemoryOnly = isPasswordMemoryOnly;
        this.cacheDeniedItems = cacheDeniedItems;
    }

    public CredentialAttributes(String serviceName) {
        this(serviceName, null, false, true);
    }

    public CredentialAttributes(String serviceName, @Nullable String userName) {
        this(serviceName, userName, false, true);
    }

    public CredentialAttributes(String serviceName, @Nullable String userName, boolean isPasswordMemoryOnly) {
        this(serviceName, userName, isPasswordMemoryOnly, true);
    }

    @Deprecated
    public CredentialAttributes(String serviceName, @Nullable String userName, Class<?> requestor) {
        this(serviceName, userName, false, true);
    }

    @Deprecated
    public CredentialAttributes(String serviceName, @Nullable String userName, Class<?> requestor, boolean isPasswordMemoryOnly) {
        this(serviceName, userName, isPasswordMemoryOnly, true);
    }

    @Deprecated
    public CredentialAttributes(String serviceName, @Nullable String userName, Class<?> requestor, boolean isPasswordMemoryOnly, boolean cacheDeniedItems) {
        this(serviceName, userName, isPasswordMemoryOnly, cacheDeniedItems);
    }

    @Deprecated
    public CredentialAttributes(String serviceName, @Nullable String userName, Class<?> requestor, boolean isPasswordMemoryOnly, int i, Object m) {
        this(serviceName, userName, isPasswordMemoryOnly, true);
    }

    @Deprecated
    public CredentialAttributes(String serviceName, @Nullable String userName, Class<?> requestor, boolean isPasswordMemoryOnly, boolean cacheDeniedItems, int i, Object m) {
        this(serviceName, userName, isPasswordMemoryOnly, cacheDeniedItems);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUserName() {
        return userName;
    }

    public boolean isPasswordMemoryOnly() {
        return isPasswordMemoryOnly;
    }

    public boolean isCacheDeniedItems() {
        return cacheDeniedItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CredentialAttributes)) {
            return false;
        }
        CredentialAttributes that = (CredentialAttributes) o;
        if (isPasswordMemoryOnly != that.isPasswordMemoryOnly) {
            return false;
        }
        if (cacheDeniedItems != that.cacheDeniedItems) {
            return false;
        }
        if (!serviceName.equals(that.serviceName)) {
            return false;
        }
        return userName != null ? userName.equals(that.userName) : that.userName == null;
    }

    @Override
    public int hashCode() {
        int result = serviceName.hashCode();
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (isPasswordMemoryOnly ? 1 : 0);
        result = 31 * result + (cacheDeniedItems ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CredentialAttributes{" +
            "serviceName='" + serviceName + '\'' +
            ", userName='" + userName + '\'' +
            ", isPasswordMemoryOnly=" + isPasswordMemoryOnly +
            ", cacheDeniedItems=" + cacheDeniedItems +
            '}';
    }
}
