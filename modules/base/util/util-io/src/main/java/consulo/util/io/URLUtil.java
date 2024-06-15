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
import consulo.util.io.internal.OSInfo;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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
  public static final String LOCALHOST_URI_PATH_PREFIX = "localhost/";

  public static final Pattern DATA_URI_PATTERN = Pattern.compile("data:([^,;]+/[^,;]+)(;charset(?:=|:)[^,;]+)?(;base64)?,(.+)");
  public static final Pattern URL_PATTERN = Pattern.compile("\\b(mailto:|(news|(ht|f)tp(s?))://|((?<![\\p{L}0-9_.])(www\\.)))[-A-Za-z0-9+$&@#/%?=~_|!:,.;]*[-A-Za-z0-9+$&@#/%=~_|]");
  public static final Pattern FILE_URL_PATTERN = Pattern.compile("\\b(file:///)[-A-Za-z0-9+$&@#/%?=~_|!:,.;]*[-A-Za-z0-9+$&@#/%=~_|]");

  // The pattern for 'scheme' mainly according to RFC1738.
  // We have to violate the RFC since we need to distinguish
  // real schemes from local Windows paths; The only difference
  // with RFC is that we do not allow schemes with length=1 (in other case
  // local paths like "C:/temp/index.html" would be erroneously interpreted as
  // external URLs.)
  private static final Pattern ourExternalPrefix = Pattern.compile("^[\\w\\+\\.\\-]{2,}:");

  public static boolean isAbsoluteURL(@Nonnull String url) {
    return ourExternalPrefix.matcher(url.toLowerCase()).find();
  }

  /**
   * @return if false, then the line contains no URL; if true, then more heavy {@link #URL_PATTERN} check should be used.
   */
  public static boolean canContainUrl(@Nonnull String line) {
    return line.contains("mailto:") || line.contains("://") || line.contains("www.");
  }

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

  public static boolean containsScheme(@Nonnull String url) {
    return url.contains(SCHEME_SEPARATOR);
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
  public static URL getJarEntryURL(@Nonnull File file, @Nonnull String pathInJar) throws MalformedURLException {
    return getJarEntryURL(file.toURI(), pathInJar);
  }

  @Nonnull
  public static URL getJarEntryURL(@Nonnull URI file, @Nonnull String pathInJar) throws MalformedURLException {
    String fileURL = StringUtil.replace(file.toASCIIString(), "!", "%21");
    return new URL("jar" + ':' + fileURL + "!/" + StringUtil.trimLeading(pathInJar, '/'));
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

  @Nonnull
  public static String unescapePercentSequences(@Nonnull String s) {
    if (s.indexOf('%') == -1) {
      return s;
    }

    StringBuilder decoded = new StringBuilder();
    final int len = s.length();
    int i = 0;
    while (i < len) {
      char c = s.charAt(i);
      if (c == '%') {
        List<Byte> bytes = new ArrayList<>();
        while (i + 2 < len && s.charAt(i) == '%') {
          final int d1 = decode(s.charAt(i + 1));
          final int d2 = decode(s.charAt(i + 2));
          if (d1 != -1 && d2 != -1) {
            bytes.add((byte)((d1 & 0xf) << 4 | d2 & 0xf));
            i += 3;
          }
          else {
            break;
          }
        }
        if (!bytes.isEmpty()) {
          final byte[] bytesArray = new byte[bytes.size()];
          for (int j = 0; j < bytes.size(); j++) {
            bytesArray[j] = bytes.get(j);
          }
          decoded.append(new String(bytesArray, StandardCharsets.UTF_8));
          continue;
        }
      }

      decoded.append(c);
      i++;
    }
    return decoded.toString();
  }

  public static URL internProtocol(@Nonnull URL url) {
    try {
      final String protocol = url.getProtocol();
      if ("file".equals(protocol) || "jar".equals(protocol)) {
        return new URL(protocol.intern(), url.getHost(), url.getPort(), url.getFile());
      }
      return url;
    }
    catch (MalformedURLException e) {
      LoggerFactory.getLogger(URLUtil.class).error(url.toString(), e);
      return null;
    }
  }

  @Nonnull
  public static String parseHostFromSshUrl(@Nonnull String sshUrl) {
    // [ssh://]git@github.com:user/project.git
    String host = sshUrl;
    int at = host.lastIndexOf('@');
    if (at > 0) {
      host = host.substring(at + 1);
    }
    else {
      int firstColon = host.indexOf(':');
      if (firstColon > 0) {
        host = host.substring(firstColon + 3);
      }
    }

    int colon = host.indexOf(':');
    if (colon > 0) {
      host = host.substring(0, colon);
    }
    else {
      int slash = host.indexOf('/');
      if (slash > 0) {
        host = host.substring(0, slash);
      }
    }
    return host;
  }

  private static int decode(char c) {
    if ((c >= '0') && (c <= '9')) return c - '0';
    if ((c >= 'a') && (c <= 'f')) return c - 'a' + 10;
    if ((c >= 'A') && (c <= 'F')) return c - 'A' + 10;
    return -1;
  }

  @Nonnull
  public static String toIdeaUrl(@Nonnull String url) {
    return toIdeaUrl(url, true);
  }

  @Nonnull
  public static String toIdeaUrl(@Nonnull String url, boolean removeLocalhostPrefix) {
    int index = url.indexOf(":/");
    if (index < 0 || (index + 2) >= url.length()) {
      return url;
    }

    if (url.charAt(index + 2) != '/') {
      String prefix = url.substring(0, index);
      String suffix = url.substring(index + 2);

      if (OSInfo.isWindows) {
        return prefix + URLUtil.SCHEME_SEPARATOR + suffix;
      }
      else if (removeLocalhostPrefix && prefix.equals(URLUtil.FILE_PROTOCOL) && suffix.startsWith(LOCALHOST_URI_PATH_PREFIX)) {
        // sometimes (e.g. in Google Chrome for Mac) local file url is prefixed with 'localhost' so we need to remove it
        return prefix + ":///" + suffix.substring(LOCALHOST_URI_PATH_PREFIX.length());
      }
      else {
        return prefix + ":///" + suffix;
      }
    }
    else if (OSInfo.isWindows && (index + 3) < url.length() && url.charAt(index + 3) == '/' && url.regionMatches(0, URLUtil.FILE_PROTOCOL_PREFIX, 0, FILE_PROTOCOL_PREFIX.length())) {
      // file:///C:/test/file.js -> file://C:/test/file.js
      for (int i = index + 4; i < url.length(); i++) {
        char c = url.charAt(i);
        if (c == '/') {
          break;
        }
        else if (c == ':') {
          return FILE_PROTOCOL_PREFIX + url.substring(index + 4);
        }
      }
      return url;
    }
    return url;
  }
}
