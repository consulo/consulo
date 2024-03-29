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

package consulo.ide.impl.idea.codeInsight;

import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.internal.IndentData;
import consulo.document.Document;
import consulo.language.psi.PsiFile;
import consulo.language.util.CommentUtilCore;
import consulo.ide.impl.idea.util.text.CharArrayUtil;

import jakarta.annotation.Nonnull;

public class CommentUtil extends CommentUtilCore {
  private CommentUtil() { }

  public static IndentData getMinLineIndent(Document document, int line1, int line2, @Nonnull PsiFile file) {
    CharSequence chars = document.getCharsSequence();
    IndentData minIndent = null;
    for (int line = line1; line <= line2; line++) {
      int lineStart = document.getLineStartOffset(line);
      int textStart = CharArrayUtil.shiftForward(chars, lineStart, " \t");
      if (textStart >= document.getTextLength()) {
        textStart = document.getTextLength();
      }
      else {
        char c = chars.charAt(textStart);
        if (c == '\n' || c == '\r') continue; // empty line
      }
      IndentData indent = IndentData.createFrom(chars, lineStart, textStart, CodeStyle.getIndentOptions(file).TAB_SIZE);
      minIndent = IndentData.min(minIndent, indent);
    }
    if (minIndent == null && line1 == line2 && line1 < document.getLineCount() - 1) {
      return getMinLineIndent(document, line1 + 1, line1 + 1, file);
    }
    return minIndent;
  }
}
