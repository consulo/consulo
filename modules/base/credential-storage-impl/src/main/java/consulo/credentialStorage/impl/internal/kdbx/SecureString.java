package consulo.credentialStorage.impl.internal.kdbx;

import consulo.credentialStorage.OneTimeString;

public interface SecureString {
    default OneTimeString get() {
        return get(true);
    }
    
    OneTimeString get(boolean clearable);
}
