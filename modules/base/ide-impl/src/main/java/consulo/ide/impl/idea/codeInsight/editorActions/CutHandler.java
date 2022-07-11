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

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.openapi.editor.actionSystem.EditorWriteActionHandler;
import consulo.ide.impl.idea.openapi.editor.actions.CopyAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@ExtensionImpl
public class CutHandler extends EditorWriteActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalHandler;

  @Override
  public void init(EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_CUT;
  }

  @RequiredWriteAction
  @Override
  public void executeWriteAction(final Editor editor, Caret caret, DataContext dataContext) {
    assert caret == null : "Invocation of 'cut' operation for specific caret is not supported";
    Project project = DataManager.getInstance().getDataContext(editor.getContentComponent()).getData(Project.KEY);
    if (project == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, null, dataContext);
      }
      return;
    }

    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

    if (file == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, null, dataContext);
      }
      return;
    }

    final SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection(true)) {
      if (Registry.is(CopyAction.SKIP_COPY_AND_CUT_FOR_EMPTY_SELECTION_KEY)) {
        return;
      }
      editor.getCaretModel().runForEachCaret(new CaretAction() {
        @Override
        public void perform(Caret caret) {
          selectionModel.selectLineAtCaret();
        }
      });
      if (!selectionModel.hasSelection(true)) return;
    }

    int start = selectionModel.getSelectionStart();
    int end = selectionModel.getSelectionEnd();
    final List<TextRange> selections = new ArrayList<TextRange>();
    if (editor.getCaretModel().supportsMultipleCarets()) {
      editor.getCaretModel().runForEachCaret(new CaretAction() {
        @Override
        public void perform(Caret caret) {
          selections.add(new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd()));
        }
      });
    }

    EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_COPY).execute(editor, null, dataContext);

    if (editor.getCaretModel().supportsMultipleCarets()) {

      Collections.reverse(selections);
      final Iterator<TextRange> it = selections.iterator();
      editor.getCaretModel().runForEachCaret(new CaretAction() {
        @Override
        public void perform(Caret caret) {
          TextRange range = it.next();
          editor.getCaretModel().moveToOffset(range.getStartOffset());
          selectionModel.removeSelection();
          editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());
        }
      });
      editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }
    else {
      if (start != end) {
        // There is a possible case that 'sticky selection' is active. It's automatically removed on copying then, so, we explicitly
        // remove the text.
        editor.getDocument().deleteString(start, end);
      }
      else {
        EditorModificationUtil.deleteSelectedText(editor);
      }
    }
  }
}
