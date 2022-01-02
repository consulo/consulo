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
package consulo.util.nodep.io;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

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
}
