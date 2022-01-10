/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.URLUtil;
import consulo.logging.Logger;
import consulo.util.collection.HashingStrategy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Urls {
  private static final Logger LOG = Logger.getInstance(Urls.class);

  // about ";" see WEB-100359
  private static final Pattern URI_PATTERN = Pattern.compile("^([^:/?#]+):(//)?([^/?#]*)([^?#;]*)(.*)");

  @Nonnull
  public static Url newUri(@Nonnull String scheme, @Nonnull String path) {
    return new UrlImpl(scheme, null, path);
  }

  @Nonnull
  public static Url newLocalFileUrl(@Nonnull String path) {
    return new LocalFileUrl(path);
  }

  @Nonnull
  public static Url newFromEncoded(@Nonnull String url) {
    Url result = parseEncoded(url);
    LOG.assertTrue(result != null, url);
    return result;
  }

  @Nullable
  public static Url parseEncoded(@Nonnull String url) {
    return parse(url, false);
  }

  @Nonnull
  public static Url newHttpUrl(@Nonnull String authority, @Nullable String path) {
    return newUrl("http", authority, path);
  }

  @Nonnull
  public static Url newUrl(@Nonnull String scheme, @Nonnull String authority, @Nullable String path) {
    return new UrlImpl(scheme, authority, path);
  }

  @Nonnull
  /**
   * Url will not be normalized (see {@link VfsUtilCore#toIdeaUrl(String)}), parsed as is
   */
  public static Url newFromIdea(@Nonnull String url) {
    Url result = parseFromIdea(url);
    LOG.assertTrue(result != null, url);
    return result;
  }

  // java.net.URI.create cannot parse "file:///Test Stuff" - but you don't need to worry about it - this method is aware
  @Nullable
  public static Url parseFromIdea(@Nonnull String url) {
    return URLUtil.containsScheme(url) ? parseUrl(url) : newLocalFileUrl(url);
  }

  @Nullable
  public static Url parse(@Nonnull String url, boolean asLocalIfNoScheme) {
    if (url.isEmpty()) {
      return null;
    }

    if (asLocalIfNoScheme && !URLUtil.containsScheme(url)) {
      // nodejs debug — files only in local filesystem
      return newLocalFileUrl(url);
    }
    return parseUrl(URLUtil.toIdeaUrl(url));
  }

  @Nullable
  public static URI parseAsJavaUriWithoutParameters(@Nonnull String url) {
    Url asUrl = parseUrl(url);
    if (asUrl == null) {
      return null;
    }

    try {
      return toUriWithoutParameters(asUrl);
    }
    catch (Exception e) {
      LOG.info("Cannot parse url " + url, e);
      return null;
    }
  }

  @Nullable
  public static Url parseUrlUnsafe(@Nonnull String url) {
    return parseUrl(url);
  }

  @Nullable
  private static Url parseUrl(@Nonnull String url) {
    String urlToParse;
    if (url.startsWith("jar:file://")) {
      urlToParse = url.substring("jar:".length());
    }
    else {
      urlToParse = url;
    }

    Matcher matcher = URI_PATTERN.matcher(urlToParse);
    if (!matcher.matches()) {
      return null;
    }
    String scheme = matcher.group(1);
    if (urlToParse != url) {
      scheme = "jar:" + scheme;
    }

    String authority = StringUtil.nullize(matcher.group(3));

    String path = StringUtil.nullize(matcher.group(4));
    if (path != null) {
      path = FileUtil.toCanonicalUriPath(path);
    }

    if (authority != null && (URLUtil.FILE_PROTOCOL.equals(scheme) || StringUtil.isEmpty(matcher.group(2)))) {
      path = path == null ? authority : (authority + path);
      authority = null;
    }
    return new UrlImpl(scheme, authority, path, matcher.group(5));
  }

  public static boolean equalsIgnoreParameters(@Nonnull Url url, @Nonnull Collection<Url> urls) {
    return equalsIgnoreParameters(url, urls, true);
  }

  public static boolean equalsIgnoreParameters(@Nonnull Url url, @Nonnull Collection<Url> urls, boolean caseSensitive) {
    for (Url otherUrl : urls) {
      if (equals(url, otherUrl, caseSensitive, true)) {
        return true;
      }
    }
    return false;
  }

  public static boolean equals(@Nullable Url url1, @Nullable Url url2, boolean caseSensitive, boolean ignoreParameters) {
    if (url1 == null || url2 == null){
      return url1 == url2;
    }

    Url o1 = ignoreParameters ? url1.trimParameters() : url1;
    Url o2 = ignoreParameters ? url2.trimParameters() : url2;
    return caseSensitive ? o1.equals(o2) : o1.equalsIgnoreCase(o2);
  }

  @Nonnull
  public static URI toUriWithoutParameters(@Nonnull Url url) {
    try {
      String externalPath = url.getPath();
      boolean inLocalFileSystem = url.isInLocalFileSystem();
      if (inLocalFileSystem && SystemInfoRt.isWindows && externalPath.charAt(0) != '/') {
        externalPath = '/' + externalPath;
      }
      return new URI(inLocalFileSystem ? "file" : url.getScheme(), inLocalFileSystem ? "" : url.getAuthority(), externalPath, null, null);
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static HashingStrategy<Url> getCaseInsensitiveUrlHashingStrategy() {
    return CaseInsensitiveUrlHashingStrategy.INSTANCE;
  }

  private static final class CaseInsensitiveUrlHashingStrategy implements HashingStrategy<Url> {
    private static final HashingStrategy<Url> INSTANCE = new CaseInsensitiveUrlHashingStrategy();

    @Override
    public int hashCode(Url url) {
      return url == null ? 0 : url.hashCodeCaseInsensitive();
    }

    @Override
    public boolean equals(Url url1, Url url2) {
      return Urls.equals(url1, url2, false, false);
    }
  }
}