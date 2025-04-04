package consulo.credentialStorage.impl.internal.linux;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface SecretLibrary extends Library {
    Pointer secret_schema_new(String name, int flags, Object... attributes);

    void secret_schema_unref(Pointer schema);

    String secret_password_lookup_sync(Pointer schema, Pointer cancellable, GErrorByRef error, Pointer... attributes);

    void secret_password_store_sync(Pointer schema, Pointer collection, Pointer label, Pointer password, Pointer cancellable, GErrorByRef error, Pointer... attributes);

    void secret_password_clear_sync(Pointer schema, Pointer cancellable, GErrorByRef error, Pointer... attributes);

    int g_dbus_error_quark();

    int secret_error_get_quark();
}
