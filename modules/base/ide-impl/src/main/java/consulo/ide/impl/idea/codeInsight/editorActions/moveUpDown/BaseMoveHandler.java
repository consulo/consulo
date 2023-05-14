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
package consulo.ide.impl.idea.codeInsight.editorActions.moveUpDown;

import consulo.language.ast.ASTNode;
import consulo.language.DependentLanguage;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.language.editor.moveUpDown.LineRange;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.util.PsiUtilBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.annotation.access.RequiredWriteAction;

/**
 * @author Dennis.Ushakov
 */
public abstract class BaseMoveHandler extends EditorWriteActionHandler {
  protected final boolean isDown;

  public BaseMoveHandler(boolean down) {
    super(true);
    isDown = down;
  }

  @RequiredWriteAction
  @Override
  public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
    final Project project = editor.getProject();
    assert project != null;
    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    final Document document = editor.getDocument();
    PsiFile file = getRoot(documentManager.getPsiFile(document), editor);

    if (file != null) {
      final MoverWrapper mover = getSuitableMover(editor, file);
      if (mover != null && mover.getInfo().toMove2 != null) {
        LineRange range = mover.getInfo().toMove;
        if ((range.startLine > 0 || isDown) && (range.endLine < document.getLineCount() || !isDown)) {
          mover.move(editor, file);
        }
      }
    }
  }

  @Override
  public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
    if (editor.isViewer() || editor.isOneLineMode()) return false;
    final Project project = editor.getProject();
    if (project == null || project.isDisposed()) return false;
    return true;
  }

  @Nullable
  protected abstract MoverWrapper getSuitableMover(@Nonnull Editor editor, @Nonnull PsiFile file);

  @Nullable
  private static PsiFile getRoot(final PsiFile file, final Editor editor) {
    if (file == null) return null;
    int offset = editor.getCaretModel().getOffset();
    if (offset == editor.getDocument().getTextLength()) offset--;
    if (offset<0) return null;
    PsiElement leafElement = file.findElementAt(offset);
    if (leafElement == null) return null;
    if (leafElement.getLanguage() instanceof DependentLanguage) {
      leafElement = file.getViewProvider().findElementAt(offset, file.getViewProvider().getBaseLanguage());
      if (leafElement == null) return null;
    }
    ASTNode node = leafElement.getNode();
    if (node == null) return null;
    return (PsiFile)PsiUtilBase.getRoot(node).getPsi();
  }
}