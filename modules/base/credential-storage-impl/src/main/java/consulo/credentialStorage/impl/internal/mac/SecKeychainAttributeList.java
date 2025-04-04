package consulo.credentialStorage.impl.internal.mac;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a list of keychain attributes.
 */
@Structure.FieldOrder({"count", "attr"})
public class SecKeychainAttributeList extends Structure {
    public int count;
    public Pointer attr;

    public SecKeychainAttributeList() {
        super();
    }

    public SecKeychainAttributeList(Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("count", "attr");
    }
}
