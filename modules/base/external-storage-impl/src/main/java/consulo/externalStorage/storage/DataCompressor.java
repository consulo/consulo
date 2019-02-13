/*
 * Copyright 2013-2019 consulo.io
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
package consulo.externalStorage.storage;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.io.DataOutputStream;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.intellij.util.io.UnsyncByteArrayOutputStream;
import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;

import javax.annotation.Nonnull;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 2019-02-11
 */
public class DataCompressor {
  public static final byte SNAPPY_V1 = 1;

  // data info
  // 1 byte -> version
  // 4 bytes -> modification count
  // 2 bytes -> content length
  // > content (compressed or not - based on version)
  private static final int ourVersion = SNAPPY_V1;

  public static byte[] compress(byte[] content, int modificationCount) throws IOException {
    byte[] compressedData = null;

    switch (ourVersion) {
      case SNAPPY_V1: {
        UnsyncByteArrayOutputStream compressStream = new UnsyncByteArrayOutputStream(content.length);
        try (SnappyOutputStream snappyOutputStream = new SnappyOutputStream(compressStream)) {
          snappyOutputStream.write(content);
        }
        compressedData = compressStream.toByteArray();
        break;
      }
      default:
        throw new UnsupportedOperationException("Unknown version " + ourVersion);
    }

    UnsyncByteArrayOutputStream outStream = new UnsyncByteArrayOutputStream(compressedData.length + 3);
    try (DataOutputStream outputStream = new DataOutputStream(outStream)) {
      outputStream.write(ourVersion);
      outputStream.writeInt(modificationCount);
      outputStream.writeShort(compressedData.length);
      outputStream.write(compressedData);
    }

    return outStream.toByteArray();
  }

  /**
   * @return pair bytes and modification count
   */
  public static Pair<byte[], Integer> uncompress(@Nonnull InputStream stream) throws IOException {
    try (DataInputStream inputStream = new DataInputStream(stream)) {
      byte version = inputStream.readByte();
      int modCount = inputStream.readInt();
      int length = inputStream.readUnsignedShort();
      switch (version) {
        case SNAPPY_V1: {
          byte[] compressedData = new byte[length];
          int read = inputStream.read(compressedData);
          if (read != length) {
            throw new IllegalArgumentException("Can't read full byte array");
          }

          try (SnappyInputStream snappyInputStream = new SnappyInputStream(new UnsyncByteArrayInputStream(compressedData))) {
            return Pair.create(StreamUtil.loadFromStream(snappyInputStream), modCount);
          }
        }
        default:
          throw new UnsupportedOperationException("Unknown version " + version);
      }
    }
  }
}
