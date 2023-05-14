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
package consulo.util.io;

import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class UriUtil {
  private UriUtil() {
  }

  @Nonnull
  public static String encode(@Nonnull String url) {
    return URLEncoder.encode(url, StandardCharsets.UTF_8);
  }

  @Nonnull
  public static String trimTrailingSlashes(@Nonnull String url) {
    return StringUtil.trimTrailing(url, '/');
  }

  @Nonnull
  public static String trimLeadingSlashes(@Nonnull String url) {
    return StringUtil.trimLeading(url, '/');
  }

  public static String trimParameters(@Nonnull String url) {
    int end = StringUtil.indexOfAny(url, "?#;");
    return end != -1 ? url.substring(0, end) : url;
  }

  /**
   * Splits the url into 2 parts: the scheme ("http", for instance) and the rest of the URL. <br/>
   * Scheme separator is not included neither to the scheme part, nor to the url part. <br/>
   * The scheme can be absent, in which case empty string is written to the first item of the Pair.
   */
  @Nonnull
  public static Couple<String> splitScheme(@Nonnull String url) {
    List<String> list = StringUtil.split(url, URLUtil.SCHEME_SEPARATOR);
    if (list.size() == 1) {
      return Couple.of("", list.get(0));
    }
    return Couple.of(list.get(0), list.get(1));
  }
}