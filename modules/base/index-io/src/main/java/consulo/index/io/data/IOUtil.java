// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.index.io.data;

import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreadLocalCachedValue;
import consulo.util.lang.function.ThrowableSupplier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IOUtil {
  public static final boolean BYTE_BUFFERS_USE_NATIVE_BYTE_ORDER = true;//SystemProperties.getBooleanProperty("idea.bytebuffers.use.native.byte.order", true);

  private static final int STRING_HEADER_SIZE = 1;
  private static final int STRING_LENGTH_THRESHOLD = 255;
  private static final String LONGER_THAN_64K_MARKER = "LONGER_THAN_64K";

  private IOUtil() {
  }

  public static String readString(@Nonnull DataInput stream) throws IOException {
    try {
      int length = stream.readInt();
      if (length == -1) return null;
      if (length == 0) return "";

      byte[] bytes = new byte[length * 2];
      stream.readFully(bytes);
      return new String(bytes, 0, length * 2, StandardCharsets.UTF_16BE);
    }
    catch (IOException e) {
      throw e;
    }
    catch (Throwable e) {
      throw new IOException(e);
    }
  }

  public static void writeString(@Nullable String s, @Nonnull DataOutput stream) throws IOException {
    if (s == null) {
      stream.writeInt(-1);
      return;
    }

    stream.writeInt(s.length());
    if (s.isEmpty()) {
      return;
    }

    char[] chars = s.toCharArray();
    byte[] bytes = new byte[chars.length * 2];

    for (int i = 0, i2 = 0; i < chars.length; i++, i2 += 2) {
      char aChar = chars[i];
      bytes[i2] = (byte)(aChar >>> 8 & 0xFF);
      bytes[i2 + 1] = (byte)(aChar & 0xFF);
    }

    stream.write(bytes);
  }

  public static void writeUTFTruncated(@Nonnull DataOutput stream, @Nonnull String text) throws IOException {
    // we should not compare number of symbols to 65635 -> it is number of bytes what should be compared
    // ? 4 bytes per symbol - rough estimation
    if (text.length() > 16383) {
      stream.writeUTF(text.substring(0, 16383));
    }
    else {
      stream.writeUTF(text);
    }
  }

  private static final ThreadLocalCachedValue<byte[]> ourReadWriteBuffersCache = new ThreadLocalCachedValue<byte[]>() {
    @Nonnull
    @Override
    protected byte[] create() {
      return allocReadWriteUTFBuffer();
    }
  };

  public static void writeUTF(@Nonnull DataOutput storage, @Nonnull String value) throws IOException {
    writeUTFFast(ourReadWriteBuffersCache.getValue(), storage, value);
  }

  public static String readUTF(@Nonnull DataInput storage) throws IOException {
    return readUTFFast(ourReadWriteBuffersCache.getValue(), storage);
  }

  @Nonnull
  public static byte[] allocReadWriteUTFBuffer() {
    return new byte[STRING_LENGTH_THRESHOLD + STRING_HEADER_SIZE];
  }

  public static void writeUTFFast(@Nonnull byte[] buffer, @Nonnull DataOutput storage, @Nonnull String value) throws IOException {
    int len = value.length();
    if (len < STRING_LENGTH_THRESHOLD) {
      buffer[0] = (byte)len;
      boolean isAscii = true;
      for (int i = 0; i < len; i++) {
        char c = value.charAt(i);
        if (c >= 128) {
          isAscii = false;
          break;
        }
        buffer[i + STRING_HEADER_SIZE] = (byte)c;
      }
      if (isAscii) {
        storage.write(buffer, 0, len + STRING_HEADER_SIZE);
        return;
      }
    }
    storage.writeByte((byte)0xFF);

    try {
      storage.writeUTF(value);
    }
    catch (UTFDataFormatException e) {
      storage.writeUTF(LONGER_THAN_64K_MARKER);
      writeString(value, storage);
    }
  }

  public static String readUTFFast(@Nonnull byte[] buffer, @Nonnull DataInput storage) throws IOException {
    int len = 0xFF & (int)storage.readByte();
    if (len == 0xFF) {
      String result = storage.readUTF();
      if (LONGER_THAN_64K_MARKER.equals(result)) {
        return readString(storage);
      }

      return result;
    }

    if (len == 0) return "";
    storage.readFully(buffer, 0, len);

    return new String(buffer, 0, len, StandardCharsets.ISO_8859_1);
  }

  public static boolean isAscii(@Nonnull String str) {
    return StringUtil.isAscii(str);
  }

  public static boolean isAscii(@Nonnull CharSequence str) {
    return StringUtil.isAscii(str);
  }

  public static boolean isAscii(char c) {
    return StringUtil.isAscii(c);
  }

  public static boolean deleteAllFilesStartingWith(@Nonnull File file) {
    String baseName = file.getName();
    File parentFile = file.getParentFile();
    File[] files = parentFile != null ? parentFile.listFiles(pathname -> pathname.getName().startsWith(baseName)) : null;

    boolean ok = true;
    if (files != null) {
      for (File f : files) {
        ok &= FileUtil.delete(f);
      }
    }

    return ok;
  }

  public static void syncStream(@Nonnull OutputStream stream) throws IOException {
    stream.flush();

    try {
      Field outField = FilterOutputStream.class.getDeclaredField("out");
      outField.setAccessible(true);
      while (stream instanceof FilterOutputStream) {
        Object o = outField.get(stream);
        if (o instanceof OutputStream) {
          stream = (OutputStream)o;
        }
        else {
          break;
        }
      }
      if (stream instanceof FileOutputStream) {
        ((FileOutputStream)stream).getFD().sync();
      }
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T openCleanOrResetBroken(@Nonnull ThrowableSupplier<T, ? extends IOException> factoryComputable, @Nonnull File file) throws IOException {
    return openCleanOrResetBroken(factoryComputable, () -> deleteAllFilesStartingWith(file));
  }

  public static <T> T openCleanOrResetBroken(@Nonnull ThrowableSupplier<T, ? extends IOException> factoryComputable, @Nonnull Runnable cleanupCallback) throws IOException {
    try {
      return factoryComputable.get();
    }
    catch (IOException ex) {
      cleanupCallback.run();
    }

    return factoryComputable.get();
  }

  public static void writeStringList(@Nonnull DataOutput out, @Nonnull Collection<String> list) throws IOException {
    DataInputOutputUtil.writeINT(out, list.size());
    for (String s : list) {
      writeUTF(out, s);
    }
  }

  @Nonnull
  public static List<String> readStringList(@Nonnull DataInput in) throws IOException {
    int size = DataInputOutputUtil.readINT(in);
    List<String> strings = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      strings.add(readUTF(in));
    }
    return strings;
  }
}