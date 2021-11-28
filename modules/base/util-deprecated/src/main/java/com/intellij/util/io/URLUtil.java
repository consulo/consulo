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
package com.intellij.util.io;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.ThreeState;
import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

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


  public static final Pattern DATA_URI_PATTERN = Pattern.compile("data:([^,;]+/[^,;]+)(;charset(?:=|:)[^,;]+)?(;base64)?,(.+)");
  public static final Pattern URL_PATTERN = Pattern.compile("\\b(mailto:|(news|(ht|f)tp(s?))://|((?<![\\p{L}0-9_.])(www\\.)))[-A-Za-z0-9+$&@#/%?=~_|!:,.;]*[-A-Za-z0-9+$&@#/%=~_|]");
  public static final Pattern FILE_URL_PATTERN = Pattern.compile("\\b(file:///)[-A-Za-z0-9+$&@#/%?=~_|!:,.;]*[-A-Za-z0-9+$&@#/%=~_|]");

  public static final String LOCALHOST_URI_PATH_PREFIX = "localhost/";

  private URLUtil() {
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
  @Deprecated
  public static InputStream openStream(@Nonnull URL url) throws IOException {
    return consulo.util.io.URLUtil.openStream(url);
  }

  @Nonnull
  @Deprecated
  public static InputStream openResourceStream(@Nonnull URL url) throws IOException {
    return consulo.util.io.URLUtil.openResourceStream(url);
  }

  /**
   * Checks whether local resource specified by {@code url} exists. Returns {@link ThreeState#UNSURE} if {@code url} point to a remote resource.
   */
  @Nonnull
  public static ThreeState resourceExists(@Nonnull URL url) {
    if (url.getProtocol().equals(FILE_PROTOCOL)) {
      return ThreeState.fromBoolean(urlToFile(url).exists());
    }
    if (url.getProtocol().equals(JAR_PROTOCOL)) {
      Pair<String, String> paths = splitJarUrl(url.getFile());
      if (paths == null) {
        return ThreeState.NO;
      }
      if (!new File(paths.first).isFile()) {
        return ThreeState.NO;
      }
      try {
        try (ZipFile file = new ZipFile(paths.first)) {
          return ThreeState.fromBoolean(file.getEntry(paths.second) != null);
        }
      }
      catch (IOException e) {
        return ThreeState.NO;
      }
    }
    return ThreeState.UNSURE;
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
  @Deprecated
  public static Pair<String, String> splitJarUrl(@Nonnull String url) {
    consulo.util.lang.Pair<String, String> pair = consulo.util.io.URLUtil.splitJarUrl(url);
    if(pair == null) {
      return null;
    }
    return Pair.create(pair.first, pair.second);
  }

  @Nonnull
  @Deprecated
  public static File urlToFile(@Nonnull URL url) {
    return consulo.util.io.URLUtil.urlToFile(url);
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
        IntList bytes = IntLists.newArrayList();
        while (i + 2 < len && s.charAt(i) == '%') {
          final int d1 = decode(s.charAt(i + 1));
          final int d2 = decode(s.charAt(i + 2));
          if (d1 != -1 && d2 != -1) {
            bytes.add(((d1 & 0xf) << 4 | d2 & 0xf));
            i += 3;
          }
          else {
            break;
          }
        }
        if (!bytes.isEmpty()) {
          final byte[] bytesArray = new byte[bytes.size()];
          for (int j = 0; j < bytes.size(); j++) {
            bytesArray[j] = (byte)bytes.get(j);
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
      Logger.getInstance(URLUtil.class).error(e);
      return null;
    }
  }

  private static int decode(char c) {
    if ((c >= '0') && (c <= '9')) return c - '0';
    if ((c >= 'a') && (c <= 'f')) return c - 'a' + 10;
    if ((c >= 'A') && (c <= 'F')) return c - 'A' + 10;
    return -1;
  }

  public static boolean containsScheme(@Nonnull String url) {
    return url.contains(SCHEME_SEPARATOR);
  }

  public static boolean isDataUri(@Nonnull String value) {
    return !value.isEmpty() && value.startsWith("data:", value.charAt(0) == '"' || value.charAt(0) == '\'' ? 1 : 0);
  }

  /**
   * Extracts byte array from given data:URL string.
   * data:URL will be decoded from base64 if it contains the marker of base64 encoding.
   *
   * @param dataUrl data:URL-like string (may be quoted)
   * @return extracted byte array or {@code null} if it cannot be extracted.
   */
  @Nullable
  public static byte[] getBytesFromDataUri(@Nonnull String dataUrl) {
    Matcher matcher = DATA_URI_PATTERN.matcher(StringUtil.unquoteString(dataUrl));
    if (matcher.matches()) {
      try {
        String content = matcher.group(4);
        return ";base64".equalsIgnoreCase(matcher.group(3)) ? Base64.getDecoder().decode(content) : content.getBytes(StandardCharsets.UTF_8);
      }
      catch (IllegalArgumentException e) {
        return null;
      }
    }
    return null;
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

  @Nonnull
  public static URL getJarEntryURL(@Nonnull File file, @Nonnull String pathInJar) throws MalformedURLException {
    return getJarEntryURL(file.toURI(), pathInJar);
  }

  @Nonnull
  public static URL getJarEntryURL(@Nonnull URI file, @Nonnull String pathInJar) throws MalformedURLException {
    String fileURL = StringUtil.replace(file.toASCIIString(), "!", "%21");
    return new URL(JAR_PROTOCOL + ':' + fileURL + JAR_SEPARATOR + StringUtil.trimLeading(pathInJar, '/'));
  }

  /**
   * Encodes a URI component by replacing each instance of certain characters by one, two, three,
   * or four escape sequences representing the UTF-8 encoding of the character.
   * Behaves similarly to standard JavaScript build-in function <a href="https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent">encodeURIComponent</a>.
   *
   * @param s a component of a URI
   * @return a new string representing the provided string encoded as a URI component
   */
  @Nonnull
  public static String encodeURIComponent(@Nonnull String s) {
    try {
      return URLEncoder.encode(s, CharsetToolkit.UTF8).replace("+", "%20").replace("%21", "!").replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%7E", "~");
    }
    catch (UnsupportedEncodingException e) {
      return s;
    }
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

      if (SystemInfoRt.isWindows) {
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
    else if (SystemInfoRt.isWindows && (index + 3) < url.length() && url.charAt(index + 3) == '/' && url.regionMatches(0, URLUtil.FILE_PROTOCOL_PREFIX, 0, FILE_PROTOCOL_PREFIX.length())) {
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