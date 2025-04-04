package consulo.credentialStorage.impl.internal.encrypt;

public interface EncryptionSupport {
    byte[] encrypt(byte[] data);

    byte[] decrypt(byte[] data);
}
