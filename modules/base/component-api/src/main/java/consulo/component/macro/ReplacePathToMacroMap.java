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
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;
import org.jdom.Attribute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @see PathMacrosImpl#addMacroReplacements(ReplacePathToMacroMap)
 * @see PathMacroManager
 */
public class ReplacePathToMacroMap extends PathMacroMap {
    private @Nullable List<String> myPathsIndex = null;
    private final Map<String, String> myMacroMap = new LinkedHashMap<>();
    private @Nullable String myStoredUrlPrefix;
    private @Nullable String myActualUrlPrefix;

    /**
     * Before collapsing, a url starting with {@code actualUrlPrefix} (e.g. {@code platform://<id>}) is rewritten to start
     * with {@code storedUrlPrefix} (e.g. {@code file://}) - the reverse of {@link ExpandMacroToPathMap#mapUrlPrefix}.
     */
    public void mapUrlPrefix(String storedUrlPrefix, String actualUrlPrefix) {
        myStoredUrlPrefix = storedUrlPrefix;
        myActualUrlPrefix = actualUrlPrefix;
    }

    private String unmapUrl(String text) {
        String stored = myStoredUrlPrefix;
        String actual = myActualUrlPrefix;
        if (stored == null || actual == null || !text.startsWith(actual + "/")) {
            return text;
        }
        String path = text.substring(actual.length());
        // a drive of a windows platform: platform://<id>/C:/a is stored as file://C:/a - the url of a local windows file
        return stored + (isDrivePath(path) ? path.substring(1) : path);
    }

    private String unmapUrlsRecursively(String text) {
        String stored = myStoredUrlPrefix;
        String actual = myActualUrlPrefix;
        if (stored == null || actual == null) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        String prefix = actual + "/";
        int from = 0;
        while (true) {
            int index = text.indexOf(prefix, from);
            if (index < 0) {
                result.append(text, from, text.length());
                return result.toString();
            }
            result.append(text, from, index).append(stored);
            int pathStart = index + actual.length();
            from = isDrivePath(text.substring(pathStart, Math.min(text.length(), pathStart + 3))) ? pathStart + 1 : pathStart;
        }
    }

    /**
     * {@code /C:} - the start of a path of a windows drive
     */
    private static boolean isDrivePath(String path) {
        return path.length() >= 3 && path.charAt(0) == '/' && Character.isLetter(path.charAt(1)) && path.charAt(2) == ':';
    }

    public void addMacroReplacement(PathMacroProtocolProvider pathMacroProtocolProvider, String path, String macroName) {
        addReplacement(pathMacroProtocolProvider, quotePath(path), "$" + macroName + "$", true);
    }

    public void addReplacement(PathMacroProtocolProvider pathMacroProtocolProvider, String path, String macroExpr, boolean overwrite) {
        path = StringUtil.trimEnd(path, "/");
        putIfAbsent(path, macroExpr, overwrite);

        for (String protocol : pathMacroProtocolProvider.getSupportedProtocols()) {
            putIfAbsent(protocol + ":" + path, protocol + ":" + macroExpr, overwrite);
            putIfAbsent(protocol + ":/" + path, protocol + ":/" + macroExpr, overwrite);
            putIfAbsent(protocol + "://" + path, protocol + "://" + macroExpr, overwrite);
        }
    }

    private void putIfAbsent(String path, String substitution, boolean overwrite) {
        if (overwrite || !myMacroMap.containsKey(path)) {
            myMacroMap.put(path, substitution);
        }
    }

    @Contract("null,_ -> null; !null,_ -> !null")
    @Override
    public @Nullable String substitute(@Nullable String text, boolean caseSensitive) {
        if (text == null) {
            //noinspection ConstantConditions
            return null;
        }

        text = unmapUrl(text);
        for (String path : getPathIndex()) {
            text = replacePathMacro(text, path, caseSensitive);
        }
        return text;
    }

    private String replacePathMacro(String text, String path, boolean caseSensitive) {
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
        boolean isWindowsRoot = path.endsWith(":/");
        if (!isWindowsRoot &&
            endOfOccurrence < text.length() &&
            text.charAt(endOfOccurrence) != '/' &&
            !text.substring(endOfOccurrence).startsWith("!/")) {
            return text;
        }

        return myMacroMap.get(path) + text.substring(endOfOccurrence);
    }

    @Override
    public String substituteRecursively(String text, boolean caseSensitive) {
        text = unmapUrlsRecursively(text);
        for (String path : getPathIndex()) {
            text = replacePathMacroRecursively(text, path, caseSensitive);
        }
        return text;
    }

    private String replacePathMacroRecursively(String text, String path, boolean caseSensitive) {
        if (text.length() < path.length()) {
            return text;
        }

        if (path.isEmpty()) {
            return text;
        }

        StringBuilder newText = new StringBuilder();
        boolean isWindowsRoot = path.endsWith(":/");
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

    private static int getIndex(Map.Entry<String, String> s) {
        String replacement = s.getValue();
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
            List<Map.Entry<String, String>> entries = new ArrayList<>(myMacroMap.entrySet());

            ObjectIntMap<Map.Entry<String, String>> weights = ObjectMaps.newObjectIntHashMap();
            for (Map.Entry<String, String> entry : entries) {
                weights.putInt(entry, getIndex(entry) * 512 + stripPrefix(entry.getKey()));
            }

            ContainerUtil.sort(entries, (o1, o2) -> weights.getInt(o2) - weights.getInt(o1));
            myPathsIndex = ContainerUtil.map2List(entries, Map.Entry::getKey);
        }
        return myPathsIndex;
    }

    public String getAttributeValue(Attribute attribute, @Nullable PathMacroFilter filter, boolean caseSensitive, boolean recursively) {
        String oldValue = attribute.getValue();
        if (recursively || (filter != null && filter.recursePathMacros(attribute))) {
            return substituteRecursively(oldValue, caseSensitive);
        }
        else {
            return substitute(oldValue, caseSensitive);
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this
            || obj instanceof ReplacePathToMacroMap that && myMacroMap.equals(that.myMacroMap)
            && Objects.equals(myActualUrlPrefix, that.myActualUrlPrefix);
    }

    @Override
    public int hashCode() {
        return myMacroMap.hashCode() + Objects.hashCode(myActualUrlPrefix);
    }

    public void put(String path, String replacement) {
        myMacroMap.put(path, replacement);
    }
}
