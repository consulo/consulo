package consulo.credentialStorage.impl.internal.mac;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import consulo.credentialStorage.*;
import consulo.credentialStorage.impl.internal.SharedLogger;
import consulo.credentialStorage.impl.internal.mac.MacOsKeychainLibrary;
import consulo.credentialStorage.impl.internal.mac.SecKeychainAttribute;
import consulo.credentialStorage.impl.internal.mac.SecKeychainAttributeInfo;
import consulo.credentialStorage.impl.internal.mac.SecKeychainAttributeList;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Arrays;

public class KeyChainCredentialStore implements CredentialStore {
    // Error codes and constants from the macOS Security framework.
    private static final int errSecSuccess = 0;
    private static final int errSecItemNotFound = -25300;
    private static final int errSecInvalidRecord = -67701;
    private static final int errUserNameNotCorrect = -25293;
    private static final int errSecUserCanceled = -128;
    private static final int kSecFormatUnknown = 0;
    // Compute kSecAccountItemAttr as in Kotlin: (('a'.code shl 8 or 'c'.code) shl 8 or 'c'.code) shl 8 or 't'.code
    private static final int kSecAccountItemAttr = ((((int) 'a' << 8 | (int) 'c') << 8 | (int) 'c') << 8 | (int) 't');

    // Load the native Security library using JNA.
    private static final MacOsKeychainLibrary library = Native.load("Security", MacOsKeychainLibrary.class);

    // Static helper: find a generic password from the keychain.
    private static Credentials findGenericPassword(byte[] serviceName, String accountName) {
        byte[] accountNameBytes = accountName != null ? accountName.getBytes() : null;
        int[] passwordSize = new int[1];
        PointerByReference passwordRef = new PointerByReference();
        PointerByReference itemRef = new PointerByReference();
        int errorCode = checkForError("find", library.SecKeychainFindGenericPassword(
            null,
            serviceName.length,
            serviceName,
            accountNameBytes != null ? accountNameBytes.length : 0,
            accountNameBytes,
            passwordSize,
            passwordRef,
            itemRef));

        try {
            if (errorCode == errSecUserCanceled) {
                return Credentials.ACCESS_TO_KEY_CHAIN_DENIED;
            }
            if (errorCode == errUserNameNotCorrect) {
                return Credentials.CANNOT_UNLOCK_KEYCHAIN;
            }
            Pointer pointer = passwordRef.getValue();
            if (pointer == null) {
                return null;
            }
            OneTimeString password = OneTimeString.fromByteArray(pointer.getByteArray(0, passwordSize[0]));
            library.SecKeychainItemFreeContent(null, pointer);

            String effectiveAccountName = accountName;
            if (effectiveAccountName == null) {
                PointerByReference attributes = new PointerByReference();
                checkForError("SecKeychainItemCopyAttributesAndData",
                    library.SecKeychainItemCopyAttributesAndData(
                        itemRef.getValue(),
                        SecKeychainAttributeInfo.create(kSecAccountItemAttr),
                        null,
                        attributes,
                        null,
                        null));
                SecKeychainAttributeList attributeList = new SecKeychainAttributeList(attributes.getValue());
                try {
                    attributeList.read();
                    Int2ObjectMap<String> attrMap = readAttributes(attributeList);
                    effectiveAccountName = attrMap.get(kSecAccountItemAttr);
                }
                finally {
                    library.SecKeychainItemFreeAttributesAndData(attributeList, null);
                }
            }
            return new Credentials(effectiveAccountName, password);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            Pointer item = itemRef.getValue();
            if (item != null) {
                library.CFRelease(item);
            }
        }
    }

    // Static helper: check for errors and log messages.
    private static int checkForError(String message, int code) {
        if (code == errSecSuccess || code == errSecItemNotFound) {
            return code;
        }
        Pointer translated = library.SecCopyErrorMessageString(code, null);
        StringBuilder builder = new StringBuilder(message).append(": ");
        if (translated == null) {
            builder.append(code);
        }
        else {
            int length = (int) library.CFStringGetLength(translated);
            char[] buf = new char[length];
            for (int i = 0; i < length; i++) {
                buf[i] = library.CFStringGetCharacterAtIndex(translated, i);
            }
            library.CFRelease(translated);
            builder.append(buf).append(" (").append(code).append(')');
        }
        if (code == errUserNameNotCorrect || code == errSecUserCanceled || code == -25299) {
            SharedLogger.LOG.warn(builder.toString());
        }
        else {
            SharedLogger.LOG.error(builder.toString());
        }
        return code;
    }

    // Dummy implementation of readAttributes; replace with your actual implementation.
    private static Int2ObjectMap<String> readAttributes(SecKeychainAttributeList attributeList) {
        return new Int2ObjectOpenHashMap<>();
    }

    @Override
    public Credentials get(CredentialAttributes attributes) {
        return findGenericPassword(attributes.getServiceName().getBytes(), StringUtil.nullize(attributes.getUserName()));
    }

    @Override
    public void set(CredentialAttributes attributes, Credentials credentials) {
        byte[] serviceName = attributes.getServiceName().getBytes();
        if (CredentialUtils.isEmpty(credentials)) {
            PointerByReference itemRef = new PointerByReference();
            byte[] userNameBytes = attributes.getUserName() != null ? StringUtil.nullize(attributes.getUserName()).getBytes() : null;
            int code = library.SecKeychainFindGenericPassword(null,
                serviceName.length,
                serviceName,
                userNameBytes != null ? userNameBytes.length : 0,
                userNameBytes,
                null,
                null,
                itemRef);
            if (code == errSecItemNotFound || code == errSecInvalidRecord) {
                return;
            }
            checkForError("find (for delete)", code);
            Pointer item = itemRef.getValue();
            if (item != null) {
                checkForError("delete", library.SecKeychainItemDelete(item));
                library.CFRelease(item);
            }
            return;
        }

        byte[] userName = (attributes.getUserName() != null ? StringUtil.nullize(attributes.getUserName()) : credentials.getUserName()).getBytes();
        byte[] searchUserName = attributes.getServiceName().equals(CredentialAttributesUtil.SERVICE_NAME_PREFIX) ? userName : null;
        PointerByReference itemRef = new PointerByReference();
        checkForError("find (for save)", library.SecKeychainFindGenericPassword(null,
            serviceName.length,
            serviceName,
            searchUserName != null ? searchUserName.length : 0,
            searchUserName,
            null,
            null,
            itemRef));

        byte[] password = (attributes.isPasswordMemoryOnly() || credentials.getPassword() == null)
            ? null
            : credentials.getPassword().toByteArray(false);
        Pointer pointer = itemRef.getValue();
        if (pointer == null) {
            checkForError("save (new)", library.SecKeychainAddGenericPassword(null,
                serviceName.length,
                serviceName,
                userName != null ? userName.length : 0,
                userName,
                password != null ? password.length : 0,
                password));
        }
        else {
            SecKeychainAttribute attribute = new SecKeychainAttribute();
            attribute.tag = kSecAccountItemAttr;
            attribute.length = userName != null ? userName.length : 0;
            if (userName != null && userName.length > 0) {
                Memory userNamePointer = new Memory(userName.length);
                userNamePointer.write(0, userName, 0, userName.length);
                attribute.data = userNamePointer;
            }
            SecKeychainAttributeList attributeList = new SecKeychainAttributeList();
            attributeList.count = 1;
            attribute.write();
            attributeList.attr = attribute.getPointer();
            checkForError("save (update)", library.SecKeychainItemModifyContent(pointer, attributeList,
                password != null ? password.length : 0,
                password != null ? password : ArrayUtil.EMPTY_BYTE_ARRAY));
            library.CFRelease(pointer);
        }
        if (password != null) {
            Arrays.fill(password, (byte) 0);
        }
    }
}
