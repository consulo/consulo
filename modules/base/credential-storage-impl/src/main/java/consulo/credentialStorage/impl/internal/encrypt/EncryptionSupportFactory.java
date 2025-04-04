package consulo.credentialStorage.impl.internal.encrypt;

import consulo.credentialStorage.impl.internal.gpg.PgpKeyEncryptionSupport;
import consulo.credentialStorage.impl.internal.windows.WindowsCrypt32EncryptionSupport;
import consulo.application.util.SystemInfo;
import consulo.util.jna.JnaLoader;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public final class EncryptionSupportFactory {
    private static final Key builtInEncryptionKey = new SecretKeySpec(new byte[]{
        (byte) 0x50, (byte) 0x72, (byte) 0x6f, (byte) 0x78,
        (byte) 0x79, (byte) 0x20, (byte) 0x43, (byte) 0x6f,
        (byte) 0x6e, (byte) 0x66, (byte) 0x69, (byte) 0x67,
        (byte) 0x20, (byte) 0x53, (byte) 0x65, (byte) 0x63
    }, "AES");

    public static EncryptionType getDefaultEncryptionType() {
        return SystemInfo.isWindows ? EncryptionType.CRYPT_32 : EncryptionType.BUILT_IN;
    }

    public static EncryptionSupport createEncryptionSupport(EncryptionSpec spec) {
        switch (spec.getType()) {
            case BUILT_IN:
                return createBuiltInOrCrypt32EncryptionSupport(false);
            case CRYPT_32:
                return createBuiltInOrCrypt32EncryptionSupport(true);
            case PGP_KEY:
                return new PgpKeyEncryptionSupport(spec);
            default:
                throw new IllegalArgumentException("Unknown encryption type: " + spec.getType());
        }
    }

    public static EncryptionSupport createBuiltInOrCrypt32EncryptionSupport(boolean isCrypt32) {
        if (isCrypt32) {
            if (!SystemInfo.isWindows) {
                throw new IllegalArgumentException("Crypt32 encryption is supported only on Windows");
            }
            if (JnaLoader.isLoaded()) {
                return new WindowsCrypt32EncryptionSupport(builtInEncryptionKey);
            }
        }
        return new AesEncryptionSupport(builtInEncryptionKey);
    }
}
