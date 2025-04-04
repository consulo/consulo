package consulo.credentialStorage.impl.internal.kdbx;

import consulo.credentialStorage.impl.internal.CredentialStoreUtil;
import consulo.util.jdom.JDOMUtil;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * KeePassDatabase provides functionality for loading, saving, and protecting values in a KeePass database.
 */
public class KeePassDatabase {
    // --- Static constants and helper methods ---

    public static final String[] LOCATION_CHANGED = new String[]{"Times", "LocationChanged"};
    public static final String[] USAGE_COUNT_ELEMENT_NAME = new String[]{"Times", "UsageCount"};
    public static final String[] EXPIRES_ELEMENT_NAME = new String[]{"Times", "Expires"};
    public static final String[] ICON_ELEMENT_NAME = new String[]{"IconID"};
    public static final String[] UUID_ELEMENT_NAME = new String[]{"UUID"};
    public static final String[] LAST_MODIFICATION_TIME_ELEMENT_NAME = new String[]{"Times", "LastModificationTime"};
    public static final String[] CREATION_TIME_ELEMENT_NAME = new String[]{"Times", "CreationTime"};
    public static final String[] LAST_ACCESS_TIME_ELEMENT_NAME = new String[]{"Times", "LastAccessTime"};
    public static final String[] EXPIRY_TIME_ELEMENT_NAME = new String[]{"Times", "ExpiryTime"};

    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static SkippingStreamCipher createRandomlyInitializedChaCha7539Engine(SecureRandom secureRandom) {
        ChaCha7539Engine engine = new ChaCha7539Engine();
        initCipherRandomly(secureRandom, engine);
        return engine;
    }

    private static void initCipherRandomly(SecureRandom secureRandom, SkippingStreamCipher engine) {
        // generateBytes() is assumed to be a utility method that returns a byte array of the given length.
        byte[] keyBytes = CredentialStoreUtil.generateBytes(secureRandom, 32);
        KeyParameter keyParameter = new KeyParameter(keyBytes);
        byte[] iv = CredentialStoreUtil.generateBytes(secureRandom, 12);
        engine.init(true, new ParametersWithIV(keyParameter, iv));
    }

    public static String formattedNow() {
        return LocalDateTime.now(ZoneOffset.UTC).format(dateFormatter);
    }

    public static String base64FromUuid(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    private static Element createEmptyDatabase() {
        String creationDate = formattedNow();
        // JDOMUtil.load() expects a String containing XML.
        String xml = "<KeePassFile>\n" +
            "  <Meta>\n" +
            "    <Generator>Consulo</Generator>\n" +
            "    <HeaderHash></HeaderHash>\n" +
            "    <DatabaseName>New Database</DatabaseName>\n" +
            "    <DatabaseNameChanged>" + creationDate + "</DatabaseNameChanged>\n" +
            "    <DatabaseDescription>Empty Database</DatabaseDescription>\n" +
            "    <DatabaseDescriptionChanged>" + creationDate + "</DatabaseDescriptionChanged>\n" +
            "    <DefaultUserName/>\n" +
            "    <DefaultUserNameChanged>" + creationDate + "</DefaultUserNameChanged>\n" +
            "    <MaintenanceHistoryDays>365</MaintenanceHistoryDays>\n" +
            "    <Color/>\n" +
            "    <MasterKeyChanged>" + creationDate + "</MasterKeyChanged>\n" +
            "    <MasterKeyChangeRec>-1</MasterKeyChangeRec>\n" +
            "    <MasterKeyChangeForce>-1</MasterKeyChangeForce>\n" +
            "    <MemoryProtection>\n" +
            "      <ProtectTitle>False</ProtectTitle>\n" +
            "      <ProtectUserName>False</ProtectUserName>\n" +
            "      <ProtectPassword>True</ProtectPassword>\n" +
            "      <ProtectURL>False</ProtectURL>\n" +
            "      <ProtectNotes>False</ProtectNotes>\n" +
            "    </MemoryProtection>\n" +
            "    <CustomIcons/>\n" +
            "    <RecycleBinEnabled>True</RecycleBinEnabled>\n" +
            "    <RecycleBinUUID>AAAAAAAAAAAAAAAAAAAAAA==</RecycleBinUUID>\n" +
            "    <RecycleBinChanged>" + creationDate + "</RecycleBinChanged>\n" +
            "    <EntryTemplatesGroup>AAAAAAAAAAAAAAAAAAAAAA==</EntryTemplatesGroup>\n" +
            "    <EntryTemplatesGroupChanged>" + creationDate + "</EntryTemplatesGroupChanged>\n" +
            "    <LastSelectedGroup>AAAAAAAAAAAAAAAAAAAAAA==</LastSelectedGroup>\n" +
            "    <LastTopVisibleGroup>AAAAAAAAAAAAAAAAAAAAAA==</LastTopVisibleGroup>\n" +
            "    <HistoryMaxItems>10</HistoryMaxItems>\n" +
            "    <HistoryMaxSize>6291456</HistoryMaxSize>\n" +
            "    <Binaries/>\n" +
            "    <CustomData/>\n" +
            "  </Meta>\n" +
            "</KeePassFile>";
        try {
            return JDOMUtil.load(new StringReader(xml));
        }
        catch (JDOMException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<String[], ValueCreator> mandatoryEntryElements = new LinkedHashMap<>();

    static {
        mandatoryEntryElements.put(UUID_ELEMENT_NAME, new UuidValueCreator());
        mandatoryEntryElements.put(ICON_ELEMENT_NAME, new ConstantValueCreator("0"));
        mandatoryEntryElements.put(CREATION_TIME_ELEMENT_NAME, new DateValueCreator());
        mandatoryEntryElements.put(LAST_MODIFICATION_TIME_ELEMENT_NAME, new DateValueCreator());
        mandatoryEntryElements.put(LAST_ACCESS_TIME_ELEMENT_NAME, new DateValueCreator());
        mandatoryEntryElements.put(EXPIRY_TIME_ELEMENT_NAME, new DateValueCreator());
        mandatoryEntryElements.put(EXPIRES_ELEMENT_NAME, new ConstantValueCreator("False"));
        mandatoryEntryElements.put(USAGE_COUNT_ELEMENT_NAME, new ConstantValueCreator("0"));
        mandatoryEntryElements.put(LOCATION_CHANGED, new DateValueCreator());
    }

    private static void ensureElements(Element element, Map<String[], ValueCreator> childElements) {
        for (Map.Entry<String[], ValueCreator> entry : childElements.entrySet()) {
            String[] elementPath = entry.getKey();
            ValueCreator valueCreator = entry.getValue();
            Element result = findElement(element, elementPath);
            if (result == null) {
                Element currentElement = element;
                for (String elementName : elementPath) {
                    currentElement = getOrCreateChild(currentElement, elementName);
                }
                currentElement.setText(valueCreator.getValue());
            }
        }
    }

    private static Element findElement(Element element, String[] elementPath) {
        Element result = element;
        for (String elementName : elementPath) {
            result = result.getChild(elementName);
            if (result == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * Helper method to obtain a child element with the specified name.
     * If it does not exist, it is created and added to the parent.
     */
    private static Element getOrCreateChild(Element parent, String childName) {
        Element child = parent.getChild(childName);
        if (child == null) {
            child = new Element(childName);
            parent.addContent(child);
        }
        return child;
    }

    // --- Instance fields and methods of KeePassDatabase ---

    private Element rootElement;
    private SkippingStreamCipher secureStringCipher;
    private volatile boolean isDirty = false;
    private final KdbxGroup rootGroup;

    /**
     * Constructs a KeePassDatabase with an existing XML root element.
     * If null is passed, an empty database is created.
     */
    public KeePassDatabase(Element rootElement) {
        this.rootElement = (rootElement != null) ? rootElement : createEmptyDatabase();
        // Get or create the Root element.
        this.rootElement = getOrCreateChild(this.rootElement, KdbxDbElementNames.ROOT);
        Element groupElement = this.rootElement.getChild(KdbxDbElementNames.GROUP);
        if (groupElement == null) {
            // createGroup(this, null) is assumed to be a factory method that creates a new KdbxGroup.
            this.rootGroup = KdbxGroup.createGroup(this, null);
            this.rootGroup.setName(KdbxDbElementNames.ROOT);
            this.rootElement.addContent(this.rootGroup.getElement());
        }
        else {
            this.rootGroup = new KdbxGroup(groupElement, this, null);
        }
        // Initialize secureStringCipher lazily.
        this.secureStringCipher = createRandomlyInitializedChaCha7539Engine(CredentialStoreUtil.createSecureRandom());
    }

    public KeePassDatabase() {
        this(null);
    }

    /**
     * Protects the given value by wrapping it in a StringProtectedByStreamCipher.
     */
    public StringProtectedByStreamCipher protectValue(CharSequence value) {
        return new StringProtectedByStreamCipher(value, secureStringCipher);
    }

    /**
     * Saves the database. This method is synchronized.
     */
    public synchronized void save(KeePassCredentials credentials, OutputStream outputStream, SecureRandom secureRandom) throws Exception {
        KdbxHeader kdbxHeader = new KdbxHeader(secureRandom);
        kdbxHeader.writeKdbxHeader(outputStream);

        Element metaElement = getOrCreateChild(rootElement, "Meta");
        getOrCreateChild(metaElement, "HeaderHash").setText(Base64.getEncoder().encodeToString(kdbxHeader.getHeaderHash()));
        getOrCreateChild(getOrCreateChild(metaElement, "MemoryProtection"), "ProtectPassword").setText("True");

        // Create an encrypted stream, obtain its writer, and print the XML.
        OutputStream encryptedStream = kdbxHeader.createEncryptedStream(credentials.getKey(), outputStream);

        try (Writer writer = new OutputStreamWriter(encryptedStream, StandardCharsets.UTF_8)) {
            new ProtectedXmlWriter(Kdbx.createChaCha20StreamCipher(kdbxHeader.getProtectedStreamKey()))
                .printElement(writer, rootElement, 0);
        }

        // Reinitialize the secure string cipher.
        initCipherRandomly(secureRandom, secureStringCipher);
        isDirty = false;
    }

    /**
     * Creates a new entry with the specified title.
     */
    public KdbxEntry createEntry(String title) {
        Element element = new Element(KdbxDbElementNames.ENTRY);
        ensureElements(element, mandatoryEntryElements);
        KdbxEntry result = new KdbxEntry(element, this, null);
        result.setTitle(title);
        return result;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public KdbxGroup getRootGroup() {
        return rootGroup;
    }
}
