/*
 * Copyright 2013-2026 consulo.io
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
package consulo.configurable.search;

/**
 * What the stemmers of the Snowball family share - a word is stemmed by cutting the longest of some endings, each
 * looked for only inside a region of the word:
 * <ul>
 *     <li>{@link #rv RV} - after the first vowel;</li>
 *     <li>{@link #r1 R1} - after the first consonant which follows a vowel;</li>
 *     <li>R2 - R1 of R1.</li>
 * </ul>
 * A stemmer lists its endings and its steps; the matching is here.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
public abstract class SuffixSearchStemmer implements SearchStemmer {
    /**
     * The start of RV - after the first vowel, the length of the word when it has none.
     */
    protected static int rv(CharSequence word, String vowels) {
        for (int i = 0; i < word.length(); i++) {
            if (vowels.indexOf(word.charAt(i)) >= 0) {
                return i + 1;
            }
        }
        return word.length();
    }

    /**
     * The start of R1 of the part of the word from {@code start} - after the first consonant which follows a vowel,
     * the length of the word when there is none. {@code r1(word, r1(word, 0, vowels), vowels)} is R2.
     */
    protected static int r1(CharSequence word, int start, String vowels) {
        for (int i = start + 1; i < word.length(); i++) {
            if (vowels.indexOf(word.charAt(i)) < 0 && vowels.indexOf(word.charAt(i - 1)) >= 0) {
                return i + 1;
            }
        }
        return word.length();
    }

    /**
     * The length of the longest of the endings the word has inside the region from {@code region} - 0 for none.
     */
    protected static int longestEnding(CharSequence word, int region, String... endings) {
        int longest = 0;
        for (String ending : endings) {
            if (ending.length() > longest && endsWith(word, region, ending)) {
                longest = ending.length();
            }
        }
        return longest;
    }

    /**
     * As {@link #longestEnding}, the ending right after one of the {@code preceding} letters, which is inside the
     * region too.
     */
    protected static int longestEndingAfter(CharSequence word, int region, String preceding, String... endings) {
        int longest = 0;
        for (String ending : endings) {
            int before = word.length() - ending.length() - 1;
            if (ending.length() > longest && before >= region && preceding.indexOf(word.charAt(before)) >= 0
                && endsWith(word, region, ending)) {
                longest = ending.length();
            }
        }
        return longest;
    }

    /**
     * Cuts the longest of the endings inside the region - whether one was cut.
     */
    protected static boolean removeLongestEnding(StringBuilder word, int region, String... endings) {
        return cut(word, longestEnding(word, region, endings));
    }

    /**
     * Cuts {@code length} characters off the end of the word - whether there were any.
     */
    protected static boolean cut(StringBuilder word, int length) {
        if (length <= 0) {
            return false;
        }
        word.setLength(word.length() - length);
        return true;
    }

    /**
     * Whether the word ends with the ending, the ending all inside the region.
     */
    protected static boolean endsWith(CharSequence word, int region, String ending) {
        int offset = word.length() - ending.length();
        if (offset < region || offset < 0) {
            return false;
        }
        for (int i = 0; i < ending.length(); i++) {
            if (word.charAt(offset + i) != ending.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
