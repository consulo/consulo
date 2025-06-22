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
package consulo.ide.impl.idea.application.options;

import consulo.application.macro.PathMacros;
import consulo.component.macro.PathMacroFilter;
import consulo.component.macro.PathMacroMap;
import consulo.component.store.internal.PathMacrosService;
import consulo.pathMacro.Macro;
import consulo.pathMacro.MacroManager;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * @author Eugene Zhuravlev
 * @since 2004-12-06
 */
public class PathMacrosCollectorImpl extends PathMacroMap {
  private static final String FILE_PROTOCOL = "file:";
  private static final String JAR_PROTOCOL = "jar:";

  @Nonnull
  public static Set<String> getMacroNames(Element root, @Nullable PathMacroFilter filter, @Nonnull final PathMacros pathMacros) {
    final PathMacrosCollectorImpl collector = new PathMacrosCollectorImpl();
    collector.substitute(root, true, false, filter);
    final HashSet<String> result = new HashSet<>(collector.myMacroMap.keySet());
    result.removeAll(pathMacros.getSystemMacroNames());
    result.removeAll(pathMacros.getLegacyMacroNames());
    for (Macro macro : MacroManager.getInstance().getMacros()) {
      result.remove(macro.getName());
    }
    result.removeAll(pathMacros.getIgnoredMacroNames());
    return result;
  }

  private final Map<String, String> myMacroMap = new LinkedHashMap<>();
  private final Matcher myMatcher;

  private PathMacrosCollectorImpl() {
    myMatcher = PathMacrosService.MACRO_PATTERN.matcher("");
  }

  @Override
  public String substituteRecursively(String text, boolean caseSensitive) {
    if (text == null || text.isEmpty()) return text;

    myMatcher.reset(text);
    while (myMatcher.find()) {
      final String macroName = myMatcher.group(1);
      myMacroMap.put(macroName, null);
    }

    return text;
  }

  @Override
  public String substitute(String text, boolean caseSensitive) {
    if (text == null || text.isEmpty()) return text;

    String protocol = null;
    if (text.length() > 7 && text.charAt(0) == 'f') {
      protocol = FILE_PROTOCOL;
    } else if (text.length() > 6 && text.charAt(0) == 'j') {
      protocol = JAR_PROTOCOL;
    } else if ('$' != text.charAt(0)) {
      return text;
    }

    if (protocol != null && !text.startsWith(protocol)) return text;

    myMatcher.reset(text);
    if (myMatcher.find()) {
      final String macroName = myMatcher.group(1);
      myMacroMap.put(macroName, null);
    }

    return text;
  }

  @Override
  public int hashCode() {
    return myMacroMap.hashCode();
  }
}
