/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.spellcheker.tokenizer.splitter;

import consulo.document.util.TextRange;
import consulo.language.spellcheker.internal.SpellcheckerStringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdentifierTokenSplitter extends BaseTokenSplitter {
    private static final IdentifierTokenSplitter INSTANCE = new IdentifierTokenSplitter();

    private static final char HIRAGANA_START = '\u3040';
    private static final char HIRAGANA_END = '\u309f';
    private static final char KATAKANA_START = '\u30A0';
    private static final char KATAKANA_END = '\u30ff';
    private static final char CJK_UNIFIED_IDEOGRAPHS_START = '\u4E00';
    private static final char CJK_UNIFIED_IDEOGRAPHS_END = '\u9FFF';
    private static final char CJK_COMPAT_IDEOGRAPHS_START = '\uF900';
    private static final char CJK_COMPAT_IDEOGRAPHS_END = '\uFAFF';
    private static final char HALFWIDTH_AND_FULLWIDTH_FORMS_START = '\uFF00';
    private static final char HALFWIDTH_AND_FULLWIDTH_FORMS_END = '\uFFEF';

    private static final int LETTER_TYPE_MASK = (1 << Character.LOWERCASE_LETTER)
        | (1 << Character.UPPERCASE_LETTER)
        | (1 << Character.TITLECASE_LETTER)
        | (1 << Character.OTHER_LETTER)
        | (1 << Character.MODIFIER_LETTER)
        | (1 << Character.OTHER_PUNCTUATION);

    public static IdentifierTokenSplitter getInstance() {
        return INSTANCE;
    }

    private static final Pattern WORD = Pattern.compile("\\b\\p{L}*'?\\p{L}*");
    private static final Pattern WORD_IN_QUOTES = Pattern.compile("'([^']*)'");

    @Override
    public void split(@Nonnull SplitContext context, @Nonnull TextRange range) {
        if (context.isEmpty() || range.isEmpty() || range.getStartOffset() < 0) {
            return;
        }

        List<TextRange> extracted = excludeByPattern(context, range, WORD_IN_QUOTES, 1);

        String text = context.getText();
        for (TextRange textRange : extracted) {
            List<TextRange> words = splitByCase(text, textRange);

            if (words.isEmpty()) {
                continue;
            }

            if (words.size() == 1) {
                context.addWord(words.get(0));
                continue;
            }

            boolean isCapitalized = SpellcheckerStringUtil.isCapitalized(text, words.get(0));
            boolean containsShortWord = containsShortWord(words);

            if (isCapitalized && containsShortWord) {
                continue;
            }

            boolean isAllWordsAreUpperCased = isAllWordsAreUpperCased(text, words);

            for (TextRange word : words) {
                Matcher matcher = WORD.matcher(text.substring(word.getStartOffset(), word.getEndOffset()));
                if (matcher.find()) {
                    TextRange found = matcherRange(word, matcher);
                    boolean uc = SpellcheckerStringUtil.isUpperCased(text, word);
                    boolean ignore = uc && !isAllWordsAreUpperCased;
                    if (!ignore) {
                        context.addWord(found);
                    }
                }
            }
        }
    }

    @Nonnull
    private static List<TextRange> splitByCase(@Nonnull String text, @Nonnull TextRange range) {
        //System.out.println("text = " + text + " range = " + range);
        List<TextRange> result = new ArrayList<>();
        int i = range.getStartOffset();
        int s = -1;
        int prevType = Character.MATH_SYMBOL;
        while (i < range.getEndOffset()) {
            char ch = text.charAt(i);
            if (HIRAGANA_START <= ch && ch <= HIRAGANA_END
                || KATAKANA_START <= ch && ch <= KATAKANA_END
                || CJK_UNIFIED_IDEOGRAPHS_START <= ch && ch <= CJK_UNIFIED_IDEOGRAPHS_END
                || CJK_COMPAT_IDEOGRAPHS_START <= ch && ch <= CJK_COMPAT_IDEOGRAPHS_END
                || HALFWIDTH_AND_FULLWIDTH_FORMS_START <= ch && ch <= HALFWIDTH_AND_FULLWIDTH_FORMS_END) {
                if (s >= 0) {
                    add(result, i, s);
                    s = -1;
                }
                prevType = Character.MATH_SYMBOL;
                ++i;
                continue;
            }

            int type = Character.getType(ch);
            if (((1 << type) & LETTER_TYPE_MASK) != 0) {
                //letter
                if (s < 0) {
                    //start
                    s = i;
                }
                else if (type == Character.UPPERCASE_LETTER && prevType == Character.LOWERCASE_LETTER) {
                    //a|Camel
                    add(result, i, s);
                    s = i;
                }
                else if (i - s >= 1 && type == Character.LOWERCASE_LETTER && prevType == Character.UPPERCASE_LETTER) {
                    //CAPITALN|ext
                    add(result, i - 1, s);
                    s = i - 1;
                }
            }
            else if (s >= 0) {
                //non-letter
                add(result, i, s);
                s = -1;
            }
            prevType = type;
            i++;
        }
        //remainder
        if (s >= 0) {
            add(result, i, s);
        }
        return result;
    }

    private static void add(List<TextRange> result, int i, int s) {
        if (i - s > 3) {
            TextRange textRange = new TextRange(s, i);
            result.add(textRange);
        }
    }
}
