package consulo.credentialStorage.impl.internal.kdbx;

import consulo.credentialStorage.OneTimeString;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.jdom.Text;

import java.util.Base64;

public class ProtectedValue extends Text implements SecureString {
    private byte[] encryptedValue;
    private int position;
    private SkippingStreamCipher streamCipher;

    public ProtectedValue(byte[] encryptedValue, int position, SkippingStreamCipher streamCipher) {
        // Pass an empty string to the Text constructor since its text is not used.
        super("");
        this.encryptedValue = encryptedValue;
        this.position = position;
        this.streamCipher = streamCipher;
    }

    @Override
    public synchronized OneTimeString get(boolean clearable) {
        byte[] output = new byte[encryptedValue.length];
        decryptInto(output);
        try {
            return OneTimeString.fromByteArray(output, clearable);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void setNewStreamCipher(SkippingStreamCipher newStreamCipher) {
        byte[] value = encryptedValue;
        decryptInto(value);
        synchronized (newStreamCipher) {
            this.position = (int) newStreamCipher.getPosition();
            newStreamCipher.processBytes(value, 0, value.length, value, 0);
        }
        this.streamCipher = newStreamCipher;
    }

    private synchronized void decryptInto(byte[] out) {
        synchronized (streamCipher) {
            streamCipher.seekTo((long) position);
            streamCipher.processBytes(encryptedValue, 0, encryptedValue.length, out, 0);
        }
    }

    @Override
    public String getText() {
        throw new IllegalStateException("encodeToBase64 must be used for serialization");
    }

    public String encodeToBase64() {
        if (encryptedValue.length == 0) {
            return "";
        }
        else {
            return Base64.getEncoder().encodeToString(encryptedValue);
        }
    }
}
