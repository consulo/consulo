/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.util;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author anna
 * @since 18-May-2009
 */
public class CoverageIOUtil {
  private static final int STRING_HEADER_SIZE = 1;
  private static final int STRING_LENGTH_THRESHOLD = 255;

  private static final String LONGER_THAN_64K_MARKER = "LONGER_THAN_64K";


  private CoverageIOUtil() {
  }


  public static String readString(DataInput stream) throws IOException {
    int length = stream.readInt();
    if (length == -1) return null;

    char[] chars = new char[length];
    byte[] bytes = new byte[length*2];
    stream.readFully(bytes);

    for (int i = 0, i2 = 0; i < length; i++, i2+=2) {
      chars[i] = (char)((bytes[i2] << 8) + (bytes[i2 + 1] & 0xFF));
    }

    return new String(chars);
  }

  public static void writeString(String s, DataOutput stream) throws IOException {
    if (s == null) {
      stream.writeInt(-1);
      return;
    }
    char[] chars = s.toCharArray();
    byte[] bytes = new byte[chars.length * 2];

    stream.writeInt(chars.length);
    for (int i = 0, i2 = 0; i < chars.length; i++, i2 += 2) {
      char aChar = chars[i];
      bytes[i2] = (byte)((aChar >>> 8) & 0xFF);
      bytes[i2 + 1] = (byte)((aChar) & 0xFF);
    }

    stream.write(bytes);
  }

  private final static byte[] ioBuffer = allocReadWriteUTFBuffer();

  public static byte[] allocReadWriteUTFBuffer() {
    return new byte[STRING_LENGTH_THRESHOLD + STRING_HEADER_SIZE];
  }

  public static void writeUTF(final DataOutput storage, final String value) throws IOException {
    int len = value.length();
    if (len < STRING_LENGTH_THRESHOLD && isAscii(value)) {
      ioBuffer[0] = (byte)len;
      for (int i = 0; i < len; i++) {
        ioBuffer[i + STRING_HEADER_SIZE] = (byte)value.charAt(i);
      }
      storage.write(ioBuffer, 0, len + STRING_HEADER_SIZE);
    }
    else {
      storage.writeByte((byte)0xFF);

      try {
        storage.writeUTF(value);
      }
      catch (UTFDataFormatException e) {
        storage.writeUTF(LONGER_THAN_64K_MARKER);
        writeString(value, storage);
      }
    }
  }

  public static String readUTFFast(final DataInput storage) throws IOException {
    int len = 0xFF & (int)storage.readByte();
    if (len == 0xFF) {
      String result = storage.readUTF();
      if (LONGER_THAN_64K_MARKER.equals(result)) {
        return readString(storage);
      }

      return result;
    }

    final char[] chars = new char[len];
    storage.readFully(ioBuffer, 0, len);
    for (int i = 0; i < len; i++) {
      chars[i] = (char)ioBuffer[i];
    }

    return new String(chars);
  }

  static boolean isAscii(final String str) {
    for (int i = 0; i != str.length(); ++ i) {
      final char c = str.charAt(i);
      if (c < 0 || c >= 128) {
        return false;
      }
    }
    return true;
  }

  public static int readINT(DataInput record) throws IOException {
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

  public static void writeINT(DataOutput record, int val) throws IOException {
    /*
    if (0 <= val && val < 255)
      record.writeByte(val);
    else {
      record.writeByte(255);
      record.writeInt(val);
    }
    */
    if (0 <= val && val < 192) {
      record.writeByte(val);
    }
    else {
      record.writeByte(192 + (val & 0x3F));
      val >>>= 6;
      while (val >= 128) {
        record.writeByte((val & 0x7F) | 0x80);
        val >>>= 7;
      }
      record.writeByte(val);
    }
  }


  public static String collapse(String methodSignature, final DictionaryLookup dictionaryLookup) {
    return processWithDictionary(methodSignature, new Consumer() {
      protected String consume(String type) {
        final int dictionaryIndex = dictionaryLookup.getDictionaryIndex(type);
        return dictionaryIndex >= 0 ? String.valueOf(dictionaryIndex) : type;
      }
    });
  }

  private static final Pattern TYPE_PATTERN = Pattern.compile("(L.*;)*");

  public static abstract class Consumer {
    protected abstract String consume(String str);
  }
  public static String processWithDictionary(String methodSignature, Consumer consumer) {
    final Matcher matcher = TYPE_PATTERN.matcher(methodSignature);
    while (matcher.find()) {
      String s = matcher.group();
      if (s.startsWith("L") && s.endsWith(";")) {
        final String type = s.substring(1, s.length() - 1);
        methodSignature = methodSignature.replace(type, consumer.consume(type));
      }
    }
    return methodSignature;
  }

  public static DataOutputStream openFile(File file) throws FileNotFoundException {
    return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
  }

  public static void close(DataOutputStream out) {
    if (out != null) {
      try {
        out.close();
      } catch (IOException ignored) {}
    }
  }
}