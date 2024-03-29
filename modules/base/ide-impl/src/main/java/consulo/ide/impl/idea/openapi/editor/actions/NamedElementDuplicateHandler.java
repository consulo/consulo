/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.util.lang.Pair;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
@ExtensionImpl
public class NamedElementDuplicateHandler extends EditorWriteActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginal;

  @Inject
  public NamedElementDuplicateHandler() {
    super(true);
  }

  @Override
  protected boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
    return myOriginal.isEnabled(editor, caret, dataContext);
  }

  @RequiredWriteAction
  @Override
  public void executeWriteAction(Editor editor, DataContext dataContext) {
    Project project = editor.getProject();
    if (project != null && !editor.getSelectionModel().hasSelection()) {
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file != null) {
        VisualPosition caret = editor.getCaretModel().getVisualPosition();
        Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcSurroundingRange(editor, caret, caret);
        TextRange toDuplicate = new TextRange(editor.logicalPositionToOffset(lines.first), editor.logicalPositionToOffset(lines.second));

        PsiElement name = findNameIdentifier(editor, file, toDuplicate);
        if (name != null && !name.getTextRange().containsOffset(editor.getCaretModel().getOffset())) {
          editor.getCaretModel().moveToOffset(name.getTextOffset());
        }
      }
    }

    myOriginal.execute(editor, dataContext);
  }

  @Nullable
  private static PsiElement findNameIdentifier(Editor editor, PsiFile file, TextRange toDuplicate) {
    int nonWs = CharArrayUtil.shiftForward(editor.getDocument().getCharsSequence(), toDuplicate.getStartOffset(), "\n\t ");
    PsiElement psi = file.findElementAt(nonWs);
    PsiElement named = null;
    while (psi != null) {
      TextRange range = psi.getTextRange();
      if (range == null || psi instanceof PsiFile || !toDuplicate.contains(psi.getTextRange())) {
        break;
      }
      if (psi instanceof PsiNameIdentifierOwner) {
        named = ((PsiNameIdentifierOwner)psi).getNameIdentifier();
      }
      psi = psi.getParent();
    }
    return named;
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginal = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_DUPLICATE;
  }
}