/*
 * Copyright 2013-2025 consulo.io
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
package consulo.util.lang.text;

import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.StringUtil;

import java.util.function.IntPredicate;

/**
 * @author max
 */
public class TrigramBuilder {
    private TrigramBuilder() {
    }

    public static boolean processTrigrams(CharSequence text, TrigramProcessor consumer) {
        AddonlyIntSet set = new AddonlyIntSet();
        int index = 0;
        char[] fileTextArray = CharArrayUtil.fromSequenceWithoutCopying(text);

        ScanWordsLoop:
        while (true) {
            while (true) {
                if (index == text.length()) {
                    break ScanWordsLoop;
                }
                char c = fileTextArray != null ? fileTextArray[index] : text.charAt(index);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
                    Character.isJavaIdentifierPart(c)) {
                    break;
                }
                index++;
            }
            int identifierStart = index;
            while (true) {
                index++;
                if (index == text.length()) {
                    break;
                }
                char c = fileTextArray != null ? fileTextArray[index] : text.charAt(index);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                    continue;
                }
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }
            }

            int tc1 = 0;
            int tc2 = 0;
            int tc3;
            for (int i = identifierStart, iters = 0; i < index; ++i, ++iters) {
                char c = StringUtil.toLowerCase(fileTextArray != null ? fileTextArray[i] : text.charAt(i));
                tc3 = (tc2 << 8) + c;
                tc2 = (tc1 << 8) + c;
                tc1 = c;

                if (iters >= 2) {
                    set.add(tc3);
                }
            }
        }

        return consumer.consumeTrigramsCount(set.size()) && set.forEach(consumer);
    }

    public static abstract class TrigramProcessor implements IntPredicate {
        public boolean consumeTrigramsCount(int count) {
            return true;
        }
    }
}
