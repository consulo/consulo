package consulo.credentialStorage.impl.internal.kdbx;

import consulo.util.io.DigestUtil;
import consulo.util.jdom.JDOMUtil;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.engines.Salsa20Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public final class Kdbx {
    private Kdbx() {
    }

    /**
     * Loads a KeePass database from the given file using the provided credentials.
     *
     * @param file        the path to the KeePass database file
     * @param credentials the credentials to decrypt the database
     * @return a KeePassDatabase instance
     * @throws IncorrectMainPasswordException if the provided main password is incorrect
     * @throws IOException                    if an I/O error occurs
     */
    public static KeePassDatabase loadKdbx(Path file, KeePassCredentials credentials)
        throws IncorrectMainPasswordException, IOException {
        try (InputStream is = new BufferedInputStream(java.nio.file.Files.newInputStream(file))) {
            return readKeePassDatabase(credentials, is);
        }
    }

    private static KeePassDatabase readKeePassDatabase(KeePassCredentials credentials, InputStream inputStream)
        throws IncorrectMainPasswordException, IOException {
        // Create the KDBX header from the input stream.
        KdbxHeader kdbxHeader = new KdbxHeader(inputStream);
        InputStream decryptedInputStream = kdbxHeader.createDecryptedStream(credentials.getKey(), inputStream);

        // Read the stream start bytes and verify against the header.
        byte[] startBytes = decryptedInputStream.readNBytes(32);
        if (!Arrays.equals(startBytes, kdbxHeader.getStreamStartBytes())) {
            throw new IncorrectMainPasswordException();
        }

        // Wrap the decrypted stream with a HashedBlockInputStream.
        InputStream resultInputStream = new HashedBlockInputStream(decryptedInputStream);
        if (kdbxHeader.getCompressionFlags() == KdbxHeader.CompressionFlags.GZIP) {
            resultInputStream = new GZIPInputStream(resultInputStream);
        }

        Element element;
        try {
            element = JDOMUtil.load(resultInputStream);
        }
        catch (JDOMException e) {
            throw new RuntimeException(e);
        }
        
        Element rootElement = element.getChild(KdbxDbElementNames.ROOT);
        if (rootElement != null) {
            SkippingStreamCipher streamCipher;
            if (kdbxHeader.getProtectedStreamAlgorithm() == KdbxHeader.ProtectedStreamAlgorithm.CHA_CHA) {
                streamCipher = createChaCha20StreamCipher(kdbxHeader.getProtectedStreamKey());
            }
            else {
                streamCipher = createSalsa20StreamCipher(kdbxHeader.getProtectedStreamKey());
            }
            new XmlProtectedValueTransformer(streamCipher).processEntries(rootElement);
        }
        return new KeePassDatabase(element);
    }

    private static final byte[] SALSA20_IV = new byte[]{(byte) -24, 48, 9, 75, (byte) -105, 32, 93, 42};

    /**
     * Creates a Salsa20 stream cipher initialized with a key derived from the provided key bytes.
     *
     * @param key the key material
     * @return an initialized SkippingStreamCipher instance
     */
    public static SkippingStreamCipher createSalsa20StreamCipher(byte[] key) {
        Salsa20Engine streamCipher = new Salsa20Engine();
        byte[] keyDigest = DigestUtil.sha256().digest(key);
        KeyParameter keyParameter = new KeyParameter(keyDigest);
        // For encryption/decryption, the 'forEncryption' flag doesn't matter for Salsa20.
        streamCipher.init(true, new ParametersWithIV(keyParameter, SALSA20_IV));
        return streamCipher;
    }

    /**
     * Creates a ChaCha20 stream cipher initialized with a key and nonce derived from the provided key bytes.
     *
     * @param key the key material
     * @return an initialized SkippingStreamCipher instance
     */
    public static SkippingStreamCipher createChaCha20StreamCipher(byte[] key) {
        ChaCha7539Engine streamCipher = new ChaCha7539Engine();
        byte[] keyHash = DigestUtil.sha2_512().digest(key);
        KeyParameter keyParameter = new KeyParameter(Arrays.copyOf(keyHash, 32));
        // The nonce is taken as bytes 32..43 (12 bytes) of the hash.
        streamCipher.init(true, new ParametersWithIV(keyParameter, Arrays.copyOfRange(keyHash, 32, 44)));
        return streamCipher;
    }
}
