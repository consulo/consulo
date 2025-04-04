package consulo.credentialStorage.impl.internal.gpg;

import consulo.process.ExecutionException;

public interface GpgToolWrapper {
    String listSecretKeys() throws ExecutionException;

    byte[] encrypt(byte[] data, String recipient);

    byte[] decrypt(byte[] data);

    default String version() {
        return "";
    }
}
