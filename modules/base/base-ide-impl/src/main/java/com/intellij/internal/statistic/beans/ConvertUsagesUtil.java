/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.internal.statistic.beans;

import java.util.*;

/**
 * ATTENTION! DO NOT IMPORT @NotNull AND @Nullable ANNOTATIONS
 * This class is also used on jetbrains web site
 */

public class ConvertUsagesUtil {
  private static final char GROUP_SEPARATOR = ':';
  private static final char GROUPS_SEPARATOR = ';';
  private static final char GROUP_VALUE_SEPARATOR = ',';

  private ConvertUsagesUtil() {
  }

  //@NotNull
  public static String convertValueMap(Set<? extends UsageDescriptor> descriptors) {
    assert descriptors != null;
    final StringBuilder buffer = new StringBuilder();
    for (UsageDescriptor usageDescriptor : descriptors) {
      buffer.append(usageDescriptor.getKey());
      buffer.append("=");
      buffer.append(usageDescriptor.getValue());
      buffer.append(GROUP_VALUE_SEPARATOR);
    }
    buffer.deleteCharAt(buffer.length() - 1);

    return buffer.toString();
  }

  //@NotNull
  public static Map<String, Set<UsageDescriptor>> convertValueString(String groupId, String valueData) {
    assert groupId != null;
    final Map<String, Set<UsageDescriptor>> descriptors = new HashMap<>();
    for (String value : valueData.split(Character.toString(GROUP_VALUE_SEPARATOR))) {
      if (!isEmptyOrSpaces(value)) {
        final StringPair pair = getPair(value, "=");
        if (pair != null) {
          final String count = pair.second;
          if (!isEmptyOrSpaces(count)) {
            try {
              final int i = Integer.parseInt(count);
              if (!descriptors.containsKey(groupId)) {
                descriptors.put(groupId, new LinkedHashSet<>());
              }
              descriptors.get(groupId).add(new UsageDescriptor(pair.first, i));
            }
            catch (NumberFormatException ignored) {
            }
          }
        }
      }
    }

    return descriptors;
  }

  //@Nullable
  public static StringPair getPair(String str, String separator) {
    assert str != null;
    assert separator != null;
    final int i = str.indexOf(separator);
    if (i > 0 && i < str.length() - 1) {
      String key = str.substring(0, i).trim();
      String value = str.substring(i + 1).trim();
      if (!isEmptyOrSpaces(key) && !isEmptyOrSpaces(value)) {
        return new StringPair(key, value);
      }
    }
    return null;
  }

  private static class StringPair {
    public final String first;
    public final String second;

    public StringPair(String first, String second) {
      this.first = first;
      this.second = second;
    }
  }

  public static boolean isEmptyOrSpaces(final String s) {
    return s == null || s.trim().length() == 0;
  }

  public static void assertDescriptorName(String key) {
    assert key != null;
    assert key.indexOf(GROUP_SEPARATOR) == -1 : key + " contains invalid chars";
    assert key.indexOf(GROUPS_SEPARATOR) == -1 : key + " contains invalid chars";
    assert key.indexOf(GROUP_VALUE_SEPARATOR) == -1 : key + " contains invalid chars";
    assert !key.contains("=") : key + " contains invalid chars";
    assert !key.contains("'") : key + " contains invalid chars";
    assert !key.contains("\"") : key + " contains invalid chars";
  }

  //@NotNull
  public static String ensureProperKey(/*@NotNull*/ String input) {
    final StringBuilder escaped = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      final char ch = input.charAt(i);
      switch (ch) {
        case GROUP_SEPARATOR:
        case GROUPS_SEPARATOR:
        case GROUP_VALUE_SEPARATOR:
        case '\'':
        case '\"':
        case '=':
          escaped.append(' ');
          break;
        default:
          escaped.append(ch);
          break;
      }
    }
    return escaped.toString();
  }
}
