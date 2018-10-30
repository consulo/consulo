/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.credentialStore.kdbx;

import com.intellij.credentialStore.OneTimeString;
import com.intellij.util.io.ByteBufferUtil;
import org.bouncycastle.crypto.SkippingStreamCipher;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

/**
 * from kotlin
 */
public class StringProtectedByStreamCipher implements SecureString {
  private final SkippingStreamCipher cipher;

  private long position;
  private byte[] data;

  public StringProtectedByStreamCipher(CharSequence value, SkippingStreamCipher cipher) {
    this(ByteBufferUtil.toByteArray(StandardCharsets.UTF_8.encode(CharBuffer.wrap(value))), cipher);
  }

  public StringProtectedByStreamCipher(byte[] value, SkippingStreamCipher cipher) {
    this.cipher = cipher;

    long position;
    data = new byte[value.length];
    synchronized (cipher) {
      position = cipher.getPosition();
      cipher.processBytes(value, 0, value.length, data, 0);
    }

    this.position = position;
  }

  @Override
  public OneTimeString get(boolean clearable) {
    try {
      return OneTimeString.from(getAsByteArray(), clearable);
    }
    catch (CharacterCodingException e) {
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
}
