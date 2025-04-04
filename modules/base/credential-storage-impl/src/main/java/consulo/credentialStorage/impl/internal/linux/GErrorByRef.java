package consulo.credentialStorage.impl.internal.linux;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;

public class GErrorByRef extends ByReference {
    public GErrorByRef() {
        super(Native.POINTER_SIZE);
        getPointer().setPointer(0, null);
    }

    public GError getValue() {
        Pointer p = getPointer().getPointer(0);
        if (p == null || p.equals(Pointer.NULL)) {
            return null;
        }
        GError error = new GError(p);
        error.read();
        return error;
    }
}
