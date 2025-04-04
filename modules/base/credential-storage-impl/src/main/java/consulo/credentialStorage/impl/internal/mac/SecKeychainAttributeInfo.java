package consulo.credentialStorage.impl.internal.mac;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

@Structure.FieldOrder({"count", "tag", "format"})
public class SecKeychainAttributeInfo extends Structure {
    public int count;
    public Pointer tag;
    public Pointer format;

    public SecKeychainAttributeInfo() {
        super();
    }

    public SecKeychainAttributeInfo(Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("count", "tag", "format");
    }

    /**
     * Factory method to create a SecKeychainAttributeInfo from the given IDs.
     * This allocates memory for both tag and format arrays and initializes them.
     *
     * @param ids one or more attribute IDs.
     * @return an initialized SecKeychainAttributeInfo instance.
     */
    public static SecKeychainAttributeInfo create(int... ids) {
        SecKeychainAttributeInfo info = new SecKeychainAttributeInfo();
        info.count = ids.length;
        // Each integer occupies 4 bytes.
        int size = ids.length * 4;
        // Allocate a block of memory twice as big as needed for tag; the second half will be used for format.
        Memory mem = new Memory(size * 2L);
        Pointer tagPointer = mem;
        // The format data is stored in the second half.
        Pointer formatPointer = mem.share(size, size);
        info.tag = tagPointer;
        info.format = formatPointer;
        int offset = 0;
        for (int id : ids) {
            // Set the tag value.
            mem.setInt(offset, id);
            // kSecFormatUnknown is 0.
            formatPointer.setInt(offset, 0);
            offset += 4;
        }
        return info;
    }
}
