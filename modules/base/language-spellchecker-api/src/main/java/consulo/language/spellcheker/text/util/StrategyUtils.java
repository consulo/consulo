// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

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
package consulo.language.spellcheker.text.util;

import consulo.document.util.TextRange;

import java.util.LinkedHashSet;
import java.util.Set;

public final class StrategyUtils {
    private StrategyUtils() {
    }

    /**
     * Finds indent indexes for each line (indent of specific {@code chars})
     * <p>
     * @param str   source text
     * @param chars characters, which considered as indentation
     * @return list of ranges for such indents
     */
    public static Set<TextRange> indentIndexes(CharSequence str, Set<Character> chars) {
        Set<TextRange> result = new LinkedHashSet<>();
        int from = -1;
        for (int index = 0; index < str.length(); index++) {
            char c = str.charAt(index);
            if ((Text.isNewline(c) || (index == 0 && chars.contains(c))) && from == -1) {
                // for first line without \n
                from = index + (Text.isNewline(c) ? 1 : 0);
            }
            else {
                if (from != -1) {
                    if (!chars.contains(c)) {
                        if (index > from) {
                            result.add(new TextRange(from, index));
                        }
                        from = Text.isNewline(c) ? index + 1 : -1;
                    }
                }
            }
        }

        if (from != -1) {
            result.add(new TextRange(from, str.length()));
        }

        return result;
    }
}
