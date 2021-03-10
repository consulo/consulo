// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import consulo.util.nodep.ArrayUtilRt;
import consulo.util.nodep.io.FileUtilRt;
import consulo.util.nodep.io.UnsyncByteArrayInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MemoryResource extends Resource {
  protected final URL myUrl;
  private final byte[] myContent;
  private final Map<Resource.Attribute, String> myAttributes;

   public MemoryResource(URL url, byte[] content, Map<Resource.Attribute, String> attributes) {
    myUrl = url;
    myContent = content;
    myAttributes = attributes;
  }

  @Override
  public URL getURL() {
    return myUrl;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new UnsyncByteArrayInputStream(myContent);
  }

  @Override
  public byte[] getBytes() throws IOException {
    return myContent;
  }

  @Override
  public String getValue(Attribute key) {
    return myAttributes != null ? myAttributes.get(key) : null;
  }

  @Nonnull
  static MemoryResource load(URL baseUrl, @Nonnull ZipFile zipFile, @Nonnull ZipEntry entry, @Nullable Map<Attribute, String> attributes) throws IOException {
    String name = entry.getName();
    URL url = new URL(baseUrl, name);

    byte[] content = ArrayUtilRt.EMPTY_BYTE_ARRAY;
    InputStream stream = zipFile.getInputStream(entry);
    if (stream != null) {
      try {
        content = FileUtilRt.loadBytes(stream, (int)entry.getSize());
      }
      finally {
        stream.close();
      }
    }

    return new MemoryResource(url, content, attributes);
  }
}
