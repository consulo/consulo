package consulo.credentialStorage.impl.internal.keePass;

import consulo.credentialStorage.impl.internal.kdbx.IncorrectMainPasswordException;
import consulo.credentialStorage.impl.internal.kdbx.Kdbx;
import consulo.credentialStorage.impl.internal.kdbx.KdbxPassword;
import consulo.credentialStorage.impl.internal.kdbx.KeePassDatabase;
import consulo.application.Application;
import consulo.application.io.SafeOutputStream;
import consulo.application.io.SafeOutputStreamFactory;
import consulo.container.boot.ContainerPathManager;
import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.impl.internal.CredentialStoreUtil;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionSpec;
import consulo.logging.Logger;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeePassCredentialStore extends BaseKeePassCredentialStore {

    public static final String DB_FILE_NAME = "c.kdbx";
    // MAIN_KEY_FILE_NAME is assumed to be defined somewhere; here we define a placeholder.
    public static final String MAIN_KEY_FILE_NAME = "main.key";

    public static Path getDefaultDbFile() {
        return Path.of(ContainerPathManager.get().getConfigPath()).resolve(DB_FILE_NAME);
    }

    public static Path getDefaultMainPasswordFile() {
        return Path.of(ContainerPathManager.get().getConfigPath()).resolve(MAIN_KEY_FILE_NAME);
    }

    private final Path dbFile;
    private final MainKeyFileStorage mainKeyStorage;
    private final AtomicBoolean isNeedToSave;
    // The database instance from the KeePass file.
    protected KeePassDatabase db;

    /**
     * Primary constructor.
     *
     * @param dbFile         the path to the KeePass database file
     * @param mainKeyStorage the main key storage
     * @param preloadedDb    a preloaded KeePassDatabase; may be null
     */
    public KeePassCredentialStore(Path dbFile, MainKeyFileStorage mainKeyStorage, KeePassDatabase preloadedDb) {
        this.dbFile = dbFile;
        this.mainKeyStorage = mainKeyStorage;
        if (preloadedDb == null) {
            this.isNeedToSave = new AtomicBoolean(false);
            if (Files.exists(dbFile)) {
                byte[] mainPassword = mainKeyStorage.load();
                if (mainPassword == null) {
                    throw new IncorrectMainPasswordException(true);
                }
                try {
                    this.db = Kdbx.loadKdbx(dbFile, KdbxPassword.createAndClear(mainPassword));
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                this.db = new KeePassDatabase();
            }
        }
        else {
            this.isNeedToSave = new AtomicBoolean(true);
            this.db = preloadedDb;
        }
    }

    /**
     * Secondary constructor that creates MainKeyFileStorage from a file.
     */
    public KeePassCredentialStore(Path dbFile, Path mainKeyFile) {
        this(dbFile, new MainKeyFileStorage(mainKeyFile), null);
    }

    public Path getMainKeyFile() {
        return mainKeyStorage.getPasswordFile();
    }

    @Override
    public KeePassDatabase getDb() {
        return db;
    }

    @TestOnly
    public synchronized void reload() {
        byte[] key = mainKeyStorage.load();
        if (key == null) {
            throw new IllegalStateException("Main key is null");
        }
        KdbxPassword kdbxPassword = new KdbxPassword(key);
        Arrays.fill(key, (byte) 0);
        try {
            this.db = Kdbx.loadKdbx(dbFile, kdbxPassword);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        isNeedToSave.set(false);
    }

    public synchronized void save(EncryptionSpec mainKeyEncryptionSpec) {
        if (!isNeedToSave.compareAndSet(true, false) && !db.isDirty()) {
            return;
        }

        try {
            SecureRandom secureRandom = CredentialStoreUtil.createSecureRandom();
            byte[] mainKey = mainKeyStorage.load();
            KdbxPassword kdbxPassword;
            if (mainKey == null) {
                MainKey key = generateRandomMainKey(mainKeyEncryptionSpec, secureRandom);
                kdbxPassword = new KdbxPassword(key.getValue());
                mainKeyStorage.save(key);
            }
            else {
                kdbxPassword = new KdbxPassword(mainKey);
                Arrays.fill(mainKey, (byte) 0);
            }

            SafeOutputStreamFactory streamFactory = Application.get().getInstance(SafeOutputStreamFactory.class);

            try (SafeOutputStream os = streamFactory.create(dbFile)) {
                db.save(kdbxPassword, os, secureRandom);
            }
        }
        catch (Throwable e) {
            // Schedule a save again.
            isNeedToSave.set(true);
            Logger.getInstance(KeePassCredentialStore.class).error("Cannot save password database", e);
        }
    }

    public synchronized void deleteFileStorage() {
        try {
            Files.delete(dbFile);
        }
        catch (Exception e) {
            // Ignore delete exception.
        }
        finally {
            mainKeyStorage.save(null);
        }
    }

    public void clear() {
        db.getRootGroup().removeGroup(BaseKeePassCredentialStore.ROOT_GROUP_NAME);
        isNeedToSave.set(db.isDirty());
    }

    @TestOnly
    public void setMainPassword(MainKey mainKey, SecureRandom secureRandom) {
        // KdbxPassword hashes the value so it can be cleared before writing the file.
        saveDatabase(dbFile, db, mainKey, mainKeyStorage, secureRandom);
    }

    @Override
    public void markDirty() {
        isNeedToSave.set(true);
    }

    public static MainKey generateRandomMainKey(EncryptionSpec mainKeyEncryptionSpec, SecureRandom secureRandom) {
        byte[] bytes = new byte[512];
        secureRandom.nextBytes(bytes);
        byte[] encoded = Base64.getEncoder().withoutPadding().encode(bytes);
        return new MainKey(encoded, true, mainKeyEncryptionSpec);
    }

    public static void saveDatabase(Path dbFile, KeePassDatabase db, MainKey mainKey,
                                    MainKeyFileStorage mainKeyStorage, SecureRandom secureRandom) {
        KdbxPassword kdbxPassword = new KdbxPassword(mainKey.getValue());
        mainKeyStorage.save(mainKey);

        SafeOutputStreamFactory streamFactory = Application.get().getInstance(SafeOutputStreamFactory.class);
        try (OutputStream os = streamFactory.create(dbFile)) {
            db.save(kdbxPassword, os, secureRandom);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyTo(Map<CredentialAttributes, Credentials> from, CredentialStore store) {
        for (Map.Entry<CredentialAttributes, Credentials> entry : from.entrySet()) {
            store.set(entry.getKey(), entry.getValue());
        }
    }
}
