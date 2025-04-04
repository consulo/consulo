/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.credentialStorage.impl.internal.kdbx;

import consulo.credentialStorage.OneTimeString;
import org.bouncycastle.crypto.SkippingStreamCipher;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/**
 * StringProtectedByStreamCipher wraps a byte array protected by a SkippingStreamCipher and implements SecureString.
 */
public class StringProtectedByStreamCipher implements SecureString {
    private final long position;
    private final byte[] data;
    private final SkippingStreamCipher cipher;

    public StringProtectedByStreamCipher(byte[] value, SkippingStreamCipher cipher) {
        this.cipher = cipher;
        this.data = new byte[value.length];
        long pos;
        synchronized (cipher) {
            pos = cipher.getPosition();
            cipher.processBytes(value, 0, value.length, data, 0);
        }
        this.position = pos;
    }

    public StringProtectedByStreamCipher(CharSequence value, SkippingStreamCipher cipher) {
        this(encodeToByteArray(value), cipher);
    }

    @Override
    public OneTimeString get(boolean clearable) {
        try {
            return OneTimeString.fromByteArray(getAsByteArray(), clearable);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getAsByteArray() {
        byte[] value = new byte[data.length];
        synchronized (cipher) {
            cipher.seekTo(position);
            cipher.processBytes(data, 0, data.length, value, 0);
        }
        return value;
    }

    // Helper method to encode a CharSequence into a UTF-8 byte array.
    private static byte[] encodeToByteArray(CharSequence cs) {
        CharBuffer charBuffer = CharBuffer.wrap(cs);
        return StandardCharsets.UTF_8.encode(charBuffer).array();
    }
}
