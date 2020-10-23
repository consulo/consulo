/*
 * Copyright 2013-2020 consulo.io
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
package consulo.util.io;

import consulo.annotation.DeprecationInfo;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 2020-10-22
 */
public class URLUtil {
  public static final String SCHEME_SEPARATOR = "://";
  public static final String FILE_PROTOCOL = "file";
  public static final String FILE_PROTOCOL_PREFIX = FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR;
  public static final String HTTP_PROTOCOL = "http";
  @Deprecated
  public static final String JAR_PROTOCOL = "jar";
  public static final String ARCHIVE_SEPARATOR = "!/";
  @Deprecated
  @DeprecationInfo(value = "Use #ARCHIVE_SEPARATOR")
  public static final String JAR_SEPARATOR = ARCHIVE_SEPARATOR;

  /**
   * Opens a url stream. The semantics is the sames as {@link URL#openStream()}. The
   * separate method is needed, since jar URLs open jars via JarFactory and thus keep them
   * mapped into memory.
   */
  @Nonnull
  public static InputStream openStream(@Nonnull URL url) throws IOException {
    String protocol = url.getProtocol();
    return protocol.equals(JAR_PROTOCOL) ? openJarStream(url) : url.openStream();
  }

  /**
   * Splits .jar URL along a separator and strips "jar" and "file" prefixes if any.
   * Returns a pair of path to a .jar file and entry name inside a .jar, or null if the URL does not contain a separator.
   * <p/>
   * E.g. "jar:file:///path/to/jar.jar!/resource.xml" is converted into ["/path/to/jar.jar", "resource.xml"].
   * <p/>
   * Please note that the first part is platform-dependent - see UrlUtilTest.testJarUrlSplitter() for examples.
   */
  @Nullable
  public static Pair<String, String> splitJarUrl(@Nonnull String url) {
    int pivot = url.indexOf(JAR_SEPARATOR);
    if (pivot < 0) return null;

    String resourcePath = url.substring(pivot + 2);
    String jarPath = url.substring(0, pivot);

    if (StringUtil.startsWithConcatenation(jarPath, JAR_PROTOCOL, ":")) {
      jarPath = jarPath.substring(JAR_PROTOCOL.length() + 1);
    }

    if (jarPath.startsWith(FILE_PROTOCOL)) {
      try {
        jarPath = urlToFile(new URL(jarPath)).getPath().replace('\\', '/');
      }
      catch (Exception e) {
        jarPath = jarPath.substring(FILE_PROTOCOL.length());
        if (jarPath.startsWith(SCHEME_SEPARATOR)) {
          jarPath = jarPath.substring(SCHEME_SEPARATOR.length());
        }
        else if (StringUtil.startsWithChar(jarPath, ':')) {
          jarPath = jarPath.substring(1);
        }
      }
    }

    return Pair.create(jarPath, resourcePath);
  }

  @Nonnull
  public static File urlToFile(@Nonnull URL url) {
    try {
      return new File(url.toURI().getSchemeSpecificPart());
    }
    catch (URISyntaxException e) {
      throw new IllegalArgumentException("URL='" + url.toString() + "'", e);
    }
  }

  @Nonnull
  private static InputStream openJarStream(@Nonnull URL url) throws IOException {
    Pair<String, String> paths = splitJarUrl(url.getFile());
    if (paths == null) {
      throw new MalformedURLException(url.getFile());
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") final ZipFile zipFile = new ZipFile(paths.first);
    ZipEntry zipEntry = zipFile.getEntry(paths.second);
    if (zipEntry == null) {
      zipFile.close();
      throw new FileNotFoundException("Entry " + paths.second + " not found in " + paths.first);
    }

    return new FilterInputStream(zipFile.getInputStream(zipEntry)) {
      @Override
      public void close() throws IOException {
        super.close();
        zipFile.close();
      }
    };
  }

  @Nonnull
  public static InputStream openResourceStream(@Nonnull URL url) throws IOException {
    try {
      return openStream(url);
    }
    catch (FileNotFoundException ex) {
      String protocol = url.getProtocol();
      String file = null;
      if (protocol.equals(FILE_PROTOCOL)) {
        file = url.getFile();
      }
      else if (protocol.equals(JAR_PROTOCOL)) {
        int pos = url.getFile().indexOf("!");
        if (pos >= 0) {
          file = url.getFile().substring(pos + 1);
        }
      }
      if (file != null && file.startsWith("/")) {
        InputStream resourceStream = URLUtil.class.getResourceAsStream(file);
        if (resourceStream != null) return resourceStream;
      }
      throw ex;
    }
  }
}
