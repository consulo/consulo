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
package com.intellij.openapi.application;

import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * UUID identifying pair user@computer
 *
 * FIXME [VISTALL] review this code for Web Plaform
 */
public class PermanentInstallationID {
  private static final String INSTALLATION_ID_KEY = "user_id_on_machine";
  private static final String INSTALLATION_DATE_KEY = "user_date_on_machine";

  private static final String INSTALLATION_ID = calculateValue(INSTALLATION_ID_KEY, () -> UUID.randomUUID().toString());
  private static final long INSTALLATION_DATE = Long.parseLong(calculateValue(INSTALLATION_DATE_KEY, () -> String.valueOf(System.currentTimeMillis())));

  @Nonnull
  public static String get() {
    return INSTALLATION_ID;
  }

  public static long date() {
    return INSTALLATION_DATE;
  }

  private static String calculateValue(String key, Supplier<String> factory) {
    final Preferences prefs = Preferences.userRoot().node("consulo");

    String value = prefs.get(key, null);
    if (StringUtil.isEmptyOrSpaces(value)) {
      value = factory.get();
      prefs.put(key, value);
    }

    return value;
  }
}