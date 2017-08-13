package com.intellij.openapi.diagnostic;

import com.intellij.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;

public class Attachment {
  public static Attachment[] EMPTY_ARRAY = new Attachment[0];

  public static ArrayFactory<Attachment> ARRAY_FACTORY = new ArrayFactory<Attachment>() {
    @NotNull
    @Override
    public Attachment[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new Attachment[count];
    }
  };

  private final String myPath;
  private final byte[] myBytes;
  private boolean myIncluded = true;
  private final String myDisplayText;

  public Attachment(String path, String content) {
    myPath = path;
    myDisplayText = content;
    myBytes = getBytes(content);
  }

  public Attachment(String path, byte[] bytes, String displayText) {
    myPath = path;
    myBytes = bytes;
    myDisplayText = displayText;
  }

  public Attachment(@NotNull String name, @NotNull Throwable throwable) {
    this(name + ".trace", ExceptionUtil.getThrowableText(throwable));
  }

  public static byte[] getBytes(String content) {
    try {
      return content.getBytes("UTF-8");
    }
    catch (UnsupportedEncodingException ignored) {
      return ArrayUtil.EMPTY_BYTE_ARRAY;
    }
  }

  public String getDisplayText() {
    return myDisplayText;
  }

  public String getPath() {
    return myPath;
  }

  public String getName() {
    return PathUtilRt.getFileName(myPath);
  }

  public String getEncodedBytes() {
    return Base64Converter.encode(myBytes);
  }

  public boolean isIncluded() {
    return myIncluded;
  }

  public void setIncluded(Boolean included) {
    myIncluded = included;
  }
}
