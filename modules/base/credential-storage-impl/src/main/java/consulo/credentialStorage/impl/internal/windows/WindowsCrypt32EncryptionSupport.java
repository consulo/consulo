package consulo.credentialStorage.impl.internal.windows;

import consulo.credentialStorage.impl.internal.encrypt.AesEncryptionSupport;

import java.security.Key;

public class WindowsCrypt32EncryptionSupport extends AesEncryptionSupport {
    public WindowsCrypt32EncryptionSupport(Key key) {
        super(key);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        byte[] aesEncrypted = super.encrypt(data);
        return WindowsCryptUtils.protect(aesEncrypted);
    }

    @Override
    public byte[] decrypt(byte[] data) {
        byte[] unprotected = WindowsCryptUtils.unprotect(data);
        return super.decrypt(unprotected);
    }
}
