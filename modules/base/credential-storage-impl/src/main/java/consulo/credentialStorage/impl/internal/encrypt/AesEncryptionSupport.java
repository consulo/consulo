package consulo.credentialStorage.impl.internal.encrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.Key;

public class AesEncryptionSupport implements EncryptionSupport {
    private final Key key;

    public AesEncryptionSupport(Key key) {
        this.key = key;
    }

    private static byte[] encrypt(byte[] message, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] body = cipher.doFinal(message, 0, message.length);
        byte[] iv = cipher.getIV();

        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + body.length);
        byteBuffer.putInt(iv.length);
        byteBuffer.put(iv);
        byteBuffer.put(body);
        return byteBuffer.array();
    }

    private static byte[] decrypt(byte[] data, Key key) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        int ivLength = byteBuffer.getInt();
        int ivStart = byteBuffer.position();
        IvParameterSpec ivSpec = new IvParameterSpec(data, ivStart, ivLength);
        int dataOffset = ivStart + ivLength;
        int bodyLength = data.length - dataOffset;
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        return cipher.doFinal(data, dataOffset, bodyLength);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        try {
            return encrypt(data, key);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data) {
        try {
            return decrypt(data, key);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
