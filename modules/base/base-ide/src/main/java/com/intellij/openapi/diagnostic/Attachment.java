package com.intellij.openapi.diagnostic;

import com.intellij.util.*;
import consulo.annotation.DeprecationInfo;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;

@Deprecated
@DeprecationInfo("Use consulo.logging.attachment.Attachment")
public class Attachment implements consulo.logging.attachment.Attachment {
  public static Attachment[] EMPTY_ARRAY = new Attachment[0];

  public static ArrayFactory<Attachment> ARRAY_FACTORY = new ArrayFactory<Attachment>() {
    @Nonnull
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

  public Attachment(@Nonnull String name, @Nonnull Throwable throwable) {
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

  @Override
  public String getDisplayText() {
    return myDisplayText;
  }

  @Override
  public String getPath() {
    return myPath;
  }

  @Override
  public String getName() {
    return PathUtilRt.getFileName(myPath);
  }

  @Override
  public String getEncodedBytes() {
    return Base64Converter.encode(myBytes);
  }

  @Override
  public boolean isIncluded() {
    return myIncluded;
  }

  @Override
  public void setIncluded(Boolean included) {
    myIncluded = included;
  }
}
