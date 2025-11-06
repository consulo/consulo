/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import jakarta.annotation.Nonnull;
import org.jdom.Verifier;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlainTextTokenSplitter extends BaseTokenSplitter {
    private static final PlainTextTokenSplitter INSTANCE = new PlainTextTokenSplitter();

    public static PlainTextTokenSplitter getInstance() {
        return INSTANCE;
    }

    private static final Pattern SPLIT_PATTERN = Pattern.compile("(\\s)");
    private static final Pattern MAIL = Pattern.compile("([\\p{L}0-9.\\-_]+@([\\p{L}0-9\\-_]+\\.)+(com|net|[a-z]{2}))");
    private static final Pattern URL = Pattern.compile("((?>file|ftp|https?)://([^/]+)(/\\w*)?(/\\w*))");

    @Override
    public void split(@Nonnull SplitContext context, @Nonnull TextRange range) {
        if (context.isEmpty()) {
            return;
        }
        String text = context.getText();
        String substring = range.substring(text);
        if (Verifier.checkCharacterData(substring) != null) {
            return;
        }
        //for (int i = 0; i < text.length(); ++i) {
        //    char ch = text.charAt(i);
        //    if (ch >= '\u3040' && ch <= '\u309f' || // Hiragana
        //        ch >= '\u30A0' && ch <= '\u30ff' || // Katakana
        //        ch >= '\u4E00' && ch <= '\u9FFF' || // CJK Unified ideographs
        //        ch >= '\uF900' && ch <= '\uFAFF' || // CJK Compatibility Ideographs
        //        ch >= '\uFF00' && ch <= '\uFFEF' //Half-width and Full-width Forms of Katakana & Full-width ASCII variants
        //    ) {
        //        return;
        //    }
        //}

        TextTokenSplitter ws = TextTokenSplitter.getInstance();
        int from = range.getStartOffset();
        int till;
        Matcher matcher = SPLIT_PATTERN.matcher(range.substring(text));
        while (true) {
            context.checkCanceled();
            List<TextRange> toCheck;
            TextRange wRange;
            String word;
            if (matcher.find()) {
                TextRange found = matcherRange(range, matcher);
                till = found.getStartOffset();
                if (badSize(from, till)) {
                    continue;
                }
                wRange = new TextRange(from, till);
                word = wRange.substring(text);
                from = found.getEndOffset();
            }
            else { // end hit or zero matches
                wRange = new TextRange(from, range.getEndOffset());
                word = wRange.substring(text);
            }
            if (word.contains("@")) {
                toCheck = excludeByPattern(context, wRange, MAIL, 0);
            }
            else if (word.contains("://")) {
                toCheck = excludeByPattern(context, wRange, URL, 0);
            }
            else {
                toCheck = Collections.singletonList(wRange);
            }
            for (TextRange r : toCheck) {
                ws.split(context, r);
            }
            if (matcher.hitEnd()) {
                break;
            }
        }
    }
}
