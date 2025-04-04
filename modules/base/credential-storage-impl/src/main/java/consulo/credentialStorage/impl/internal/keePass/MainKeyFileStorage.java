package consulo.credentialStorage.impl.internal.keePass;

import consulo.credentialStorage.impl.internal.encrypt.EncryptionSpec;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionType;
import consulo.credentialStorage.impl.internal.SharedLogger;
import consulo.application.Application;
import consulo.application.io.SafeOutputStreamFactory;
import consulo.application.util.SystemInfo;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static consulo.credentialStorage.impl.internal.encrypt.EncryptionSupportFactory.*;

public class MainKeyFileStorage {
    public static final String MAIN_KEY_FILE_NAME = "c.pwd";
    private static final String OLD_MAIN_PASSWORD_FILE_NAME = "pdb.pwd";

    // The file where the main key is stored.
    public final Path passwordFile;

    public MainKeyFileStorage(Path passwordFile) {
        this.passwordFile = passwordFile;
    }

    public byte[] load() {
        byte[] data;
        boolean isOld = false;
        try {
            data = Files.readAllBytes(passwordFile);
        }
        catch (NoSuchFileException e) {
            try {
                data = Files.readAllBytes(passwordFile.getParent().resolve(OLD_MAIN_PASSWORD_FILE_NAME));
            }
            catch (IOException ex) {
                return null;
            }
            isOld = true;
        }
        catch (IOException e) {
            return null;
        }

        try {
            byte[] decrypted;
            if (isOld) {
                decrypted = createBuiltInOrCrypt32EncryptionSupport(SystemInfo.isWindows).decrypt(data);
                fillArray(data, (byte) 0);
            }
            else {
                decrypted = decryptMainKey(data);
                if (decrypted == null) {
                    return null;
                }
            }
            fillArray(data, (byte) 0);
            return decrypted;
        }
        catch (Exception e) {
            String fileContent;
            if (isOld) {
                fileContent = Base64.getEncoder().encodeToString(data);
            }
            else {
                fileContent = new String(data, StandardCharsets.UTF_8);
            }
            SharedLogger.LOG.warn("Cannot decrypt main key, file content:\n" + fileContent, e);
            return null;
        }
    }

    private byte[] decryptMainKey(byte[] data) {
        EncryptionType encryptionType = null;
        byte[] value = null;
        List<NodeTuple> nodes = createMainKeyReader(data);
        for (NodeTuple node : nodes) {
            if (node.getKeyNode() instanceof ScalarNode && node.getValueNode() instanceof ScalarNode) {
                ScalarNode keyNode = (ScalarNode) node.getKeyNode();
                ScalarNode valueNode = (ScalarNode) node.getValueNode();
                String propertyValue = valueNode.getValue();
                if (propertyValue == null) {
                    continue;
                }
                switch (keyNode.getValue()) {
                    case "encryption":
                        encryptionType = EncryptionType.valueOf(propertyValue.toUpperCase());
                        break;
                    case "value":
                        value = Base64.getDecoder().decode(propertyValue);
                        break;
                }
            }
        }
        if (encryptionType == null) {
            SharedLogger.LOG.error("encryption type not specified in " + passwordFile +
                ", default one will be used (file content:\n" + new String(data, StandardCharsets.UTF_8) + ")");
            encryptionType = getDefaultEncryptionType();
        }
        if (value == null) {
            SharedLogger.LOG.error("password not specified in " + passwordFile +
                ", automatically generated will be used (file content:\n" + new String(data, StandardCharsets.UTF_8) + ")");
            return null;
        }
        // PGP key id is not stored in the main key file; createEncryptionSupport is used only for decryption (pgpKeyId is null)
        return createEncryptionSupport(new EncryptionSpec(encryptionType, null)).decrypt(value);
    }

    public void save(MainKey key) {
        if (key == null) {
            try {
                Files.delete(passwordFile);
            }
            catch (IOException e) {
                // Ignore deletion failure.
            }
            return;
        }
        byte[] encrypted = createEncryptionSupport(key.encryptionSpec).encrypt(key.getValue());
        byte[] encodedValue = Base64.getEncoder().encode(encrypted);
        key.clear();

        BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream();
        EncryptionType encryptionType = key.encryptionSpec.getType();
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.append("encryption: ").append(encryptionType.name()).append('\n')
                .append("isAutoGenerated: ").append(Boolean.toString(key.isAutoGenerated)).append('\n')
                .append("value: !!binary ");
        }
        catch (IOException e) {
            // Should not occur with ByteArrayOutputStream.
        }

        SafeOutputStreamFactory streamFactory = Application.get().getInstance(SafeOutputStreamFactory.class);

        try (OutputStream os = streamFactory.create(passwordFile)) {
            os.write(out.getInternalBuffer(), 0, out.size());
            os.write(encodedValue);
            fillArray(encodedValue, (byte) 0);
        }
        catch (IOException e) {
            // Handle exception as needed.
        }
    }

    private void fillArray(byte[] array, byte value) {
        if (array != null) {
            Arrays.fill(array, value);
        }
    }

    private boolean readMainKeyIsAutoGeneratedMetadata(byte[] data) {
        boolean isAutoGenerated = true;
        List<NodeTuple> nodes = createMainKeyReader(data);
        for (NodeTuple node : nodes) {
            if (node.getKeyNode() instanceof ScalarNode && node.getValueNode() instanceof ScalarNode) {
                ScalarNode keyNode = (ScalarNode) node.getKeyNode();
                ScalarNode valueNode = (ScalarNode) node.getValueNode();
                String propertyValue = valueNode.getValue();
                if (propertyValue == null) {
                    continue;
                }
                if ("isAutoGenerated".equals(keyNode.getValue())) {
                    isAutoGenerated = Boolean.parseBoolean(propertyValue) || "yes".equals(propertyValue);
                }
            }
        }
        return isAutoGenerated;
    }

    public boolean isAutoGenerated() {
        try {
            byte[] data = Files.readAllBytes(passwordFile);
            return readMainKeyIsAutoGeneratedMetadata(data);
        }
        catch (NoSuchFileException e) {
            // True because on save a new key will be generated.
            return true;
        }
        catch (IOException e) {
            return true;
        }
    }

    private List<NodeTuple> createMainKeyReader(byte[] data) {
        LoaderOptions loaderOptions = new LoaderOptions();
        Composer composer = new Composer(
            new ParserImpl(new StreamReader(new String(data, StandardCharsets.UTF_8)), loaderOptions),
            new Resolver() {
                @Override
                public Tag resolve(NodeId kind, String value, boolean implicit) {
                    if (kind == NodeId.scalar) {
                        return Tag.STR;
                    }
                    return super.resolve(kind, value, implicit);
                }
            },
            loaderOptions
        );
        Node singleNode = composer.getSingleNode();
        if (singleNode instanceof MappingNode) {
            return ((MappingNode) singleNode).getValue();
        }
        return Collections.emptyList();
    }

    public Path getPasswordFile() {
        return passwordFile;
    }
}
