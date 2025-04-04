package consulo.credentialStorage.impl.internal.keePass;

import consulo.credentialStorage.impl.internal.encrypt.EncryptionSpec;

public class MainKey {
    // The main key value; may be cleared.
    private byte[] value;
    // Whether the main key was auto-generated.
    public final boolean isAutoGenerated;
    public final EncryptionSpec encryptionSpec;

    public MainKey(byte[] value, boolean isAutoGenerated, EncryptionSpec encryptionSpec) {
        this.value = value;
        this.isAutoGenerated = isAutoGenerated;
        this.encryptionSpec = encryptionSpec;
    }

    public byte[] getValue() {
        return value;
    }

    /**
     * Clears the key value (fills with zeros and sets to null) to avoid sensitive data in memory.
     */
    public void clear() {
        if (value != null) {
            for (int i = 0; i < value.length; i++) {
                value[i] = 0;
            }
            value = null;
        }
    }
}
