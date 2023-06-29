/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.editor.impl.view;

import consulo.codeEditor.impl.FontInfo;

import java.awt.*;
import java.util.List;

class TextFragmentFactory {
  static void createTextFragments(List<LineFragment> fragments,
                                  char[] lineChars,
                                  int start,
                                  int end,
                                  boolean isRtl,
                                  FontInfo fontInfo) {
    boolean needsLayout = isRtl || fontInfo.getFont().hasLayoutAttributes();
    boolean nonLatinText = false;
    if (!needsLayout && (containsSurrogatePairs(lineChars, start, end) || Font.textRequiresLayout(lineChars, start, end))) {
      needsLayout = true;
      nonLatinText = true;
    }
    if (needsLayout) {
      int lastOffset = start;
      if (nonLatinText || containsNonLatinText(lineChars, start, end)) {
        // Split text by scripts. JDK does this as well inside 'Font.layoutGlyphVector',
        // but doing it here effectively disables brace matching logic in 'layoutGlyphVector',
        // which breaks ligatures in some cases (see JBR-10).
        Character.UnicodeScript lastScript = Character.UnicodeScript.COMMON;
        for (int i = start; i < end; i++) {
          int c = Character.codePointAt(lineChars, i, end);
          if (Character.isSupplementaryCodePoint(c)) {
            //noinspection AssignmentToForLoopParameter
            i++;
          }
          Character.UnicodeScript script = Character.UnicodeScript.of(c);
          if (script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.INHERITED && script != Character.UnicodeScript.UNKNOWN) {
            if (lastScript != script && lastScript != Character.UnicodeScript.COMMON) {
              fragments.add(new ComplexTextFragment(lineChars, lastOffset, i, isRtl, fontInfo));
              lastOffset = i;
            }
            lastScript = script;
          }
        }
      }
      fragments.add(new ComplexTextFragment(lineChars, lastOffset, end, isRtl, fontInfo));
    }
    else {
      fragments.add(new SimpleTextFragment(lineChars, start, end, fontInfo));
    }
  }

  private static boolean containsSurrogatePairs(char[] chars, int start, int end) {
    end--; // no need to check last character for high surrogate
    for (int i = start; i < end; i++) {
      if (Character.isHighSurrogate(chars[i]) && Character.isLowSurrogate(chars[i + 1])) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsNonLatinText(char[] chars, int start, int end) {
    for (int i = start; i < end; i++) {
      if (chars[i] >= 0x2ea /* first non-Latin code point */) return true;
    }
    return false;
  }
}
