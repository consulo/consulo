/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.util.io;

import consulo.application.util.function.ThrowableComputable;
import consulo.util.lang.function.ThrowableConsumer;
import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

@Deprecated
public class DataInputOutputUtilRt {
  public static int readINT(@Nonnull DataInput record) throws IOException {
    final int val = record.readUnsignedByte();
    if (val < 192) {
      return val;
    }

    int res = val - 192;
    for (int sh = 6; ; sh += 7) {
      int next = record.readUnsignedByte();
      res |= (next & 0x7F) << sh;
      if ((next & 0x80) == 0) {
        return res;
      }
    }
  }

  public static int readINT(@Nonnull ByteBuffer byteBuffer) {
    final int val = byteBuffer.get() & 0xFF;
    if (val < 192) {
      return val;
    }

    int res = val - 192;
    for (int sh = 6; ; sh += 7) {
      int next = byteBuffer.get() & 0xFF;
      res |= (next & 0x7F) << sh;
      if ((next & 0x80) == 0) {
        return res;
      }
    }
  }

  public static void writeINT(@Nonnull DataOutput record, int val) throws IOException {
    if (0 > val || val >= 192) {
      record.writeByte(192 + (val & 0x3F));
      val >>>= 6;
      while (val >= 128) {
        record.writeByte((val & 0x7F) | 0x80);
        val >>>= 7;
      }
    }
    record.writeByte(val);
  }

  public static void writeINT(@Nonnull ByteBuffer byteBuffer, int val) {
    if (0 > val || val >= 192) {
      byteBuffer.put((byte)(192 + (val & 0x3F)));
      val >>>= 6;
      while (val >= 128) {
        byteBuffer.put((byte)((val & 0x7F) | 0x80));
        val >>>= 7;
      }
    }
    byteBuffer.put((byte)val);
  }

  /**
   * Writes the given collection to the output using the given procedure to write each element.
   * Should be coupled with {@link #readSeq}
   */
  public static <T> void writeSeq(@Nonnull DataOutput out, @Nonnull Collection<? extends T> collection, @SuppressWarnings("BoundedWildcard") @Nonnull ThrowableConsumer<T, IOException> writeElement)
          throws IOException {
    writeINT(out, collection.size());
    for (T t : collection) {
      writeElement.consume(t);
    }
  }

  /**
   * Reads a collection using the given function to read each element.
   * Should be coupled with {@link #writeSeq}
   */
  @Nonnull
  public static <T> List<T> readSeq(@Nonnull DataInput in, @SuppressWarnings("BoundedWildcard") @Nonnull ThrowableComputable<? extends T, IOException> readElement) throws IOException {
    int size = readINT(in);
    List<T> result = new ArrayList<T>(size);
    for (int i = 0; i < size; i++) {
      result.add(readElement.compute());
    }
    return result;
  }

  /**
   * Writes the given map to the output using the given procedure to write each key and value.
   * Should be coupled with {@link #readMap}
   */
  public static <K, V> void writeMap(@Nonnull DataOutput out,
                                     @Nonnull Map<? extends K, ? extends V> map,
                                     @Nonnull ThrowableConsumer<K, ? extends IOException> writeKey,
                                     @Nonnull ThrowableConsumer<V, ? extends IOException> writeValue) throws IOException {
    writeINT(out, map.size());
    for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
      writeKey.consume(e.getKey());
      writeValue.consume(e.getValue());
    }
  }

  /**
   * Reads a map using the given function to read each element.
   * Should be coupled with {@link #writeMap}
   */
  @Nonnull
  public static <K, V> Map<K, V> readMap(@Nonnull DataInput in,
                                         @Nonnull ThrowableComputable<? extends K, ? extends IOException> readKey,
                                         @Nonnull ThrowableComputable<? extends V, ? extends IOException> readValue) throws IOException {
    int size = readINT(in);
    Map<K, V> result = new HashMap<K, V>();
    for (int i = 0; i < size; i++) {
      result.put(readKey.compute(), readValue.compute());
    }
    return result;
  }
}
