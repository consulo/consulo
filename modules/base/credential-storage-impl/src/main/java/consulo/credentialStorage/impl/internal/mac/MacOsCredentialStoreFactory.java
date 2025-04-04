package consulo.credentialStorage.impl.internal.mac;

import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.internal.CredentialStoreFactory;
import consulo.credentialStorage.impl.internal.NativeCredentialStoreWrapper;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.SystemInfo;
import consulo.util.jna.JnaLoader;

@ExtensionImpl
public class MacOsCredentialStoreFactory implements CredentialStoreFactory {
    @Override
    public CredentialStore create() {
        if (isMacOsCredentialStoreSupported() && JnaLoader.isLoaded()) {
            return new NativeCredentialStoreWrapper(new KeyChainCredentialStore());
        }
        return null;
    }

    private boolean isMacOsCredentialStoreSupported() {
        return SystemInfo.isMac;
    }
}
