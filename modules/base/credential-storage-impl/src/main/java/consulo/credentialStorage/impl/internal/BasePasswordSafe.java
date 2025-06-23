package consulo.credentialStorage.impl.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.util.SynchronizedClearableLazy;
import consulo.component.ProcessCanceledException;
import consulo.component.extension.ExtensionPoint;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionSpec;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionSupportFactory;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionType;
import consulo.credentialStorage.impl.internal.kdbx.IncorrectMainPasswordException;
import consulo.credentialStorage.impl.internal.keePass.InMemoryCredentialStore;
import consulo.credentialStorage.impl.internal.keePass.KeePassCredentialStore;
import consulo.credentialStorage.impl.internal.ui.PasswordSafeConfigurable;
import consulo.credentialStorage.impl.internal.ui.PasswordSafeNotificationGroupContributor;
import consulo.credentialStorage.internal.CredentialStoreFactory;
import consulo.credentialStorage.internal.CredentialStoreManager;
import consulo.credentialStorage.internal.ProviderType;
import consulo.credentialStorage.localize.CredentialStorageLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationAction;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static consulo.credentialStorage.impl.internal.keePass.KeePassCredentialStore.getDefaultDbFile;
import static consulo.credentialStorage.impl.internal.keePass.KeePassCredentialStore.getDefaultMainPasswordFile;

public abstract class BasePasswordSafe implements PasswordSafe {
    protected final ApplicationConcurrency myApplicationConcurrency;

    // Constructor accepts an executor; in production you might use your app’s coroutine scope.
    protected BasePasswordSafe(ApplicationConcurrency applicationConcurrency) {
        myApplicationConcurrency = applicationConcurrency;

        _currentProvider = new SynchronizedClearableLazy<>(() -> computeProvider(getSettings()));
    }

    // Subclasses must provide the settings.
    protected abstract PasswordSafeSettings getSettings();

    @Override
    public boolean isRememberPasswordByDefault() {
        return getSettings().getState().isRememberPasswordByDefault();
    }

    @Override
    public void setRememberPasswordByDefault(boolean rememberPasswordByDefault) {
        getSettings().getState().setRememberPasswordByDefault(rememberPasswordByDefault);
    }

    // Synchronized lazy holder for the current credential provider.
    private final SynchronizedClearableLazy<CredentialStore> _currentProvider;

    // Returns the provider if already computed.
    private CredentialStore getCurrentProviderIfComputed() {
        return _currentProvider.isInitialized() ? _currentProvider.get() : null;
    }

    public CredentialStore getCurrentProvider() {
        return _currentProvider.get();
    }

    public void setCurrentProvider(CredentialStore provider) {
        _currentProvider.setValue(provider);
    }

    public void closeCurrentStore(boolean isSave, boolean isEvenMemoryOnly) {
        CredentialStore store = getCurrentProviderIfComputed();
        if (store == null) return;
        if (!isEvenMemoryOnly && (store instanceof InMemoryCredentialStore)) return;

        _currentProvider.drop();
        if (isSave && (store instanceof KeePassCredentialStore)) {
            try {
                ((KeePassCredentialStore) store).save(createMainKeyEncryptionSpec());
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                getLogger().warn(e);
            }
        }
        else if (store instanceof Closeable) {
            try {
                ((Closeable) store).close();
            }
            catch (Exception e) {
                // Ignore close exceptions.
            }
        }
    }

    private EncryptionSpec createMainKeyEncryptionSpec() {
        String pgpKey = getSettings().getState().getPgpKeyId();
        if (pgpKey == null) {
            return new EncryptionSpec(EncryptionSupportFactory.getDefaultEncryptionType(), null);
        }
        else {
            return new EncryptionSpec(EncryptionType.PGP_KEY, pgpKey);
        }
    }

    // A simple lazy helper for memory-only storage.
    private CredentialStore memoryHelperProvider = null;
    private boolean memoryHelperProviderInitialized = false;

    private synchronized CredentialStore getMemoryHelperProvider() {
        if (!memoryHelperProviderInitialized) {
            memoryHelperProvider = new InMemoryCredentialStore();
            memoryHelperProviderInitialized = true;
        }
        return memoryHelperProvider;
    }

    private synchronized boolean isMemoryHelperProviderInitialized() {
        return memoryHelperProviderInitialized;
    }

    @Override
    public boolean isMemoryOnly() {
        return getSettings().getProviderType() == ProviderType.MEMORY_ONLY;
    }

    @Override
    public Credentials get(CredentialAttributes attributes) {
        //SlowOperations.assertNonCancelableSlowOperationsAreAllowed();
        Credentials value = getCurrentProvider().get(attributes);
        if ((value == null || StringUtil.isEmptyOrSpaces(value.getPassword()))
            && isMemoryHelperProviderInitialized()) {
            Credentials memCred = getMemoryHelperProvider().get(attributes);
            if (memCred != null) return memCred;
        }
        return value;
    }

    @Override
    public void set(CredentialAttributes attributes, Credentials credentials) {
        //SlowOperations.assertNonCancelableSlowOperationsAreAllowed();
        getCurrentProvider().set(attributes, credentials);
        if (attributes.isPasswordMemoryOnly() && credentials != null && !StringUtil.isEmptyOrSpaces(credentials.getPassword())) {
            // If password is stored as memory-only, use simplified attributes.
            CredentialAttributes simpleAttrs = new CredentialAttributes(attributes.getServiceName(), attributes.getUserName());
            getMemoryHelperProvider().set(simpleAttrs, credentials);
        }
        else if (isMemoryHelperProviderInitialized()) {
            getMemoryHelperProvider().set(attributes, null);
        }
    }

    @Override
    public void set(CredentialAttributes attributes, Credentials credentials, boolean memoryOnly) {
        if (memoryOnly) {
            CredentialAttributes attrsToUse = attributes.isPasswordMemoryOnly()
                ? new CredentialAttributes(attributes.getServiceName(), attributes.getUserName())
                : attributes;
            getMemoryHelperProvider().set(attrsToUse, credentials);
            // Remove from the default provider.
            getCurrentProvider().set(attributes, null);
        }
        else {
            set(attributes, credentials);
        }
    }

    @Override
    public CompletableFuture<Credentials> getAsync(CredentialAttributes attributes) {
        return CompletableFuture.supplyAsync(() -> get(attributes), myApplicationConcurrency.getExecutorService());
    }

    public void save() {
        CredentialStore store = getCurrentProviderIfComputed();
        if (!(store instanceof KeePassCredentialStore)) return;
        KeePassCredentialStore kpStore = (KeePassCredentialStore) store;
        try {
            kpStore.save(createMainKeyEncryptionSpec());
        }
        catch (Exception e) {
            // Handle or log the exception as needed.
        }
    }

    @Override
    public boolean isPasswordStoredOnlyInMemory(CredentialAttributes attributes, Credentials credentials) {
        if (isMemoryOnly() || StringUtil.isEmptyOrSpaces(credentials.getPassword())) {
            return true;
        }
        if (!isMemoryHelperProviderInitialized()) {
            return false;
        }
        Credentials memCred = getMemoryHelperProvider().get(attributes);
        return memCred != null && !StringUtil.isEmptyOrSpaces(memCred.getPassword());
    }

    private static CredentialStore computeProvider(PasswordSafeSettings settings) {
        if (settings.getProviderType() == ProviderType.MEMORY_ONLY ||
            ApplicationManager.getApplication().isUnitTestMode()) {
            return new InMemoryCredentialStore();
        }

        Consumer<LocalizeValue> showError = (title) -> PasswordSafeNotificationGroupContributor.GROUP.buildError()
            .title(title)
            .content(CredentialStorageLocalize.notificationContentInMemoryStorage())
            .addClosingAction(
                CredentialStorageLocalize.notificationContentPasswordSettingsAction(),
                e -> {
                    Project project = e.getData(Project.KEY);
                    Application.get().getInstance(ShowConfigurableService.class).showAndSelect(project, PasswordSafeConfigurable.class);
                }
            )
            .notify(null);

        if (CredentialStoreManager.getInstance().isSupported(settings.getProviderType())) {
            if (settings.getProviderType() == ProviderType.KEEPASS) {
                try {
                    Path dbFile = settings.getKeepassDb() != null
                        ? Paths.get(settings.getKeepassDb())
                        : getDefaultDbFile();
                    return new KeePassCredentialStore(dbFile, getDefaultMainPasswordFile());
                }
                catch (IncorrectMainPasswordException e) {
                    getLogger().warn(e);
                    showError.accept(e.isFileMissed()
                        ? CredentialStorageLocalize.notificationTitlePasswordMissing()
                        : CredentialStorageLocalize.notificationTitlePasswordIncorrect());
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Throwable e) {
                    getLogger().error(e);
                    showError.accept(CredentialStorageLocalize.notificationTitleDatabaseError());
                }
            }
            else {
                try {
                    CredentialStore store = createPersistentCredentialStore();
                    if (store == null) {
                        showError.accept(CredentialStorageLocalize.notificationTitleKeychainNotAvailable());
                    }
                    else {
                        return store;
                    }
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Throwable e) {
                    getLogger().error(e);
                    showError.accept(CredentialStorageLocalize.notificationTitleCannotUseKeychain());
                }
            }
        }
        else {
            getLogger().error("Provider " + settings.getProviderType() + " is not supported in this environment");
            showError.accept(CredentialStorageLocalize.notificationTitleCannotUseProvider(settings.getProviderType().toString()));
        }

        settings.setProviderType(ProviderType.MEMORY_ONLY);
        return new InMemoryCredentialStore();
    }

    @Nullable
    public static CredentialStore createPersistentCredentialStore() {
        Platform platform = Platform.current();

        ExtensionPoint<CredentialStoreFactory> point = Application.get().getExtensionPoint(CredentialStoreFactory.class);

        return point.computeSafeIfAny(f -> f.create(platform));
    }

    private static Logger getLogger() {
        return Logger.getInstance(BasePasswordSafe.class);
    }
}
