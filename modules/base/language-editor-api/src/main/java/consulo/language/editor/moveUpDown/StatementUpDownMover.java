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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.util.lang.Pair;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.SelectionModel;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.ast.ASTNode;
import consulo.document.Document;
import consulo.document.RangeMarker;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class StatementUpDownMover {
  public static final ExtensionPointName<StatementUpDownMover> STATEMENT_UP_DOWN_MOVER_EP = ExtensionPointName.create(StatementUpDownMover.class);

  public static class MoveInfo {
    /** Source line range */
    @Nonnull
    public LineRange toMove;

    /**
     * Target line range, or <code>null</code> if move not available
     * @see #prohibitMove()
     */
    public LineRange toMove2;

    public RangeMarker range1;
    public RangeMarker range2;

    public boolean indentSource;
    public boolean indentTarget = true;

    /**
     * Use this method in {@link StatementUpDownMover#checkAvailable(Editor, PsiFile, StatementUpDownMover.MoveInfo, boolean)}
     * @return true to suppress further movers processing
     */
    public final boolean prohibitMove() {
      toMove2 = null;
      return true;
    }
  }

  public abstract boolean checkAvailable(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull MoveInfo info, boolean down);

  public void beforeMove(@Nonnull Editor editor, @Nonnull MoveInfo info, boolean down) {
  }

  public void afterMove(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull MoveInfo info, boolean down) {
  }

  public static int getLineStartSafeOffset(@Nonnull Document document, int line) {
    if (line == document.getLineCount()) return document.getTextLength();
    return document.getLineStartOffset(line);
  }

  @Nonnull
  protected static LineRange getLineRangeFromSelection(@Nonnull Editor editor) {
    int startLine;
    int endLine;
    SelectionModel selectionModel = editor.getSelectionModel();
    LineRange range;
    if (selectionModel.hasSelection()) {
      startLine = editor.offsetToLogicalPosition(selectionModel.getSelectionStart()).line;
      LogicalPosition endPos = editor.offsetToLogicalPosition(selectionModel.getSelectionEnd());
      endLine = endPos.column == 0 ? endPos.line : endPos.line+1;
      range = new LineRange(startLine, endLine);
    }
    else {
      startLine = editor.getCaretModel().getLogicalPosition().line;
      endLine = startLine+1;
      range = new LineRange(startLine, endLine);
    }
    return range;
  }

  @Nullable
  protected static Pair<PsiElement, PsiElement> getElementRange(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull LineRange range) {
    int startOffset = editor.logicalPositionToOffset(new LogicalPosition(range.startLine, 0));
    PsiElement startingElement = firstNonWhiteElement(startOffset, file, true);
    if (startingElement == null) return null;
    int endOffset = editor.logicalPositionToOffset(new LogicalPosition(range.endLine, 0)) -1;

    PsiElement endingElement = firstNonWhiteElement(endOffset, file, false);
    if (endingElement == null) return null;
    if (PsiTreeUtil.isAncestor(startingElement, endingElement, false) ||
        startingElement.getTextRange().getEndOffset() <= endingElement.getTextRange().getStartOffset()) {
      return Pair.create(startingElement, endingElement);
    }
    if (PsiTreeUtil.isAncestor(endingElement, startingElement, false)) {
      return Pair.create(startingElement, endingElement);
    }
    return null;
  }

  @Nullable
  protected static PsiElement firstNonWhiteElement(int offset, @Nonnull PsiFile file, boolean lookRight) {
    ASTNode leafElement = file.getNode().findLeafElementAt(offset);
    return leafElement == null ? null : firstNonWhiteElement(leafElement.getPsi(), lookRight);
  }

  @Nullable
  protected static PsiElement firstNonWhiteElement(PsiElement element, boolean lookRight) {
    if (element instanceof PsiWhiteSpace) {
      element = lookRight ? element.getNextSibling() : element.getPrevSibling();
    }
    return element;
  }
}
