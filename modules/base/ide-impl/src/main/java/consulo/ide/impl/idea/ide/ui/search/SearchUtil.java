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
package consulo.ide.impl.idea.ide.ui.search;

import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.OptionDescription;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author anna
 * @since 2006-02-07
 */
public class SearchUtil {
    private static final Pattern QUOTED = Pattern.compile("\\\"([^\\\"]+)\\\"");

    public static final String STYLE_END = "</style>";

    private SearchUtil() {
    }

    public static String markup(String textToMarkup, @Nullable String filter) {
        if (filter == null || filter.length() == 0) {
            return textToMarkup;
        }
        int bodyStart = textToMarkup.indexOf("<body>");
        int bodyEnd = textToMarkup.indexOf("</body>");
        String head;
        String foot;
        if (bodyStart >= 0) {
            bodyStart += "<body>".length();
            head = textToMarkup.substring(0, bodyStart);
            if (bodyEnd >= 0) {
                foot = textToMarkup.substring(bodyEnd);
            }
            else {
                foot = "";
            }
            textToMarkup = textToMarkup.substring(bodyStart, bodyEnd);
        }
        else {
            foot = "";
            head = "";
        }
        Pattern insideHtmlTagPattern = Pattern.compile("[<[^<>]*>]*<[^<>]*");
        SearchableOptionsRegistrar registrar = SearchableOptionsRegistrar.getInstance();
        Set<String> quoted = new HashSet<>();
        filter = processFilter(quoteStrictOccurrences(textToMarkup, filter), quoted);
        Set<String> options = registrar.getProcessedWords(filter);
        Set<String> words = registrar.getProcessedWords(textToMarkup);
        for (String option : options) {
            if (words.contains(option)) {
                textToMarkup = markup(textToMarkup, insideHtmlTagPattern, option);
            }
        }
        for (String stripped : quoted) {
            textToMarkup = markup(textToMarkup, insideHtmlTagPattern, stripped);
        }
        return head + textToMarkup + foot;
    }

    private static String quoteStrictOccurrences(String textToMarkup, String filter) {
        String cur = "";
        String s = textToMarkup.toLowerCase();
        for (String part : filter.split(" ")) {
            if (s.contains(part)) {
                cur += "\"" + part + "\" ";
            }
            else {
                cur += part + " ";
            }
        }
        return cur;
    }

    private static String markup(String textToMarkup, Pattern insideHtmlTagPattern, String option) {
        String result;
        int styleIdx = textToMarkup.indexOf("<style");
        int styleEndIdx = textToMarkup.indexOf("</style>");
        if (styleIdx < 0 || styleEndIdx < 0) {
            result = markupInText(textToMarkup, insideHtmlTagPattern, option);
        }
        else {
            result = markup(textToMarkup.substring(0, styleIdx), insideHtmlTagPattern, option) +
                markup(textToMarkup.substring(styleEndIdx + STYLE_END.length()), insideHtmlTagPattern, option);
        }
        return result;
    }

    private static String markupInText(String textToMarkup, Pattern insideHtmlTagPattern, String option) {
        String result = "";
        int beg = 0;
        int idx;
        while ((idx = StringUtil.indexOfIgnoreCase(textToMarkup, option, beg)) != -1) {
            String prefix = textToMarkup.substring(beg, idx);
            String toMark = textToMarkup.substring(idx, idx + option.length());
            if (insideHtmlTagPattern.matcher(prefix).matches()) {
                int lastIdx = textToMarkup.indexOf(">", idx);
                result += prefix + textToMarkup.substring(idx, lastIdx + 1);
                beg = lastIdx + 1;
            }
            else {
                result += prefix + "<font color='#ffffff' bgColor='#1d5da7'>" + toMark + "</font>";
                beg = idx + option.length();
            }
        }
        result += textToMarkup.substring(beg);
        return result;
    }

    public static void appendFragments(
        String filter,
        String text,
        @SimpleTextAttributes.StyleAttributeConstant int style,
        Color foreground,
        Color background,
        SimpleColoredComponent textRenderer
    ) {
        if (text == null) {
            return;
        }

        ColorValue bgColor = TargetAWT.from(background);
        ColorValue fgColor = TargetAWT.from(foreground);
        if (StringUtil.isEmpty(filter)) {
            textRenderer.append(text, SimpleTextAttributes.of(bgColor, fgColor, TargetAWT.from(JBColor.RED), style));
        }
        else { //markup
            Set<String> quoted = new HashSet<>();
            filter = processFilter(quoteStrictOccurrences(text, filter), quoted);
            Map<Integer, String> fragmentsByIndex = new TreeMap<>();
            for (String stripped : quoted) {
                int beg = 0;
                int index;
                while ((index = StringUtil.indexOfIgnoreCase(text, stripped, beg)) != -1) {
                    fragmentsByIndex.put(index, text.substring(index, index + stripped.length()));
                    beg = index + stripped.length();
                }
            }

            List<String> selectedWords = new ArrayList<>();
            int pos = 0;
            for (Integer index : fragmentsByIndex.keySet()) {
                String stripped = fragmentsByIndex.get(index);
                int start = index;
                if (pos > start) {
                    String highlighted = selectedWords.get(selectedWords.size() - 1);
                    if (highlighted.length() < stripped.length()) {
                        selectedWords.remove(highlighted);
                    }
                    else {
                        continue;
                    }
                }
                appendSelectedWords(text, selectedWords, pos, start, filter);
                selectedWords.add(stripped);
                pos = start + stripped.length();
            }
            appendSelectedWords(text, selectedWords, pos, text.length(), filter);

            int idx = 0;
            for (String word : selectedWords) {
                text = text.substring(idx);
                String before = text.substring(0, text.indexOf(word));
                if (!before.isEmpty()) {
                    textRenderer.append(before, SimpleTextAttributes.of(bgColor, fgColor, null, style));
                }
                idx = text.indexOf(word) + word.length();
                textRenderer.append(
                    text.substring(idx - word.length(), idx),
                    SimpleTextAttributes.of(bgColor, fgColor, null, style | SimpleTextAttributes.STYLE_SEARCH_MATCH)
                );
            }
            String after = text.substring(idx);
            if (!after.isEmpty()) {
                textRenderer.append(after, SimpleTextAttributes.of(bgColor, fgColor, null, style));
            }
        }
    }

    private static void appendSelectedWords(
        String text,
        List<String> selectedWords,
        int pos,
        int end,
        String filter
    ) {
        if (pos < end) {
            Set<String> filters = SearchableOptionsRegistrar.getInstance().getProcessedWords(filter);
            String[] words = text.substring(pos, end).split("[\\W&&[^-]]+");
            for (String word : words) {
                if (filters.contains(PorterStemmerUtil.stem(word.toLowerCase()))) {
                    selectedWords.add(word);
                }
            }
        }
    }

    public static List<Set<String>> findKeys(String filter, Set<String> quoted) {
        filter = processFilter(filter.toLowerCase(), quoted);
        List<Set<String>> keySetList = new ArrayList<>();
        SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();
        Set<String> words = optionsRegistrar.getProcessedWords(filter);
        for (String word : words) {
            Set<OptionDescription> descriptions = ((SearchableOptionsRegistrarImpl) optionsRegistrar).getAcceptableDescriptions(word);
            Set<String> keySet = new HashSet<>();
            if (descriptions != null) {
                for (OptionDescription description : descriptions) {
                    keySet.add(description.getPath());
                }
            }
            keySetList.add(keySet);
        }
        return keySetList;
    }

    public static String processFilter(String filter, Set<String> quoted) {
        String withoutQuoted = "";
        int beg = 0;
        Matcher matcher = QUOTED.matcher(filter);
        while (matcher.find()) {
            int start = matcher.start(1);
            withoutQuoted += " " + filter.substring(beg, start);
            beg = matcher.end(1);
            String trimmed = filter.substring(start, beg).trim();
            if (trimmed.length() > 0) {
                quoted.add(trimmed);
            }
        }
        return withoutQuoted + " " + filter.substring(beg);
    }

    public static List<Configurable> expand(Configurable[] configurables) {
        return expandGroup(configurables);
    }

    public static List<Configurable> expandGroup(Configurable[] configurables) {
        List<Configurable> result = new ArrayList<>();
        ContainerUtil.addAll(result, configurables);
        for (Configurable each : configurables) {
            addChildren(each, result);
        }

        result = ContainerUtil.filter(
            result,
            configurable -> !(configurable instanceof SearchableConfigurable.Parent searchableParent) || searchableParent.isVisible()
        );

        return result;
    }

    private static void addChildren(Configurable configurable, List<Configurable> list) {
        if (configurable instanceof Configurable.Composite composite) {
            for (Configurable eachKid : composite.getConfigurables()) {
                list.add(eachKid);
                addChildren(eachKid, list);
            }
        }
    }
}
