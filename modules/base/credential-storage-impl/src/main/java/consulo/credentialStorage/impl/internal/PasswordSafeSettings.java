package consulo.credentialStorage.impl.internal;

import consulo.credentialStorage.impl.internal.keePass.KeePassCredentialStore;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.SystemInfo;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.credentialStorage.internal.CredentialStoreManager;
import consulo.credentialStorage.internal.PasswordSafeSettingsListener;
import consulo.credentialStorage.internal.ProviderType;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import jakarta.inject.Singleton;
import jakarta.annotation.Nonnull;

@State(name = "PasswordSafe",
    storages = {@Storage(value = "security.xml", roamingType = RoamingType.DISABLED)})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class PasswordSafeSettings implements PersistentStateComponent<PasswordSafeOptions> {

    private static final Logger LOG = Logger.getInstance(PasswordSafeSettings.class);

    public static final Class<PasswordSafeSettingsListener> TOPIC = PasswordSafeSettingsListener.TOPIC;

    private PasswordSafeOptions state = new PasswordSafeOptions();

    public String getKeepassDb() {
        String result = state.getKeepassDb();
        if (result == null && getProviderType() == ProviderType.KEEPASS) {
            return KeePassCredentialStore.getDefaultDbFile().toString();
        }
        return result;
    }

    public void setKeepassDb(String value) {
        String v = StringUtil.nullize(value);
        if (v != null && v.equals(KeePassCredentialStore.getDefaultDbFile().toString())) {
            v = null;
        }
        state.setKeepassDb(v);
    }

    public ProviderType getProviderType() {
        if (SystemInfo.isWindows && state.getProvider() == ProviderType.KEYCHAIN) {
            return ProviderType.KEEPASS;
        }
        return state.getProvider();
    }

    public void setProviderType(ProviderType value) {
        ProviderType newValue = value;
        if (newValue == ProviderType.DO_NOT_STORE) {
            newValue = ProviderType.MEMORY_ONLY;
        }
        ProviderType oldValue = state.getProvider();
        if (newValue != oldValue && CredentialStoreManager.getInstance().isSupported(newValue)) {
            state.setProvider(newValue);
            if (ApplicationManager.getApplication() != null) {
                ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(TOPIC)
                    .typeChanged(oldValue, newValue);
            }
        }
    }

    @Override
    public PasswordSafeOptions getState() {
        return state;
    }

    @Override
    public void loadState(@Nonnull PasswordSafeOptions state) {
        CredentialStoreManager credentialStoreManager = CredentialStoreManager.getInstance();
        if ((state.getProvider() == ProviderType.DO_NOT_STORE && !credentialStoreManager.isSupported(ProviderType.MEMORY_ONLY))
            || (state.getProvider() != ProviderType.DO_NOT_STORE && !credentialStoreManager.isSupported(state.getProvider()))) {
            LOG.error("Provider " + state.getProvider() + " from loaded credential store config is not supported in this environment");
        }
        this.state = state;
        setProviderType(state.getProvider());
        state.setKeepassDb(StringUtil.nullize(state.getKeepassDb()));
    }
}
