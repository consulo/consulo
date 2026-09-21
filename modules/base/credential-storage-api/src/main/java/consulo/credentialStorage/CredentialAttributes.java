package consulo.credentialStorage;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

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
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof CredentialAttributes that
            && isPasswordMemoryOnly == that.isPasswordMemoryOnly
            && cacheDeniedItems == that.cacheDeniedItems
            && serviceName.equals(that.serviceName)
            && Objects.equals(userName, that.userName);
    }

    @Override
    public int hashCode() {
        int result = serviceName.hashCode();
        result = 31 * result + Objects.hashCode(userName);
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
