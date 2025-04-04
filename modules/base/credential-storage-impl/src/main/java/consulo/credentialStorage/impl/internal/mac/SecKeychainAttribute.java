package consulo.credentialStorage.impl.internal.mac;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a single attribute in a keychain item.
 */
@Structure.FieldOrder({"tag", "length", "data"})
public class SecKeychainAttribute extends Structure implements Structure.ByReference {
    public int tag;
    public int length;
    public Pointer data;

    public SecKeychainAttribute() {
        super();
    }

    public SecKeychainAttribute(Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("tag", "length", "data");
    }
}
