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

package consulo.language.editor.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.HighlighterIterator;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.BraceMatcher;
import consulo.language.psi.PsiElement;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
public abstract class BraceMatcherBasedSelectioner extends ExtendWordSelectionHandlerBase {

  @Override
  public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
    VirtualFile file = e.getContainingFile().getVirtualFile();
    FileType fileType = file == null? null : file.getFileType();
    if (fileType == null) return super.select(e, editorText, cursorOffset, editor);
    int textLength = editorText.length();
    TextRange totalRange = e.getTextRange();
    HighlighterIterator iterator = editor.getHighlighter().createIterator(totalRange.getStartOffset());
    BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator);

    ArrayList<TextRange> result = new ArrayList<>();
    LinkedList<Trinity<Integer, Integer, IElementType>> stack = new LinkedList<>();
    while (!iterator.atEnd() && iterator.getStart() < totalRange.getEndOffset()) {
      Trinity<Integer, Integer, IElementType> last;
      if (braceMatcher.isLBraceToken(iterator, editorText, fileType)) {
        stack.addLast(Trinity.create(iterator.getStart(), iterator.getEnd(), (IElementType) iterator.getTokenType()));
      }
      else if (braceMatcher.isRBraceToken(iterator, editorText, fileType)
          && !stack.isEmpty() && braceMatcher.isPairBraces((last = stack.getLast()).third, (IElementType)iterator.getTokenType())) {
        stack.removeLast();
        result.addAll(expandToWholeLine(editorText, new TextRange(last.first, iterator.getEnd())));
        int bodyStart = last.second;
        int bodyEnd = iterator.getStart();
        while (bodyStart < textLength && Character.isWhitespace(editorText.charAt(bodyStart))) bodyStart ++;
        while (bodyEnd > 0 && Character.isWhitespace(editorText.charAt(bodyEnd - 1))) bodyEnd --;
        result.addAll(expandToWholeLine(editorText, new TextRange(bodyStart, bodyEnd)));
      }
      iterator.advance();
    }
    result.add(e.getTextRange());
    return result;
  }

}