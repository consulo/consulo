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
package com.intellij.credentialStore;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * @author VISTALL
 * @since 2018-10-28
 */
public class AesEncryptionSupport implements EncryptionSupport {
  private final Key myKey;

  public AesEncryptionSupport(Key key) {
    myKey = key;
  }

  @Override
  public byte[] encypt(byte[] message) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, myKey);
      byte[] body = cipher.doFinal(message, 0, message.length);
      byte[] iv = cipher.getIV();

      ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4 + iv.length + body.length]);
      byteBuffer.putInt(iv.length);
      byteBuffer.put(iv);
      byteBuffer.put(body);
      return byteBuffer.array();
    }
    catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] decrypt(byte[] data) {
    try {
      ByteBuffer byteBuffer = ByteBuffer.wrap(data);
      int ivLength = byteBuffer.getInt();
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, myKey, new IvParameterSpec(data, byteBuffer.position(), ivLength));
      int dataOffset = byteBuffer.position() + ivLength;
      return cipher.doFinal(data, dataOffset, data.length - dataOffset);
    }
    catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }
}
