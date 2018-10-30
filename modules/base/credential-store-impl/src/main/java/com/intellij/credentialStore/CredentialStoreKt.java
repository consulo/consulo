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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin
 */
public class CredentialStoreKt {
  private static final char ESCAPING_CHAR = '\\';
  public static final Credentials ACCESS_TO_KEY_CHAIN_DENIED = new Credentials(null, (OneTimeString)null);

  public static byte[] serialize(Credentials credentials) {
    return serialize(credentials, true);
  }

  // check isEmpty before
  public static byte[] serialize(Credentials credentials, boolean storePassword) {
    return joinData(credentials.getUserName(), storePassword ? credentials.getPassword() : null);
  }

  public static byte[] joinData(String user, OneTimeString password) {
    if (user == null && password == null) {
      return null;
    }

    StringBuilder builder = new StringBuilder(StringUtil.notNullize(user));
    StringUtil.escapeChar(builder, '\\');
    StringUtil.escapeChar(builder, '@');

    if (password != null) {
      builder.append("@");
      password.appendTo(builder);
    }

    ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(builder));
    // clear password
    builder.setLength(0);
    return ByteBufferUtil.toByteArray(buffer);
  }

  public static Credentials splitData(String data) {
    if (StringUtil.isEmpty(data)) {
      return null;
    }

    List<String> list = parseString(data, '@');
    return new Credentials(ContainerUtil.getOrNull(list, 0), ContainerUtil.getOrNull(list, 1));
  }


  private static List<String> parseString(String data, char delimiter) {
    StringBuilder part = new StringBuilder();
    List<String> result = new ArrayList<>(2);
    int i = 0;
    Character c;

    do {
      c = StringUtil.getOrNull(data, i++);
      if (c != null && c != delimiter) {
        if (c == ESCAPING_CHAR) {
          c = StringUtil.getOrNull(data, i++);
        }

        if (c != null) {
          part.append(c);
          continue;
        }
      }

      result.add(part.toString());
      part.setLength(0);

      if (i < data.length()) {
        result.add(data.substring(i));
        break;
      }
    }
    while (c != null);

    return result;
  }

  public static synchronized byte[] generateBytes(SecureRandom secureRandom, int size) {
    byte[] result = new byte[size];
    secureRandom.nextBytes(result);
    return result;
  }

  public static SecureRandom createSecureRandom() {
    // do not use SecureRandom.getInstanceStrong()
    // https://tersesystems.com/blog/2015/12/17/the-right-way-to-use-securerandom/
    // it leads to blocking without any advantages
    return new SecureRandom();
  }
}
