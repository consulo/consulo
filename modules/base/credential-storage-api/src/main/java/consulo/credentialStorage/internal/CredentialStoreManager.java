package consulo.credentialStorage.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;

import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public interface CredentialStoreManager {

    /**
     * Checks if the specified credential store is supported in this environment.
     *
     * @param provider the credential store provider to check
     * @return true if the provider is supported, false otherwise
     */
    boolean isSupported(ProviderType provider);

    /**
     * Lists the available credential store providers in the current environment.
     * For example, in headless Linux it might be challenging to configure 'gnome-keychain',
     * so ProviderType.KEYCHAIN could be excluded from this list.
     *
     * @return list of supported providers
     */
    List<ProviderType> availableProviders();

    /**
     * Returns the default credential store provider.
     * In most cases, it is ProviderType.KEYCHAIN, but in headless Linux environments it could be different.
     *
     * @return the default credential store provider
     */
    ProviderType defaultProvider();

    /**
     * Returns the singleton instance of CredentialStoreManager.
     *
     * @return the CredentialStoreManager instance
     */
    static CredentialStoreManager getInstance() {
        return ApplicationManager.getApplication().getInstance(CredentialStoreManager.class);
    }
}
