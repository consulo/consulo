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
package com.intellij.util.io;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * from kotlin
 */
public class ByteBufferUtil {
  public static byte[] toByteArray(ByteBuffer byteBuffer) {
    return toByteArray(byteBuffer, false);
  }

  public static byte[] toByteArray(ByteBuffer byteBuffer, boolean isClear) {
    if (byteBuffer.hasArray()) {
      int offset = byteBuffer.arrayOffset();
      byte[] array = byteBuffer.array();
      if (offset == 0 && array.length == byteBuffer.limit()) {
        return array;
      }

      byte[] result = Arrays.copyOfRange(array, offset, offset + byteBuffer.limit());
      if (isClear) {
        Arrays.fill(array, (byte)0);
      }
      return result;
    }

    byte[] bytes = new byte[byteBuffer.limit()];
    byteBuffer.get(bytes);
    return bytes;
  }
}
