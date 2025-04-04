package consulo.credentialStorage.impl.internal.linux;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.registry.Registry;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.impl.internal.NativeCredentialStoreWrapper;
import consulo.credentialStorage.internal.CredentialStoreFactory;
import consulo.platform.Platform;
import consulo.util.jna.JnaLoader;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class LinuxCredentialStoreFactory implements CredentialStoreFactory {
    @Override
    public CredentialStore create(@Nonnull Platform platform) {
        if (platform.os().isLinux()) {
            boolean preferWallet = Registry.is("credentialStore.linux.prefer.kwallet", false);
            CredentialStore res = preferWallet ? KWalletCredentialStore.create() : null;
            if (res == null && JnaLoader.isLoaded()) {
                try {
                    res = SecretCredentialStore.create("com.intellij.credentialStore.Credential");
                }
                catch (UnsatisfiedLinkError e) {
                    res = !preferWallet ? KWalletCredentialStore.create() : null;
                    if (res == null) {
                        NativeCredentialStoreWrapper.notifyUnsatisfiedLinkError(e);
                    }
                }
            }
            if (res == null && !preferWallet) {
                res = KWalletCredentialStore.create();
            }
            return (res != null) ? new NativeCredentialStoreWrapper(res) : null;
        }
        return null;
    }
}
