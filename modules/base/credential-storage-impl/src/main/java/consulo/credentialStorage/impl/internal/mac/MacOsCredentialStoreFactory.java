package consulo.credentialStorage.impl.internal.mac;

import consulo.annotation.component.ExtensionImpl;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.impl.internal.NativeCredentialStoreWrapper;
import consulo.credentialStorage.internal.CredentialStoreFactory;
import consulo.platform.Platform;
import consulo.util.jna.JnaLoader;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class MacOsCredentialStoreFactory implements CredentialStoreFactory {
    @Override
    public CredentialStore create(@Nonnull Platform platform) {
        if (platform.os().isMac() && JnaLoader.isLoaded()) {
            return new NativeCredentialStoreWrapper(new KeyChainCredentialStore());
        }
        return null;
    }

}
