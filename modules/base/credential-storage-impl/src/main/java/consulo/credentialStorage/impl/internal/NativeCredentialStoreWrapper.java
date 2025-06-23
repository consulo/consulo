package consulo.credentialStorage.impl.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import consulo.application.Application;
import consulo.application.util.SystemInfo;
import consulo.application.util.concurrent.QueueProcessor;
import consulo.component.ProcessCanceledException;
import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.impl.internal.keePass.InMemoryCredentialStore;
import consulo.credentialStorage.impl.internal.ui.PasswordSafeNotificationGroupContributor;
import consulo.credentialStorage.localize.CredentialStorageLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public class NativeCredentialStoreWrapper implements CredentialStore, Closeable {
    private static final Logger LOG = Logger.getInstance(NativeCredentialStoreWrapper.class);

    // Constant used to mark removed credentials.
    private static final Credentials REMOVED_CREDENTIALS = new Credentials("REMOVED_CREDENTIALS");

    private final CredentialStore store;
    private final QueueProcessor<Runnable> queueProcessor;

    // Lazy fallback store (created only when needed)
    private InMemoryCredentialStore fallbackStore = null;

    // Used to postpone credentials until the native store is ready.
    private final InMemoryCredentialStore postponedCredentials = new InMemoryCredentialStore();

    // Cache to track denied items (expires 1 minute after access).
    private final Cache<CredentialAttributes, Boolean> deniedItems =
        CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

    public NativeCredentialStoreWrapper(CredentialStore store, QueueProcessor<Runnable> queueProcessor) {
        this.store = store;
        this.queueProcessor = queueProcessor;
    }

    // Secondary constructor: uses a QueueProcessor that immediately runs tasks.
    public NativeCredentialStoreWrapper(CredentialStore store) {
        this(store, new QueueProcessor<>(runnable -> runnable.run()));
    }

    private boolean isFallbackStoreInitialized() {
        return fallbackStore != null;
    }

    private InMemoryCredentialStore getFallbackStore() {
        if (fallbackStore == null) {
            fallbackStore = new InMemoryCredentialStore();
        }
        return fallbackStore;
    }

    @Override
    public Credentials get(CredentialAttributes attributes) {
        // Check postponed credentials first.
        Credentials postponed = postponedCredentials.get(attributes);
        if (postponed != null) {
            return REMOVED_CREDENTIALS.equals(postponed) ? null : postponed;
        }

        if (attributes.isCacheDeniedItems() && deniedItems.getIfPresent(attributes) != null) {
            LOG.warn("User denied access to " + attributes);
            return Credentials.ACCESS_TO_KEY_CHAIN_DENIED;
        }

        CredentialStore currentStore = isFallbackStoreInitialized() ? getFallbackStore() : this.store;
        try {
            Credentials value = currentStore.get(attributes);
            if (attributes.isCacheDeniedItems() && value == Credentials.ACCESS_TO_KEY_CHAIN_DENIED) {
                deniedItems.put(attributes, true);
            }
            return value;
        }
        catch (UnsatisfiedLinkError e) {
            currentStore = getFallbackStore();
            NativeCredentialStoreWrapper.notifyUnsatisfiedLinkError(e);
            return currentStore.get(attributes);
        }
        catch (Throwable e) {
            LOG.error(e);
            return null;
        }
    }

    @Override
    public void set(CredentialAttributes attributes, Credentials credentials) {
        if (isFallbackStoreInitialized()) {
            getFallbackStore().set(attributes, credentials);
            return;
        }

        Credentials postponed = (credentials != null) ? credentials : REMOVED_CREDENTIALS;
        postponedCredentials.set(attributes, postponed);

        queueProcessor.add(() -> {
            try {
                CredentialStore currentStore = isFallbackStoreInitialized() ? getFallbackStore() : this.store;
                try {
                    currentStore.set(attributes, credentials);
                }
                catch (UnsatisfiedLinkError e) {
                    currentStore = getFallbackStore();
                    NativeCredentialStoreWrapper.notifyUnsatisfiedLinkError(e);
                    currentStore.set(attributes, credentials);
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            finally {
                Credentials currentPostponed = postponedCredentials.get(attributes);
                if (postponed.equals(currentPostponed)) {
                    postponedCredentials.set(attributes, null);
                }
            }
        });
    }

    @Override
    public void close() {
        if (store instanceof Closeable) {
            queueProcessor.waitFor();
            try {
                ((Closeable) store).close();
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    public static void notifyUnsatisfiedLinkError(UnsatisfiedLinkError e) {
        LOG.error(e);
        LocalizeValue message = CredentialStorageLocalize.notificationContentNativeKeychainUnavailable(Application.get().getName().get());
        if (SystemInfo.isLinux) {
            message = LocalizeValue.join(message, LocalizeValue.of("\n"), CredentialStorageLocalize.notificationContentNativeKeychainUnavailableLinuxAddition());
        }

        PasswordSafeNotificationGroupContributor.GROUP.buildError()
            .title(CredentialStorageLocalize.notificationTitleNativeKeychainUnavailable())
            .content(message)
            .notify(null);
    }
}
