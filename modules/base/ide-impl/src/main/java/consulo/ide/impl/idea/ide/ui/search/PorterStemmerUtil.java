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
package consulo.ide.impl.idea.ide.ui.search;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"HardCodedStringLiteral"})
public class PorterStemmerUtil {
    private PorterStemmerUtil() {
    }

    public static @Nullable String stem(String str) {
        // check for zero length
        int strLen = str.length();
        if (strLen > 0) {
            int lastNonLetter = -1;
            for (int i = 0; i < strLen; ++i) {
                char c = str.charAt(i);
                if (Character.isDigit(c) || c == '-' || c == '_') {
                    lastNonLetter = i;
                }
                else if (!Character.isLetter(c)) {
                    return null;
                }
            }
            ++lastNonLetter;
            if (lastNonLetter > 0 && lastNonLetter < strLen) {
                return str.substring(0, lastNonLetter) + stemString(str.substring(lastNonLetter));
            }
            return stemString(str);
        }
        return null;
    }

    private static String stemString(String str) {
        str = step1a(str);
        str = step1b(str);
        str = step1c(str);
        str = step2(str);
        str = step3(str);
        str = step4(str);
        str = step5a(str);
        str = step5b(str);
        return str;
    }

    @SuppressWarnings("SpellCheckingInspection")
    static String step1a(String str) {
        // SSES -> SS
        if (str.endsWith("sses")) {
            return str.substring(0, str.length() - 2);
            // IES -> I
        }
        else if (str.endsWith("ies")) {
            return str.substring(0, str.length() - 2);
            // SS -> S
        }
        else if (str.endsWith("ss")) {
            return str;
            // S ->
        }
        else if (str.endsWith("s")) {
            return str.substring(0, str.length() - 1);
        }
        else {
            return str;
        }
    }

    static String step1b(String str) {
        // (m > 0) EED -> EE
        if (str.endsWith("eed")) {
            if (stringMeasure(str, 3) > 0) {
                return str.substring(0, str.length() - 1);
            }
            else {
                return str;
            }
            // (*v*) ED ->
        }
        else if (str.endsWith("ed") && containsVowel(str, 2)) {
            return step1b2(str.substring(0, str.length() - 2));
            // (*v*) ING ->
        }
        else if (str.endsWith("ing") && containsVowel(str, 3)) {
            return step1b2(str.substring(0, str.length() - 3));
        }
        return str;
    }

    private static String step1b2(String str) {
        // AT -> ATE
        if (str.endsWith("at") || str.endsWith("bl") || str.endsWith("iz")) {
            return str + "e";
        }
        else if (endsWithDoubleConsonant(str) && !(str.endsWith("l") || str.endsWith("s") || str.endsWith("z"))) {
            return str.substring(0, str.length() - 1);
        }
        else if (stringMeasure(str, 0) == 1 && endsWithCVC(str, 0)) {
            return str + "e";
        }
        else {
            return str;
        }
    }

    static String step1c(String str) {
        // (*v*) Y -> I
        if (str.endsWith("y") && containsVowel(str, 1)) {
            return str.substring(0, str.length() - 1) + "i";
        }
        return str;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static final List<Map.Entry<String, String>> STEP2_ENDINGS = List.of(
        Map.entry("ational", "ate"),
        Map.entry("tional", "tion"),
        Map.entry("enci", "ence"),
        Map.entry("anci", "ance"),
        Map.entry("izer", "ize"),
        Map.entry("abli", "able"),
        Map.entry("alli", "al"),
        Map.entry("entli", "ent"),
        Map.entry("eli", "e"),
        Map.entry("ousli", "ous"),
        Map.entry("ization", "ize"),
        Map.entry("ation", "ate"),
        Map.entry("ator", "ate"),
        Map.entry("alism", "al"),
        Map.entry("iveness", "ive"),
        Map.entry("fulness", "ful"),
        Map.entry("ousness", "ous"),
        Map.entry("aliti", "al"),
        Map.entry("iviti", "ive"),
        Map.entry("biliti", "ble")
    );

    @SuppressWarnings("SpellCheckingInspection")
    static String step2(String str) {
        return replaceEndingsIfMeasured(str, 1, STEP2_ENDINGS);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static final List<Map.Entry<String, String>> STEP3_ENDINGS = List.of(
        Map.entry("icate", "ic"),
        Map.entry("ative", ""),
        Map.entry("alize", "al"),
        Map.entry("iciti", "ic"),
        Map.entry("ical", "ic"),
        Map.entry("ful", ""),
        Map.entry("ness", "")
    );

    @SuppressWarnings("SpellCheckingInspection")
    static String step3(String str) {
        return replaceEndingsIfMeasured(str, 1, STEP3_ENDINGS);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static final List<Map.Entry<String, String>> STEP4_ENDINGS = List.of(
        Map.entry("al", ""),
        Map.entry("ance", ""),
        Map.entry("ence", ""),
        Map.entry("er", ""),
        Map.entry("ic", ""),
        Map.entry("able", ""),
        Map.entry("ible", ""),
        Map.entry("ant", ""),
        Map.entry("ement", ""),
        Map.entry("ment", ""),
        Map.entry("ent", ""),
        Map.entry("sion", "s"),
        Map.entry("tion", "t"),
        Map.entry("ou", ""),
        Map.entry("ism", ""),
        Map.entry("ate", ""),
        Map.entry("iti", ""),
        Map.entry("ous", ""),
        Map.entry("ive", ""),
        Map.entry("ize", "")
    );

    static String step4(String str) {
        return replaceEndingsIfMeasured(str, 2, STEP4_ENDINGS);
    }

    static String step5a(String str) {
        if (!str.endsWith("e")) {
            return str;
        }
        int measure = stringMeasure(str, 1);
        // (m > 1) E ->   or   (m = 1 and not *o) E ->
        if (measure > 1 || measure == 1 && !endsWithCVC(str, 1)) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    static String step5b(String str) {
        // (m > 1 and *d and *L) ->
        if (str.endsWith("l") && stringMeasure(str, 1) > 1 && endsWithDoubleConsonant(str)) {
            return str.substring(0, str.length() - 1);
        }
        else {
            return str;
        }
    }

    private static String replaceEndingsIfMeasured(String str, int prefixMinMeasure, List<Map.Entry<String, String>> endingReplacements) {
        for (Map.Entry<String, String> endingReplacement : endingReplacements) {
            String endingBefore = endingReplacement.getKey();
            String endingAfter = endingReplacement.getValue();
            int endingLength = endingBefore.length();
            if (str.endsWith(endingBefore) && stringMeasure(str, endingLength) >= prefixMinMeasure) {
                return str.substring(0, str.length() - endingLength) + endingAfter;
            }
        }
        return str;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static boolean containsVowel(String str, int endCharsToSkip) {
        for (int i = 0, n = str.length() - endCharsToSkip; i < n; i++) {
            char strChar = str.charAt(i);
            if (isVowel(strChar) || strChar == 'y') {
                return true;
            }
        }
        return false;
    }

    private static boolean isOneOf(char c, String chars) {
        return chars.indexOf(c) >= 0;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static boolean isVowel(char c) {
        return isOneOf(c, "aeiou");
    }

    private static boolean isConsonant(char c) {
        return !isVowel(c);
    }

    private static boolean endsWithDoubleConsonant(String str) {
        int n = str.length();
        return n >= 2 && str.charAt(n - 1) == str.charAt(n - 2) && isConsonant(str.charAt(n - 1));
    }

    /**
     * returns a CVC measure for the string
     */
    private static int stringMeasure(String str, int endCharsToSkip) {
        int count = 0;
        boolean vowelSeen = false;
        for (int i = 0, n = str.length() - endCharsToSkip; i < n; i++) {
            char strChar = str.charAt(i);
            if (isVowel(strChar)) {
                vowelSeen = true;
            }
            else if (vowelSeen) {
                count++;
                vowelSeen = false;
            }
        }
        return count;
    }

    private static boolean endsWithCVC(String str, int endCharsToSkip) {
        int n = str.length() - endCharsToSkip;
        return n >= 3
            && isConsonant(str.charAt(n - 1)) && !isOneOf(str.charAt(n - 1), "wxy")
            && isVowel(str.charAt(n - 2))
            && isConsonant(str.charAt(n - 3));
    }
}
