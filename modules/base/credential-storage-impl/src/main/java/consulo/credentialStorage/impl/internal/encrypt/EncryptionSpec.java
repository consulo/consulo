package consulo.credentialStorage.impl.internal.encrypt;

import consulo.credentialStorage.impl.internal.encrypt.EncryptionType;

public class EncryptionSpec {
    private final EncryptionType type;
    private final String pgpKeyId;

    public EncryptionSpec(EncryptionType type, String pgpKeyId) {
        this.type = type;
        this.pgpKeyId = pgpKeyId;
    }

    public EncryptionType getType() {
        return type;
    }

    public String getPgpKeyId() {
        return pgpKeyId;
    }
}
