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

import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Attribute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 * Date: Dec 6, 2004
 * @see PathMacrosImpl#addMacroReplacements(ReplacePathToMacroMap)
 * @see PathMacroManager
 */
public class ReplacePathToMacroMap extends PathMacroMap {
    private List<String> myPathsIndex = null;
    private final Map<String, String> myMacroMap = new LinkedHashMap<>();

    public void addMacroReplacement(@Nonnull PathMacroProtocolProvider pathMacroProtocolProvider, String path, String macroName) {
        addReplacement(pathMacroProtocolProvider, quotePath(path), "$" + macroName + "$", true);
    }

    public void addReplacement(@Nonnull PathMacroProtocolProvider pathMacroProtocolProvider, String path, String macroExpr, boolean overwrite) {
        path = StringUtil.trimEnd(path, "/");
        putIfAbsent(path, macroExpr, overwrite);

        for (String protocol : pathMacroProtocolProvider.getSupportedProtocols()) {
            putIfAbsent(protocol + ":" + path, protocol + ":" + macroExpr, overwrite);
            putIfAbsent(protocol + ":/" + path, protocol + ":/" + macroExpr, overwrite);
            putIfAbsent(protocol + "://" + path, protocol + "://" + macroExpr, overwrite);
        }
    }

    private void putIfAbsent(final String path, final String substitution, final boolean overwrite) {
        if (overwrite || !myMacroMap.containsKey(path)) {
            myMacroMap.put(path, substitution);
        }
    }

    @Override
    public String substitute(String text, boolean caseSensitive) {
        if (text == null) {
            //noinspection ConstantConditions
            return null;
        }

        for (final String path : getPathIndex()) {
            text = replacePathMacro(text, path, caseSensitive);
        }
        return text;
    }

    private String replacePathMacro(String text, final String path, boolean caseSensitive) {
        if (text.length() < path.length() || path.isEmpty()) {
            return text;
        }

        boolean startsWith = caseSensitive ? text.startsWith(path) : StringUtil.startsWithIgnoreCase(text, path);

        if (!startsWith) {
            return text;
        }

        //check that this is complete path (ends with "/" or "!/")
        // do not collapse partial paths, i.e. do not substitute "/a/b/cd" in paths like "/a/b/cdeFgh"
        int endOfOccurrence = path.length();
        final boolean isWindowsRoot = path.endsWith(":/");
        if (!isWindowsRoot &&
            endOfOccurrence < text.length() &&
            text.charAt(endOfOccurrence) != '/' &&
            !text.substring(endOfOccurrence).startsWith("!/")) {
            return text;
        }

        return myMacroMap.get(path) + text.substring(endOfOccurrence);
    }

    @Override
    public String substituteRecursively(String text, final boolean caseSensitive) {
        for (final String path : getPathIndex()) {
            text = replacePathMacroRecursively(text, path, caseSensitive);
        }
        return text;
    }

    private String replacePathMacroRecursively(String text, final String path, boolean caseSensitive) {
        if (text.length() < path.length()) {
            return text;
        }

        if (path.isEmpty()) {
            return text;
        }

        final StringBuilder newText = new StringBuilder();
        final boolean isWindowsRoot = path.endsWith(":/");
        int i = 0;
        while (i < text.length()) {
            int occurrenceOfPath = caseSensitive ? text.indexOf(path, i) : StringUtil.indexOfIgnoreCase(text, path, i);
            if (occurrenceOfPath >= 0) {
                int endOfOccurrence = occurrenceOfPath + path.length();
                if (!isWindowsRoot &&
                    endOfOccurrence < text.length() &&
                    text.charAt(endOfOccurrence) != '/' &&
                    text.charAt(endOfOccurrence) != '\"' &&
                    text.charAt(endOfOccurrence) != ' ' &&
                    !text.substring(endOfOccurrence).startsWith("!/")) {
                    newText.append(text.substring(i, endOfOccurrence));
                    i = endOfOccurrence;
                    continue;
                }
                if (occurrenceOfPath > 0) {
                    char prev = text.charAt(occurrenceOfPath - 1);
                    if (Character.isLetterOrDigit(prev) || prev == '_') {
                        newText.append(text.substring(i, endOfOccurrence));
                        i = endOfOccurrence;
                        continue;
                    }
                }
            }
            if (occurrenceOfPath < 0) {
                if (newText.length() == 0) {
                    return text;
                }
                newText.append(text.substring(i));
                break;
            }
            else {
                newText.append(text.substring(i, occurrenceOfPath));
                newText.append(myMacroMap.get(path));
                i = occurrenceOfPath + path.length();
            }
        }
        return newText.toString();
    }

    private static int getIndex(final Map.Entry<String, String> s) {
        final String replacement = s.getValue();
        if (replacement.contains("..")) {
            return 1;
        }
        if (replacement.contains("$" + PathMacroUtil.USER_HOME_MACRO_NAME + "$")) {
            return 1;
        }
        if (replacement.contains("$" + PathMacroUtil.MODULE_DIR_MACRO_NAME + "$")) {
            return 3;
        }
        if (replacement.contains("$" + PathMacroUtil.PROJECT_DIR_MACRO_NAME + "$")) {
            return 3;
        }
        return 2;
    }

    private static int stripPrefix(String key) {
        key = StringUtil.trimStart(key, "jar:");
        key = StringUtil.trimStart(key, "file:");
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key.length();
    }

    public List<String> getPathIndex() {
        if (myPathsIndex == null || myPathsIndex.size() != myMacroMap.size()) {
            List<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>(myMacroMap.entrySet());

            final ObjectIntMap<Map.Entry<String, String>> weights = ObjectMaps.newObjectIntHashMap();
            for (Map.Entry<String, String> entry : entries) {
                weights.putInt(entry, getIndex(entry) * 512 + stripPrefix(entry.getKey()));
            }

            ContainerUtil.sort(entries, (o1, o2) -> weights.getInt(o2) - weights.getInt(o1));
            myPathsIndex = ContainerUtil.map2List(entries, Map.Entry::getKey);
        }
        return myPathsIndex;
    }

    @Nonnull
    public String getAttributeValue(@Nonnull Attribute attribute, @Nullable PathMacroFilter filter, boolean caseSensitive, boolean recursively) {
        String oldValue = attribute.getValue();
        if (recursively || (filter != null && filter.recursePathMacros(attribute))) {
            return substituteRecursively(oldValue, caseSensitive).toString();
        }
        else {
            return substitute(oldValue, caseSensitive);
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ReplacePathToMacroMap)) {
            return false;
        }

        return myMacroMap.equals(((ReplacePathToMacroMap) obj).myMacroMap);
    }

    public int hashCode() {
        return myMacroMap.hashCode();
    }

    public void put(String path, String replacement) {
        myMacroMap.put(path, replacement);
    }

}
