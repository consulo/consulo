package consulo.credentialStorage.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.SystemInfo;
import consulo.credentialStorage.internal.CredentialStoreManager;
import consulo.credentialStorage.internal.ProviderType;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.List;

@Singleton
@ServiceImpl
public class CredentialStoreManagerImpl implements CredentialStoreManager {

    @Override
    public boolean isSupported(ProviderType provider) {
        if (provider == ProviderType.KEYCHAIN) {
            return (SystemInfo.isLinux || SystemInfo.isMac) && availableProviders().contains(provider);
        }
        else {
            return availableProviders().contains(provider);
        }
    }

    @Override
    public List<ProviderType> availableProviders() {
        return Arrays.asList(ProviderType.values());
    }

    @Override
    public ProviderType defaultProvider() {
        return SystemInfo.isWindows ? ProviderType.KEEPASS : ProviderType.KEYCHAIN;
    }
}
