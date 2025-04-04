package consulo.credentialStorage.impl.internal.linux;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.CredentialUtils;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.impl.internal.CredentialStoreUtil;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.errors.NoReply;
import org.freedesktop.dbus.errors.ServiceUnknown;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class KWalletCredentialStore implements CredentialStore, Closeable {
    private final DBusConnection connection;
    private final KWallet kWallet;

    private final String appId;
    private int cachedWalletId = -1;
    private String cachedWalletName = null;
    private String suggestedCreation = null;

    private static final Logger LOG = CredentialStoreUtil.getLogger();

    private KWalletCredentialStore(DBusConnection connection, KWallet kWallet) {
        this.connection = connection;
        this.kWallet = kWallet;
        this.appId = appName();
    }

    private static String appName() {
        Application app = ApplicationManager.getApplication();
        String appName = (app == null || app.isUnitTestMode()) ? null : app.getName().get();
        return (appName != null) ? appName : "Consulo tests";
    }

    public static KWalletCredentialStore create() {
        try {
            DBusConnection connection = DBusConnectionBuilder.forSessionBus().build();
            try {
                KWallet wallet = connection.getRemoteObject("org.kde.kwalletd5", "/modules/kwalletd5", KWallet.class, true);
                // Ping the wallet
                wallet.localWallet();
                return new KWalletCredentialStore(connection, wallet);
            }
            catch (ServiceUnknown e) {
                LOG.info("No KWallet service", e);
            }
            catch (DBusException | RuntimeException e) {
                LOG.warn("Failed to connect to KWallet", e);
            }
            connection.close();
        }
        catch (Exception e) {
            LOG.warn("Failed to connect to D-Bus", e);
        }
        return null;
    }

    private int getWalletId() {
        if (cachedWalletId != -1 && kWallet.isOpen(cachedWalletId)
            && cachedWalletName != null && kWallet.users(cachedWalletName).contains(appId)) {
            return cachedWalletId;
        }
        String walletName = kWallet.localWallet();
        cachedWalletName = walletName;
        List<String> wallets = kWallet.wallets();
        boolean isNew = (walletName != null && !wallets.contains(walletName));
        if (walletName == null || (isNew && walletName.equals(suggestedCreation))) {
            return -1;
        }
        cachedWalletId = kWallet.open(walletName, 0L, appId);
        if (isNew) {
            suggestedCreation = walletName;
        }
        return cachedWalletId;
    }

    private void handleError(Runnable run, Runnable handle) {
        try {
            run.run();
        }
        catch (NoReply e) {
            handle.run();
        }
    }

    @Override
    public Credentials get(final CredentialAttributes attributes) {
        final Credentials[] resultHolder = new Credentials[1];
        handleError(() -> {
            int walletId = getWalletId();
            if (walletId == -1) {
                resultHolder[0] = Credentials.ACCESS_TO_KEY_CHAIN_DENIED;
                return;
            }
            String userName = StringUtil.nullize(attributes.getUserName());
            if (userName == null) {
                userName = "*";
            }
            // readPasswordList returns a Map<String, Variant<String>>
            Map<String, Variant<String>> passwords = kWallet.readPasswordList(walletId, attributes.getServiceName(), userName, appId);
            if (passwords == null || passwords.isEmpty()) {
                resultHolder[0] = null;
                return;
            }
            Map.Entry<String, Variant<String>> entry = passwords.entrySet().iterator().next();
            resultHolder[0] = new Credentials(entry.getKey(), entry.getValue().getValue());
        }, () -> resultHolder[0] = Credentials.CANNOT_UNLOCK_KEYCHAIN);
        return resultHolder[0];
    }

    @Override
    public void set(final CredentialAttributes attributes, final Credentials credentials) {
        handleError(() -> {
            int walletId = getWalletId();
            if (walletId == -1) return;
            String accountName = StringUtil.nullize(attributes.getUserName());
            if (accountName == null && credentials != null) {
                accountName = credentials.getUserName();
            }
            if (CredentialUtils.isEmpty(credentials)) {
                kWallet.removeFolder(walletId, attributes.getServiceName(), appId);
            }
            else {
                kWallet.writePassword(walletId, attributes.getServiceName(), accountName != null ? accountName : "",
                    credentials.getPassword() != null ? credentials.getPassword().toString() : "", appId);
            }
        }, () -> { /* No error handling action */ });
    }

    @Override
    public void close() {
        try {
            if (cachedWalletId != -1) {
                kWallet.close(cachedWalletId, false, appId);
            }
            connection.close();
        }
        catch (DBusException | IOException e) {
            LOG.warn("Error closing DBus connection", e);
        }
    }

}
