package consulo.credentialStorage.impl.internal.gpg;

import consulo.credentialStorage.impl.internal.encrypt.EncryptionSpec;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionSupport;

public class PgpKeyEncryptionSupport implements EncryptionSupport {
    private final EncryptionSpec encryptionSpec;

    public PgpKeyEncryptionSupport(EncryptionSpec encryptionSpec) {
        this.encryptionSpec = encryptionSpec;
    }

    @Override
    public byte[] encrypt(byte[] data) {
        return new Pgp().encrypt(data, encryptionSpec.getPgpKeyId());
    }

    @Override
    public byte[] decrypt(byte[] data) {
        return new Pgp().decrypt(data);
    }
}
