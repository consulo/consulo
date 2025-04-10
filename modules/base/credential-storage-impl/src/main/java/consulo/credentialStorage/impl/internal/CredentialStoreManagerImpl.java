package consulo.credentialStorage.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.credentialStorage.internal.CredentialStoreManager;
import consulo.credentialStorage.internal.ProviderType;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
@ServiceImpl
public class CredentialStoreManagerImpl implements CredentialStoreManager {
    private List<ProviderType> mySupported = List.of(ProviderType.KEYCHAIN, ProviderType.MEMORY_ONLY);

    @Override
    public boolean isSupported(ProviderType provider) {
        return mySupported.contains(provider);
    }

    @Override
    public List<ProviderType> availableProviders() {
        return mySupported;
    }

    @Override
    public ProviderType defaultProvider() {
        return mySupported.get(0);
    }
}
