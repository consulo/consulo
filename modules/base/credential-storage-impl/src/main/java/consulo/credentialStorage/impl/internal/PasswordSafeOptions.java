package consulo.credentialStorage.impl.internal;

import consulo.credentialStorage.internal.CredentialStoreManager;
import consulo.credentialStorage.internal.ProviderType;
import consulo.util.xml.serializer.annotation.OptionTag;

public class PasswordSafeOptions {

    // The default value is retrieved dynamically from CredentialStoreManager
    private ProviderType provider = CredentialStoreManager.getInstance().defaultProvider();

    // Do not use this directly.
    private String keepassDb;
    private boolean isRememberPasswordByDefault = true;
    // Do not use this directly.
    private String pgpKeyId;

    @OptionTag("PROVIDER")
    public ProviderType getProvider() {
        return provider;
    }

    public void setProvider(ProviderType provider) {
        this.provider = provider;
    }

    public String getKeepassDb() {
        return keepassDb;
    }

    public void setKeepassDb(String keepassDb) {
        this.keepassDb = keepassDb;
    }

    public boolean isRememberPasswordByDefault() {
        return isRememberPasswordByDefault;
    }

    public void setRememberPasswordByDefault(boolean rememberPasswordByDefault) {
        this.isRememberPasswordByDefault = rememberPasswordByDefault;
    }

    public String getPgpKeyId() {
        return pgpKeyId;
    }

    public void setPgpKeyId(String pgpKeyId) {
        this.pgpKeyId = pgpKeyId;
    }
}
