package consulo.credentialStorage.impl.internal.linux;

import consulo.annotation.component.ExtensionImpl;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.impl.internal.NativeCredentialStoreWrapper;
import consulo.credentialStorage.internal.CredentialStoreFactory;
import consulo.platform.Platform;
import consulo.platform.os.UnixOperationSystem;
import consulo.util.jna.JnaLoader;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class LinuxCredentialStoreFactory implements CredentialStoreFactory {
    @Override
    public CredentialStore create(@Nonnull Platform platform) {
        if (platform.os().isLinux()) {
            boolean preferWallet =
                platform.os() instanceof UnixOperationSystem unix
                && unix.isKDE()
                || Boolean.parseBoolean(platform.jvm().getRuntimeProperty("credentialStore.linux.prefer.kwallet", "false"));

            CredentialStore res = preferWallet ? KWalletCredentialStore.create() : null;
            if (res == null && JnaLoader.isLoaded()) {
                try {
                    res = SecretCredentialStore.create("consulo.credentialStorage.Credentials");
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
