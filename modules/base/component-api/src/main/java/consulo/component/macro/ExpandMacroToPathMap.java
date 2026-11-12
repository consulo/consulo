/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.component.macro;

import com.uber.nullaway.annotations.Contract;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Eugene Zhuravlev
 * @since 2004-12-06
 */
public class ExpandMacroToPathMap extends PathMacroMap {
  private final Map<String, String> myPlainMap = new LinkedHashMap<>();
  private final Map<String, String> myMacroExpands = new LinkedHashMap<>();
  private @Nullable String myStoredUrlPrefix;
  private @Nullable String myActualUrlPrefix;

  /**
   * After expanding, a url starting with {@code storedUrlPrefix} (e.g. {@code file://}) is rewritten to start with
   * {@code actualUrlPrefix} (e.g. {@code platform://<id>}) - a project on a remote platform stores its urls as local
   * ones, so its files can be shared with a local checkout of the same project.
   */
  public void mapUrlPrefix(String storedUrlPrefix, String actualUrlPrefix) {
    myStoredUrlPrefix = storedUrlPrefix;
    myActualUrlPrefix = actualUrlPrefix;
  }

  public void addMacroExpand(String macroName, String path) {
    myMacroExpands.put(macroName, PathMacroMap.quotePath(path));
  }

  public void put(String fromText, String toText) {
    myPlainMap.put(fromText, toText);
  }

  public void putAll(ExpandMacroToPathMap another) {
    myPlainMap.putAll(another.myPlainMap);
    myMacroExpands.putAll(another.myMacroExpands);
    if (another.myStoredUrlPrefix != null && another.myActualUrlPrefix != null) {
      mapUrlPrefix(another.myStoredUrlPrefix, another.myActualUrlPrefix);
    }
  }

  @Contract("null,_ -> null; !null,_ -> !null")
  @Override
  public @Nullable String substitute(@Nullable String text, boolean caseSensitive) {
    if (text == null) {
      //noinspection ConstantConditions
      return null;
    }
    if (text.indexOf('$') < 0) {
      return mapUrl(text);
    }
    for (Map.Entry<String, String> entry : myPlainMap.entrySet()) {
      // when replacing macros with actual paths the replace utility may be used as always 'case-sensitive'
      // for case-insensitive file systems there will be no unnecessary toLowerCase() transforms.
      text = StringUtil.replace(text, entry.getKey(), entry.getValue(), false);
    }

    for (Map.Entry<String, String> entry : myMacroExpands.entrySet()) {
      text = replaceMacro(text, entry.getKey(), entry.getValue());
    }

    return mapUrl(text);
  }

  private String mapUrl(String text) {
    String stored = myStoredUrlPrefix;
    String actual = myActualUrlPrefix;
    if (stored == null || actual == null || !text.startsWith(stored)) {
      return text;
    }
    String path = text.substring(stored.length());
    if (path.startsWith("$")) {
      // a macro this map does not know (a module one, expanded by the project) - the map which expands it maps the url
      return text;
    }
    while (path.startsWith("//")) {
      // file:////a - written by a version which expanded module macros late
      path = path.substring(1);
    }
    // file://C:/a - a drive of a windows platform, platform://<id>/C:/a
    return actual + (path.startsWith("/") ? path : "/" + path);
  }

  private static String replaceMacro(String text, String macroName, String replacement) {
    while (true) {
      int start = findMacroIndex(text, macroName);
      if (start < 0) {
        break;
      }

      int end = start + macroName.length() + 2;
      int slashCount = getSlashCount(text, end);
      String actualReplacement = slashCount > 0 && !replacement.endsWith("/") ? replacement + "/" : replacement;
      text = StringUtil.replaceSubstring(text, start, end + slashCount, actualReplacement);
    }
    return text;
  }

  private static int getSlashCount(String text, int pos) {
    return StringUtil.isChar(text, pos, '/') ? StringUtil.isChar(text, pos + 1, '/') ? 2 : 1 : 0;
  }

  private static int findMacroIndex(String text, String macroName) {
    int i = -1;
    while (true) {
      i = text.indexOf('$', i + 1);
      if (i < 0) {
        return -1;
      }
      if (StringUtil.startsWith(text, i + 1, macroName) && StringUtil.isChar(text, i + macroName.length() + 1, '$')) {
        return i;
      }
    }
  }

  @Override
  public int hashCode() {
    return myPlainMap.hashCode() + myMacroExpands.hashCode() + Objects.hashCode(myActualUrlPrefix);
  }
}
