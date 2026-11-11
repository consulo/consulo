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
package consulo.configurable.impl.internal.search;

import org.jspecify.annotations.Nullable;

/**
 * The Porter stemmer of English words - every step trims the word in one buffer, so a word costs no allocation but
 * the buffer and the result.
 */
@SuppressWarnings({"HardCodedStringLiteral", "SpellCheckingInspection"})
public final class PorterStemmerUtil {
    /**
     * The endings of step 2, each followed by its replacement.
     */
    private static final String[] STEP2_ENDINGS = {
        "ational", "ate",
        "tional", "tion",
        "enci", "ence",
        "anci", "ance",
        "izer", "ize",
        "abli", "able",
        "alli", "al",
        "entli", "ent",
        "eli", "e",
        "ousli", "ous",
        "ization", "ize",
        "ation", "ate",
        "ator", "ate",
        "alism", "al",
        "iveness", "ive",
        "fulness", "ful",
        "ousness", "ous",
        "aliti", "al",
        "iviti", "ive",
        "biliti", "ble"
    };

    private static final String[] STEP3_ENDINGS = {
        "icate", "ic",
        "ative", "",
        "alize", "al",
        "iciti", "ic",
        "ical", "ic",
        "ful", "",
        "ness", ""
    };

    private static final String[] STEP4_ENDINGS = {
        "al", "",
        "ance", "",
        "ence", "",
        "er", "",
        "ic", "",
        "able", "",
        "ible", "",
        "ant", "",
        "ement", "",
        "ment", "",
        "ent", "",
        "sion", "s",
        "tion", "t",
        "ou", "",
        "ism", "",
        "ate", "",
        "iti", "",
        "ous", "",
        "ive", "",
        "ize", ""
    };

    private PorterStemmerUtil() {
    }

    /**
     * The stem of the word - a prefix of digits, '-' and '_' kept as it is. Null for an empty word, or one with
     * anything else than letters.
     */
    public static @Nullable String stem(String str) {
        int length = str.length();
        if (length == 0) {
            return null;
        }
        int prefix = 0;
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c) || c == '-' || c == '_') {
                prefix = i + 1;
            }
            else if (!Character.isLetter(c)) {
                return null;
            }
        }
        if (prefix == length) {
            return str;
        }
        StringBuilder word = new StringBuilder(str);
        stem(word, prefix);
        return word.toString();
    }

    /**
     * Stems the part of the word after {@code start}.
     */
    private static void stem(StringBuilder word, int start) {
        step1a(word, start);
        step1b(word, start);
        step1c(word, start);
        replaceEnding(word, start, 1, STEP2_ENDINGS);
        replaceEnding(word, start, 1, STEP3_ENDINGS);
        replaceEnding(word, start, 2, STEP4_ENDINGS);
        step5a(word, start);
        step5b(word, start);
    }

    static String step1a(String str) {
        return apply(str, PorterStemmerUtil::step1a);
    }

    static String step1b(String str) {
        return apply(str, PorterStemmerUtil::step1b);
    }

    static String step1c(String str) {
        return apply(str, PorterStemmerUtil::step1c);
    }

    static String step2(String str) {
        return apply(str, (word, start) -> replaceEnding(word, start, 1, STEP2_ENDINGS));
    }

    static String step3(String str) {
        return apply(str, (word, start) -> replaceEnding(word, start, 1, STEP3_ENDINGS));
    }

    static String step4(String str) {
        return apply(str, (word, start) -> replaceEnding(word, start, 2, STEP4_ENDINGS));
    }

    static String step5a(String str) {
        return apply(str, PorterStemmerUtil::step5a);
    }

    static String step5b(String str) {
        return apply(str, PorterStemmerUtil::step5b);
    }

    private interface Step {
        void apply(StringBuilder word, int start);
    }

    private static String apply(String str, Step step) {
        StringBuilder word = new StringBuilder(str);
        step.apply(word, 0);
        return word.toString();
    }

    private static void step1a(StringBuilder word, int start) {
        // SSES -> SS, IES -> I, SS -> SS, S ->
        if (endsWith(word, start, "sses") || endsWith(word, start, "ies")) {
            word.setLength(word.length() - 2);
        }
        else if (!endsWith(word, start, "ss") && endsWith(word, start, "s")) {
            word.setLength(word.length() - 1);
        }
    }

    private static void step1b(StringBuilder word, int start) {
        if (endsWith(word, start, "eed")) {
            // (m > 0) EED -> EE
            if (measure(word, start, 3) > 0) {
                word.setLength(word.length() - 1);
            }
        }
        else if (endsWith(word, start, "ed") && containsVowel(word, start, 2)) {
            // (*v*) ED ->
            word.setLength(word.length() - 2);
            step1b2(word, start);
        }
        else if (endsWith(word, start, "ing") && containsVowel(word, start, 3)) {
            // (*v*) ING ->
            word.setLength(word.length() - 3);
            step1b2(word, start);
        }
    }

    private static void step1b2(StringBuilder word, int start) {
        if (endsWith(word, start, "at") || endsWith(word, start, "bl") || endsWith(word, start, "iz")) {
            // AT -> ATE, BL -> BLE, IZ -> IZE
            word.append('e');
        }
        else if (endsWithDoubleConsonant(word, start)
            && !(endsWith(word, start, "l") || endsWith(word, start, "s") || endsWith(word, start, "z"))) {
            word.setLength(word.length() - 1);
        }
        else if (measure(word, start, 0) == 1 && endsWithCVC(word, start, 0)) {
            word.append('e');
        }
    }

    private static void step1c(StringBuilder word, int start) {
        // (*v*) Y -> I
        if (endsWith(word, start, "y") && containsVowel(word, start, 1)) {
            word.setCharAt(word.length() - 1, 'i');
        }
    }

    private static void step5a(StringBuilder word, int start) {
        if (!endsWith(word, start, "e")) {
            return;
        }
        // (m > 1) E ->   or   (m = 1 and not *o) E ->
        int measure = measure(word, start, 1);
        if (measure > 1 || measure == 1 && !endsWithCVC(word, start, 1)) {
            word.setLength(word.length() - 1);
        }
    }

    private static void step5b(StringBuilder word, int start) {
        // (m > 1 and *d and *L) ->
        if (endsWith(word, start, "l") && measure(word, start, 1) > 1 && endsWithDoubleConsonant(word, start)) {
            word.setLength(word.length() - 1);
        }
    }

    /**
     * Replaces the first of the endings the word has, when what is before it measures at least {@code minMeasure}.
     */
    private static void replaceEnding(StringBuilder word, int start, int minMeasure, String[] endings) {
        for (int i = 0; i < endings.length; i += 2) {
            String ending = endings[i];
            if (endsWith(word, start, ending) && measure(word, start, ending.length()) >= minMeasure) {
                word.setLength(word.length() - ending.length());
                word.append(endings[i + 1]);
                return;
            }
        }
    }

    private static boolean endsWith(StringBuilder word, int start, String ending) {
        int offset = word.length() - ending.length();
        if (offset < start) {
            return false;
        }
        for (int i = 0; i < ending.length(); i++) {
            if (word.charAt(offset + i) != ending.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsVowel(StringBuilder word, int start, int endCharsToSkip) {
        for (int i = start, n = word.length() - endCharsToSkip; i < n; i++) {
            char c = word.charAt(i);
            if (isVowel(c) || c == 'y') {
                return true;
            }
        }
        return false;
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static boolean endsWithDoubleConsonant(StringBuilder word, int start) {
        int n = word.length();
        return n - start >= 2 && word.charAt(n - 1) == word.charAt(n - 2) && !isVowel(word.charAt(n - 1));
    }

    /**
     * The count of vowel-consonant sequences, the last {@code endCharsToSkip} characters left out.
     */
    private static int measure(StringBuilder word, int start, int endCharsToSkip) {
        int count = 0;
        boolean vowelSeen = false;
        for (int i = start, n = word.length() - endCharsToSkip; i < n; i++) {
            if (isVowel(word.charAt(i))) {
                vowelSeen = true;
            }
            else if (vowelSeen) {
                count++;
                vowelSeen = false;
            }
        }
        return count;
    }

    private static boolean endsWithCVC(StringBuilder word, int start, int endCharsToSkip) {
        int n = word.length() - endCharsToSkip;
        if (n - start < 3) {
            return false;
        }
        char last = word.charAt(n - 1);
        return !isVowel(last) && last != 'w' && last != 'x' && last != 'y'
            && isVowel(word.charAt(n - 2))
            && !isVowel(word.charAt(n - 3));
    }
}
