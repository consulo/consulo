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

package consulo.language.editor.moveUpDown;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;

public class LineMover extends StatementUpDownMover {

  @Override
  public boolean checkAvailable(@Nonnull final Editor editor, @Nonnull final PsiFile file, @Nonnull final MoveInfo info, final boolean down) {
    LineRange range = StatementUpDownMover.getLineRangeFromSelection(editor);

    LogicalPosition maxLinePos = editor.offsetToLogicalPosition(editor.getDocument().getTextLength());
    int maxLine = maxLinePos.column == 0 ? maxLinePos.line : maxLinePos.line + 1;
    if (range.startLine == 0 && !down) return false;
    if (range.endLine >= maxLine && down) return false;

    int nearLine = down ? range.endLine : range.startLine - 1;
    info.toMove = range;
    info.toMove2 = new LineRange(nearLine, nearLine + 1);

    return true;
  }

  protected static Pair<PsiElement, PsiElement> getElementRange(final PsiElement parent, PsiElement element1, PsiElement element2) {
    if (PsiTreeUtil.isAncestor(element1, element2, false) || PsiTreeUtil.isAncestor(element2, element1, false)) {
      return Pair.create(parent, parent);
    }
    // find nearset children that are parents of elements
    while (element1 != null && element1.getParent() != parent) {
      element1 = element1.getParent();
    }
    while (element2 != null && element2.getParent() != parent) {
      element2 = element2.getParent();
    }
    if (element1 == null || element2 == null) return null;
    if (element1 != element2) {
      assert element1.getTextRange().getEndOffset() <= element2.getTextRange().getStartOffset() : element1.getTextRange() + "-" + element2.getTextRange() + element1 + element2;
    }
    return Pair.create(element1, element2);
  }
}
