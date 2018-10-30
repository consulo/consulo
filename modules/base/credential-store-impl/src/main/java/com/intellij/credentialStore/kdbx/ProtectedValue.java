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
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.jdom.Text;

import javax.annotation.Nonnull;
import java.nio.charset.CharacterCodingException;
import java.util.Base64;

/**
 * from kotlin
 */
public class ProtectedValue extends Text implements SecureString {
  private byte[] encryptedValue;
  private int position;
  private SkippingStreamCipher streamCipher;

  public ProtectedValue(byte[] encryptedValue, int position, SkippingStreamCipher streamCipher) {
    this.encryptedValue = encryptedValue;
    this.position = position;
    this.streamCipher = streamCipher;
  }

  @Override
  public synchronized OneTimeString get(boolean clearable) {
    byte[] output = new byte[encryptedValue.length];
    decryptInto(output);
    try {
      return OneTimeString.from(output, clearable);
    }
    catch (CharacterCodingException e) {
      throw new RuntimeException(e);
    }
  }

  private synchronized void decryptInto(byte[] out) {
    streamCipher.seekTo(position);
    streamCipher.processBytes(encryptedValue, 0, encryptedValue.length, out, 0);
  }

  public synchronized void setNewStreamCipher(SkippingStreamCipher newStreamCipher) {
    byte[] value = encryptedValue;
    decryptInto(value);

    position = (int)newStreamCipher.getPosition();
    newStreamCipher.processBytes(value, 0, value.length, value, 0);
    streamCipher = newStreamCipher;
  }

  @Nonnull
  public String encodeToBase64() {
    if (encryptedValue.length == 0) {
      return "";
    }
    else {
      return Base64.getEncoder().encodeToString(encryptedValue);
    }
  }

  @Override
  public String getText() {
    throw new IllegalStateException("encodeToBase64 must be used for serialization");
  }
}
