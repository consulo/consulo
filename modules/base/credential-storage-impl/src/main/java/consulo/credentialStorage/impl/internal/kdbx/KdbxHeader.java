package consulo.credentialStorage.impl.internal.kdbx;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import consulo.credentialStorage.impl.internal.CredentialStoreUtil;
import consulo.util.io.DigestUtil;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * Represents the header portion of a KeePass KDBX file.
 * It provides methods to create encryption/decryption streams and contains a hash of its own serialization.
 */
public class KdbxHeader {
    private static final UUID AES_CIPHER = UUID.fromString("31C1F2E6-BF71-4350-BE58-05216AFC5AFF");
    private static final int FILE_VERSION_CRITICAL_MASK = 0xFFFF0000;
    private static final int SIG1 = 0x9AA2D903;
    private static final int SIG2 = 0xB54BFB67;
    private static final int FILE_VERSION_32 = 0x00030001;

    private static final class HeaderType {
        static final byte END = 0;
        static final byte COMMENT = 1;
        static final byte CIPHER_ID = 2;
        static final byte COMPRESSION_FLAGS = 3;
        static final byte MAIN_SEED = 4;
        static final byte TRANSFORM_SEED = 5;
        static final byte TRANSFORM_ROUNDS = 6;
        static final byte ENCRYPTION_IV = 7;
        static final byte PROTECTED_STREAM_KEY = 8;
        static final byte STREAM_START_BYTES = 9;
        static final byte INNER_RANDOM_STREAM_ID = 10;
    }

    public enum CompressionFlags {
        NONE,
        GZIP
    }

    public enum ProtectedStreamAlgorithm {
        NONE,
        ARC_FOUR,
        SALSA_20,
        CHA_CHA
    }

    // Instance fields
    private UUID cipherUuid = AES_CIPHER;
    private CompressionFlags compressionFlags = CompressionFlags.GZIP;
    private byte[] mainSeed = new byte[0]; // Equivalent to ArrayUtilRt.EMPTY_BYTE_ARRAY
    private byte[] transformSeed = new byte[0];
    private long transformRounds = 6000;
    private byte[] encryptionIv = new byte[0];
    private byte[] protectedStreamKey = new byte[0];
    private ProtectedStreamAlgorithm protectedStreamAlgorithm = ProtectedStreamAlgorithm.CHA_CHA;
    private byte[] streamStartBytes = new byte[32];
    private byte[] headerHash = null;

    // Constructors

    public KdbxHeader() {
        // Default constructor
    }

    public KdbxHeader(InputStream inputStream) {
        readKdbxHeader(inputStream);
    }

    public KdbxHeader(SecureRandom random) {
        // Assume generateBytes(random, length) returns a byte array of given length.
        this.mainSeed = CredentialStoreUtil.generateBytes(random, 32);
        this.transformSeed = CredentialStoreUtil.generateBytes(random, 32);
        this.encryptionIv = CredentialStoreUtil.generateBytes(random, 16);
        this.protectedStreamKey = CredentialStoreUtil.generateBytes(random, 64);
    }

    public byte[] getHeaderHash() {
        return headerHash;
    }

    public byte[] getProtectedStreamKey() {
        return protectedStreamKey;
    }

    public CompressionFlags getCompressionFlags() {
        return compressionFlags;
    }

    public byte[] getStreamStartBytes() {
        return streamStartBytes;
    }

    public ProtectedStreamAlgorithm getProtectedStreamAlgorithm() {
        return protectedStreamAlgorithm;
    }

    /**
     * Create a decrypted input stream using the supplied digest and this header.
     */
    public InputStream createDecryptedStream(byte[] digest, InputStream inputStream) {
        byte[] finalKeyDigest = getFinalKeyDigest(digest, mainSeed, transformSeed, transformRounds);
        Cipher cipher = createChipper(false, finalKeyDigest, encryptionIv);
        return new CipherInputStream(inputStream, cipher);
    }

    /**
     * Create an encrypted output stream using the supplied digest and this header.
     */
    public OutputStream createEncryptedStream(byte[] digest, OutputStream outputStream) throws IOException {
        byte[] finalKeyDigest = getFinalKeyDigest(digest, mainSeed, transformSeed, transformRounds);
        Cipher cipher = createChipper(true, finalKeyDigest, encryptionIv);
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
        cipherOutputStream.write(streamStartBytes);
        HashedBlockOutputStream out = new HashedBlockOutputStream(cipherOutputStream);
        if (compressionFlags == CompressionFlags.GZIP) {
            return new GZIPOutputStream(out, HashedBlockOutputStream.BLOCK_SIZE);
        }
        else {
            return out;
        }
    }

    private void setCipherUuid(byte[] uuid) {
        ByteBuffer b = ByteBuffer.wrap(uuid);
        UUID incoming = new UUID(b.getLong(), b.getLong(8));
        if (!incoming.equals(AES_CIPHER)) {
            throw new IllegalStateException("Unknown Cipher UUID " + incoming);
        }
        this.cipherUuid = incoming;
    }

    /**
     * Populates this KdbxHeader from the given input stream.
     */
    private void readKdbxHeader(InputStream inputStream) {
        try {
            java.security.MessageDigest digest = DigestUtil.sha256(); // Adjust as needed
            DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);
            LittleEndianDataInputStream input = new LittleEndianDataInputStream(digestInputStream);

            if (!readSignature(input)) {
                throw new KdbxException("Bad signature");
            }
            if (!verifyFileVersion(input)) {
                throw new IllegalStateException("File version did not match");
            }

            while (true) {
                byte headerType = input.readByte();
                if (headerType == HeaderType.END) {
                    break;
                }
                switch (headerType) {
                    case HeaderType.COMMENT:
                        readHeaderData(input);
                        break;
                    case HeaderType.CIPHER_ID:
                        setCipherUuid(readHeaderData(input));
                        break;
                    case HeaderType.COMPRESSION_FLAGS:
                        int compFlag = readIntHeaderData(input);
                        this.compressionFlags = CompressionFlags.values()[compFlag];
                        break;
                    case HeaderType.MAIN_SEED:
                        this.mainSeed = readHeaderData(input);
                        break;
                    case HeaderType.TRANSFORM_SEED:
                        this.transformSeed = readHeaderData(input);
                        break;
                    case HeaderType.TRANSFORM_ROUNDS:
                        this.transformRounds = readLongHeaderData(input);
                        break;
                    case HeaderType.ENCRYPTION_IV:
                        this.encryptionIv = readHeaderData(input);
                        break;
                    case HeaderType.PROTECTED_STREAM_KEY:
                        this.protectedStreamKey = readHeaderData(input);
                        break;
                    case HeaderType.STREAM_START_BYTES:
                        this.streamStartBytes = readHeaderData(input);
                        break;
                    case HeaderType.INNER_RANDOM_STREAM_ID:
                        int alg = readIntHeaderData(input);
                        this.protectedStreamAlgorithm = ProtectedStreamAlgorithm.values()[alg];
                        break;
                    default:
                        throw new IllegalStateException("Unknown File Header");
                }
            }
            // Consume any trailing header data following END.
            readHeaderData(input);
            this.headerHash = digest.digest();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes this KdbxHeader to the provided output stream.
     */
    public void writeKdbxHeader(OutputStream outputStream) {
        try {
            java.security.MessageDigest messageDigest = DigestUtil.sha256(); // Adjust as needed
            DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, messageDigest);
            LittleEndianDataOutputStream output = new LittleEndianDataOutputStream(digestOutputStream);

            // Write magic numbers
            output.writeInt(SIG1);
            output.writeInt(SIG2);
            output.writeInt(FILE_VERSION_32);

            output.writeByte(HeaderType.CIPHER_ID);
            output.writeShort(16);
            byte[] b = new byte[16];
            ByteBuffer bb = ByteBuffer.wrap(b);
            bb.putLong(cipherUuid.getMostSignificantBits());
            bb.putLong(8, cipherUuid.getLeastSignificantBits());
            output.write(b);

            output.writeByte(HeaderType.COMPRESSION_FLAGS);
            output.writeShort(4);
            output.writeInt(compressionFlags.ordinal());

            output.writeByte(HeaderType.MAIN_SEED);
            output.writeShort(mainSeed.length);
            output.write(mainSeed);

            output.writeByte(HeaderType.TRANSFORM_SEED);
            output.writeShort(transformSeed.length);
            output.write(transformSeed);

            output.writeByte(HeaderType.TRANSFORM_ROUNDS);
            output.writeShort(8);
            output.writeLong(transformRounds);

            output.writeByte(HeaderType.ENCRYPTION_IV);
            output.writeShort(encryptionIv.length);
            output.write(encryptionIv);

            output.writeByte(HeaderType.PROTECTED_STREAM_KEY);
            output.writeShort(protectedStreamKey.length);
            output.write(protectedStreamKey);

            output.writeByte(HeaderType.STREAM_START_BYTES);
            output.writeShort(streamStartBytes.length);
            output.write(streamStartBytes);

            output.writeByte(HeaderType.INNER_RANDOM_STREAM_ID);
            output.writeShort(Integer.SIZE / Byte.SIZE);
            output.writeInt(protectedStreamAlgorithm.ordinal());

            output.writeByte(HeaderType.END);
            output.writeShort(0);

            this.headerHash = digestOutputStream.getMessageDigest().digest();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Static helper methods ---

    private static boolean readSignature(LittleEndianDataInputStream input) throws IOException {
        return input.readInt() == SIG1 && input.readInt() == SIG2;
    }

    private static boolean verifyFileVersion(LittleEndianDataInputStream input) throws IOException {
        int fileVersion = input.readInt();
        return (fileVersion & FILE_VERSION_CRITICAL_MASK) <= (FILE_VERSION_32 & FILE_VERSION_CRITICAL_MASK);
    }

    private static int readIntHeaderData(LittleEndianDataInputStream input) throws IOException {
        int fieldLength = input.readShort();
        if (fieldLength != 4) {
            throw new IllegalStateException("Int required but length was " + fieldLength);
        }
        return input.readInt();
    }

    private static long readLongHeaderData(LittleEndianDataInputStream input) throws IOException {
        int fieldLength = input.readShort();
        if (fieldLength != 8) {
            throw new IllegalStateException("Long required but length was " + fieldLength);
        }
        return input.readLong();
    }

    private static byte[] readHeaderData(LittleEndianDataInputStream input) throws IOException {
        int length = input.readShort();
        byte[] value = new byte[length];
        input.readFully(value);
        return value;
    }

    private static byte[] getFinalKeyDigest(byte[] key, byte[] mainSeed, byte[] transformSeed, long transformRounds) {
        AESEngine engine = new AESEngine();
        engine.init(true, new KeyParameter(transformSeed));
        byte[] transformedKey = Arrays.copyOf(key, key.length);
        for (long rounds = 0; rounds < transformRounds; rounds++) {
            engine.processBlock(transformedKey, 0, transformedKey, 0);
            engine.processBlock(transformedKey, 16, transformedKey, 16);
        }
        java.security.MessageDigest md = DigestUtil.sha256();
        byte[] transformedKeyDigest = md.digest(transformedKey);
        md.update(mainSeed);
        return md.digest(transformedKeyDigest);
    }

    private static Cipher createChipper(boolean forEncryption, byte[] keyData, byte[] ivData) {
        try {
            IvParameterSpec iv = new IvParameterSpec(ivData);
            SecretKeySpec secretKey = new SecretKeySpec(keyData, 0, keyData.length, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(forEncryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, secretKey, iv);
            return cipher;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
