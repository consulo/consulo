package consulo.credentialStorage.impl.internal.gpg;

public final class PgpKey {
    private final String keyId;
    private final String userId;

    public PgpKey(String keyId, String userId) {
        this.keyId = keyId;
        this.userId = userId;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PgpKey)) return false;
        PgpKey pgpKey = (PgpKey) o;
        return keyId.equals(pgpKey.keyId) && userId.equals(pgpKey.userId);
    }

    @Override
    public int hashCode() {
        int result = keyId.hashCode();
        result = 31 * result + userId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PgpKey{" +
            "keyId='" + keyId + '\'' +
            ", userId='" + userId + '\'' +
            '}';
    }
}
