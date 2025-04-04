package consulo.credentialStorage.impl.internal.linux;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.credentialStorage.*;
import consulo.credentialStorage.impl.internal.CredentialStoreUtil;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

public class SecretCredentialStore implements CredentialStore {
    // Constants
    private static final int SECRET_SCHEMA_DONT_MATCH_NAME = 2;
    private static final int SECRET_SCHEMA_ATTRIBUTE_STRING = 0;
    private static final int DBUS_ERROR_SERVICE_UNKNOWN = 2;
    private static final int SECRET_ERROR_IS_LOCKED = 2;

    // Load the Secret Service library via JNA.
    private static final SecretLibrary library = Native.load("secret-1", SecretLibrary.class);
    private static final int DBUS_ERROR = library.g_dbus_error_quark();
    private static final int SECRET_ERROR = library.secret_error_get_quark();

    // Lazy-initialized pointers for attribute names.
    private final Memory serviceAttributeNamePointer;
    private final Memory accountAttributeNamePointer;
    // Schema pointer (initialized lazily in constructor).
    private final Pointer schema;
    private final String schemeName;

    private static final Logger LOG = CredentialStoreUtil.getLogger();

    // Private constructor.
    private SecretCredentialStore(String schemeName) {
        this.schemeName = schemeName;
        // Initialize attribute name pointers.
        this.serviceAttributeNamePointer = stringPointer("service".getBytes());
        this.accountAttributeNamePointer = stringPointer("account".getBytes());
        // Initialize schema using the provided schemeName.
        this.schema = library.secret_schema_new(schemeName, SECRET_SCHEMA_DONT_MATCH_NAME,
            serviceAttributeNamePointer, SECRET_SCHEMA_ATTRIBUTE_STRING,
            accountAttributeNamePointer, SECRET_SCHEMA_ATTRIBUTE_STRING,
            null);
    }

    public static SecretCredentialStore create(String schemeName) {
        if (pingService()) {
            return new SecretCredentialStore(schemeName);
        }
        return null;
    }

    private static boolean pingService() {
        Memory attr = null;
        Pointer dummySchema = null;
        try {
            attr = stringPointer("ij-dummy-attribute".getBytes());
            dummySchema = library.secret_schema_new("IJ.dummy.ping.schema", SECRET_SCHEMA_DONT_MATCH_NAME,
                attr, SECRET_SCHEMA_ATTRIBUTE_STRING,
                null);
            GErrorByRef errorRef = new GErrorByRef();
            String lookup = library.secret_password_lookup_sync(dummySchema, null, errorRef, attr, attr);
            GError error = errorRef.getValue();
            if (error == null) {
                return true;
            }
            if (isNoSecretService(error)) {
                return false;
            }
        }
        finally {
            if (attr != null) {
                attr.close();
            }
            if (dummySchema != null) {
                library.secret_schema_unref(dummySchema);
            }
        }
        return true;
    }

    private static boolean isNoSecretService(GError error) {
        if (error.domain == DBUS_ERROR) {
            return error.code == DBUS_ERROR_SERVICE_UNKNOWN;
        }
        return false;
    }

    // Utility method to create a Memory pointer containing the data with a null terminator.
    public static Memory stringPointer(byte[] data) {
        return stringPointer(data, false);
    }

    public static Memory stringPointer(byte[] data, boolean clearInput) {
        Memory pointer = new Memory(data.length + 1L);
        pointer.write(0, data, 0, data.length);
        pointer.setByte(data.length, (byte) 0);
        if (clearInput) {
            Arrays.fill(data, (byte) 0);
        }
        return pointer;
    }

    @Override
    public Credentials get(final CredentialAttributes attributes) {
        long start = System.currentTimeMillis();
        try {
            Credentials credentials = CompletableFuture.supplyAsync((Supplier<Credentials>) () -> {
                String userName = StringUtil.nullize(attributes.getUserName());
                return checkError("secret_password_lookup_sync", errorRef -> {
                    Memory serviceNamePointer = stringPointer(attributes.getServiceName().getBytes());
                    if (userName == null) {
                        String result = library.secret_password_lookup_sync(schema, null, errorRef,
                            serviceAttributeNamePointer, serviceNamePointer, null);
                        if (result != null) {
                            return CredentialStoreUtil.splitData(result);
                        }
                    }
                    else {
                        Memory userNamePointer = stringPointer(userName.getBytes());
                        String result = library.secret_password_lookup_sync(schema, null, errorRef,
                            serviceAttributeNamePointer, serviceNamePointer,
                            accountAttributeNamePointer, userNamePointer, null);
                        if (result != null) {
                            return CredentialStoreUtil.splitData(result);
                        }
                    }
                    return null;
                });
            }, AppExecutorUtil.getAppExecutorService()).get(30, TimeUnit.SECONDS);
            long end = System.currentTimeMillis();
            if (credentials == null && (end - start) > 300) {
                return Credentials.ACCESS_TO_KEY_CHAIN_DENIED;
            }
            return credentials;
        }
        catch (TimeoutException e) {
            LOG.warn("storage unlock timeout");
            return Credentials.CANNOT_UNLOCK_KEYCHAIN;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(final CredentialAttributes attributes, final Credentials credentials) {
        Memory serviceNamePointer = stringPointer(attributes.getServiceName().getBytes());
        String accountName = StringUtil.nullize(attributes.getUserName());
        if (accountName == null && credentials != null) {
            accountName = credentials.getUserName();
        }
        String lookupName = attributes.getServiceName().equals(CredentialAttributesUtil.SERVICE_NAME_PREFIX) ? accountName : null;
        if (CredentialUtils.isEmpty(credentials)) {
            clearPassword(serviceNamePointer, lookupName);
            return;
        }
        byte[] joinedData = CredentialStoreUtil.joinData(credentials.getUserName(), credentials.getPassword());
        Memory passwordPointer = stringPointer(joinedData, true);

        final String finalAccountName = accountName;
        
        checkError("secret_password_store_sync", errorRef -> {
            try {
                // Ensure any existing password is cleared.
                clearPassword(serviceNamePointer, null);
                if (finalAccountName == null) {
                    library.secret_password_store_sync(schema, null, serviceNamePointer, passwordPointer, null, errorRef,
                        serviceAttributeNamePointer, serviceNamePointer, null);
                }
                else {
                    library.secret_password_store_sync(schema, null, serviceNamePointer, passwordPointer, null, errorRef,
                        serviceAttributeNamePointer, serviceNamePointer,
                        accountAttributeNamePointer, stringPointer(finalAccountName.getBytes()), null);
                }
            }
            finally {
                passwordPointer.close();
            }
            return null;
        });
    }

    private void clearPassword(Memory serviceNamePointer, String accountName) {
        checkError("secret_password_clear_sync", errorRef -> {
            if (accountName == null) {
                library.secret_password_clear_sync(schema, null, errorRef,
                    serviceAttributeNamePointer, serviceNamePointer, null);
            }
            else {
                library.secret_password_clear_sync(schema, null, errorRef,
                    serviceAttributeNamePointer, serviceNamePointer,
                    accountAttributeNamePointer, stringPointer(accountName.getBytes()), null);
            }
            return null;
        });
    }

    private <T> T checkError(String method, Function<GErrorByRef, T> task) {
        GErrorByRef errorRef = new GErrorByRef();
        T result = task.apply(errorRef);
        GError error = errorRef.getValue();
        if (error != null) {
            if (isNoSecretService(error)) {
                LOG.warn("gnome-keyring not installed or kde doesn't support Secret Service API. " +
                    method + " error code " + error.code + ", error message " + error.message);
            }
            if (error.domain == SECRET_ERROR && error.code == SECRET_ERROR_IS_LOCKED) {
                LOG.warn("Cancelled storage unlock: " + error.message);
            }
            else {
                LOG.error(method + " error code " + error.code + ", error message " + error.message);
            }
        }
        return result;
    }
}
