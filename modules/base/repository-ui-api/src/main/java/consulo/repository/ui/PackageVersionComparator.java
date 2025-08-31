/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.repository.ui;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageVersionComparator implements Comparator<String> {
  public static final Comparator<String> VERSION_COMPARATOR = new PackageVersionComparator();

  @Override
  public int compare(String version1, String version2) {
    List<String> vs1 = parse(version1);
    List<String> vs2 = parse(version2);
    for (int i = 0; i < vs1.size() && i < vs2.size(); i++) {
      String vs1Part = vs1.get(i);
      String vs2Part = vs2.get(i);
      if (vs1Part.equals("**") || vs2Part.equals("**")) {
        return 0;
      }
      int result = vs1Part.compareTo(vs2Part);
      if (result != 0) {
        return result;
      }
    }
    return vs1.size() - vs2.size();
  }

  @Nullable
  private static String replace(@Nonnull String s) {
    Map<String, String> sub = Map.of("pre", "c", "preview", "c", "rc", "c", "dev", "@");
    String tmp = sub.get(s);
    if (tmp != null) {
      s = tmp;
    }
    if (s.equals(".") || s.equals("-")) {
      return null;
    }
    if (s.matches("[0-9]+")) {
      try {
        long value = Long.parseLong(s);
        return String.format("%08d", value);
      }
      catch (NumberFormatException e) {
        return s;
      }
    }
    return "*" + s;
  }

  @Nonnull
  private static List<String> parse(@Nullable String s) {
    // Version parsing from pkg_resources ensures that all the "pre", "alpha", "rc", etc. are sorted correctly
    if (s == null) {
      return Collections.emptyList();
    }
    Pattern COMPONENT_RE = Pattern.compile("\\d+|[a-z]+|\\.|-|.+");
    List<String> results = new ArrayList<>();
    Matcher matcher = COMPONENT_RE.matcher(s);
    while (matcher.find()) {
      String component = replace(matcher.group());
      if (component == null) {
        continue;
      }
      results.add(component);
    }
    for (int i = results.size() - 1; i > 0; i--) {
      if ("00000000".equals(results.get(i))) {
        results.remove(i);
      }
      else {
        break;
      }
    }
    results.add("*final");
    return results;
  }
}
