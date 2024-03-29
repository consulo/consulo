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
package consulo.versionControlSystem;

import consulo.application.util.registry.Registry;

import jakarta.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.MissingResourceException;

public class VcsLocaleHelper {

  private static final String DEFAULT_EXECUTABLE_LOCALE_VALUE = "en_US.UTF-8";
  private static final String REGISTRY_KEY_SUFFIX = ".executable.locale";

  @Nonnull
  public static String getDefaultLocaleFromRegistry(@Nonnull String prefix) {
    String registryKey = prefix + REGISTRY_KEY_SUFFIX;
    try {
      return Registry.stringValue(registryKey);
    }
    catch (MissingResourceException e) {
      return DEFAULT_EXECUTABLE_LOCALE_VALUE;
    }
  }

  @Nonnull
  public static Map<String, String> getDefaultLocaleEnvironmentVars(@Nonnull String prefix) {
    Map<String, String> envMap = new LinkedHashMap<>();
    String defaultLocale = getDefaultLocaleFromRegistry(prefix);
    if (defaultLocale.isEmpty()) { // let skip locale definition if needed
      return envMap;
    }
    envMap.put("LANGUAGE", "");
    envMap.put("LC_ALL", defaultLocale);
    return envMap;
  }
}
