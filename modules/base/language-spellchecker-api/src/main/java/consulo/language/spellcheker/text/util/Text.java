// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.spellcheker.text.util;

import consulo.document.util.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {
    private Text() {
    }

    public static boolean isNewline(char c) {
        return c == '\n';
    }

    /**
     * @return all non-intersecting occurrences of the given pattern in the given text
     */
    public static List<TextRange> allOccurrences(Pattern pattern, CharSequence text) {
        int start = 0;
        List<TextRange> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (true) {
            if (!matcher.find(start)) {
                break;
            }

            result.add(new TextRange(matcher.start(), matcher.end()));
            start = Math.max(matcher.end(), start + 1);
        }
        return result;
    }
}
