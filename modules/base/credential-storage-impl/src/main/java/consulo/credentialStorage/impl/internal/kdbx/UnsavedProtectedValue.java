package consulo.credentialStorage.impl.internal.kdbx;

import consulo.credentialStorage.OneTimeString;
import org.jdom.Text;

public class UnsavedProtectedValue extends Text implements SecureString {
    // Delegates the SecureString implementation to the provided StringProtectedByStreamCipher.
    private final StringProtectedByStreamCipher secureString;

    public UnsavedProtectedValue(StringProtectedByStreamCipher secureString) {
        super("");
        this.secureString = secureString;
    }

    public StringProtectedByStreamCipher getSecureString() {
        return secureString;
    }

    @Override
    public OneTimeString get(boolean clearable) {
        return secureString.get(clearable);
    }

    @Override
    public String getText() {
        throw new IllegalStateException("Must be converted to ProtectedValue for serialization");
    }
}
