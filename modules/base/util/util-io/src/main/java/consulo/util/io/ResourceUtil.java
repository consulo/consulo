/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author max
 */
public class ResourceUtil {
   private static final String ENCODING_UTF_8 = "UTF-8";

  private ResourceUtil() {
  }

  public static URL getResource(@Nonnull ClassLoader classLoader, @NonNls @Nonnull String basePath, @NonNls @Nonnull String fileName) {
    if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length() - 1);

    final List<String> bundles = calculateBundleNames(basePath, Locale.getDefault());
    for (String bundle : bundles) {
      URL url = classLoader.getResource(bundle + "/" + fileName);
      if (url == null) continue;

      try {
        url.openConnection();
      }
      catch (IOException e) {
        continue;
      }

      return url;
    }

    return classLoader.getResource(basePath + "/" + fileName);
  }

  public static URL getResource(@Nonnull Class loaderClass, @NonNls @Nonnull String basePath, @NonNls @Nonnull String fileName) {
    return getResource(loaderClass.getClassLoader(), basePath, fileName);
  }

  /**
   * Copied from java.util.ResourceBundle implementation
   */
  private static List<String> calculateBundleNames(String baseName, Locale locale) {
    final List<String> result = new ArrayList<String>(3);
    final String language = locale.getLanguage();
    final int languageLength = language.length();
    final String country = locale.getCountry();
    final int countryLength = country.length();
    final String variant = locale.getVariant();
    final int variantLength = variant.length();

    result.add(0, baseName);

    if (languageLength + countryLength + variantLength == 0) {
      //The locale is "", "", "".
      return result;
    }

    final StringBuilder temp = new StringBuilder(baseName);
    temp.append('_');
    temp.append(language);
    if (languageLength > 0) {
      result.add(0, temp.toString());
    }

    if (countryLength + variantLength == 0) {
      return result;
    }

    temp.append('_');
    temp.append(country);
    if (countryLength > 0) {
      result.add(0, temp.toString());
    }

    if (variantLength == 0) {
      return result;
    }
    temp.append('_');
    temp.append(variant);
    result.add(0, temp.toString());

    return result;
  }

  @Nonnull
  public static String loadText(@Nonnull URL url) throws IOException {
    InputStream inputStream = new BufferedInputStream(URLUtil.openStream(url));

    try (InputStreamReader reader = new InputStreamReader(inputStream, ENCODING_UTF_8)) {
      StringBuilder text = new StringBuilder();
      char[] buf = new char[5000];
      while (reader.ready()) {
        final int length = reader.read(buf);
        if (length == -1) break;
        text.append(buf, 0, length);
      }
      return text.toString();
    }
  }
}
