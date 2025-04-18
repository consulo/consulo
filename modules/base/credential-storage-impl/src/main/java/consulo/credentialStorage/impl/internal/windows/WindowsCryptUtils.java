// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.credentialStorage.impl.internal.windows;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Crypt32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinCrypt;
import jakarta.annotation.Nonnull;

/**
 * Windows Utilities for the Password Safe
 */
public final class WindowsCryptUtils {
    private WindowsCryptUtils() {
    }

    /**
     * Protect the specified byte range
     *
     * @param data the data to protect
     * @return the protected form of the data
     */
    @Nonnull
    public static byte[] protect(@Nonnull byte[] data) {
        if (data.length == 0) {
            return data;
        }
        WinCrypt.DATA_BLOB in = prepareInput(data);
        WinCrypt.DATA_BLOB out = new WinCrypt.DATA_BLOB.ByReference();
        boolean rc = Crypt32.INSTANCE.CryptProtectData(in, "Master Key", null, null, null, 0, out);
        return getBytes(out, rc);
    }

    /**
     * Unprotect the specified byte range
     *
     * @param data the data to protect
     * @return the unprotected form of the data
     */
    @Nonnull
    public static byte[] unprotect(byte[] data) {
        if (data.length == 0) {
            return data;
        }
        WinCrypt.DATA_BLOB in = prepareInput(data);
        WinCrypt.DATA_BLOB out = new WinCrypt.DATA_BLOB.ByReference();
        boolean rc = Crypt32.INSTANCE.CryptUnprotectData(in, null, null, null, null, 0, out);
        return getBytes(out, rc);
    }

    private static WinCrypt.DATA_BLOB prepareInput(byte[] data) {
        Memory input = new Memory(data.length);
        input.write(0, data, 0, data.length);
        WinCrypt.DATA_BLOB in = new WinCrypt.DATA_BLOB.ByReference();
        in.cbData = data.length;
        in.pbData = input;
        return in;
    }

    private static byte[] getBytes(WinCrypt.DATA_BLOB out, boolean rc) {
        if (!rc) {
            throw new RuntimeException("CryptProtectData failed: " + Kernel32.INSTANCE.GetLastError());
        }

        byte[] output = new byte[out.cbData];
        out.pbData.read(0, output, 0, output.length);
        Kernel32.INSTANCE.LocalFree(out.pbData);
        return output;
    }
}
