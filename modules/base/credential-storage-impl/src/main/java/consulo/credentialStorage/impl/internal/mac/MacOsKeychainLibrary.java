package consulo.credentialStorage.impl.internal.mac;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import consulo.credentialStorage.impl.internal.mac.SecKeychainAttributeInfo;
import consulo.credentialStorage.impl.internal.mac.SecKeychainAttributeList;

public interface MacOsKeychainLibrary extends Library {
    int SecKeychainAddGenericPassword(Pointer keychain,
                                      int serviceNameLength,
                                      byte[] serviceName,
                                      int accountNameLength,
                                      byte[] accountName,
                                      int passwordLength,
                                      byte[] passwordData,
                                      Pointer itemRef);

    int SecKeychainItemModifyContent(Pointer itemRef, Object attrList, int length, byte[] data);

    int SecKeychainFindGenericPassword(Pointer keychainOrArray,
                                       int serviceNameLength,
                                       byte[] serviceName,
                                       int accountNameLength,
                                       byte[] accountName,
                                       int[] passwordLength,
                                       PointerByReference passwordData,
                                       PointerByReference itemRef);

    int SecKeychainItemCopyAttributesAndData(Pointer itemRef,
                                             SecKeychainAttributeInfo info,
                                             IntByReference itemClass,
                                             PointerByReference attrList,
                                             IntByReference length,
                                             PointerByReference outData);

    int SecKeychainItemFreeAttributesAndData(SecKeychainAttributeList attrList, Pointer data);

    int SecKeychainItemDelete(Pointer itemRef);

    Pointer SecCopyErrorMessageString(int status, Pointer reserved);

    long CFStringGetLength(Pointer theString);

    char CFStringGetCharacterAtIndex(Pointer theString, long idx);

    void CFRelease(Pointer cf);

    void SecKeychainItemFreeContent(Pointer attrList, Pointer data);
}
