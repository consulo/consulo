/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.template.impl.editorActions;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.internal.template.TemplateManagerImpl;
import consulo.language.editor.impl.internal.template.TemplateStateImpl;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class TemplateLineStartEndHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalHandler;
  private final boolean myIsHomeHandler;
  private final boolean myWithSelection;

  public TemplateLineStartEndHandler(boolean isHomeHandler, boolean withSelection) {
    super(true);
    myIsHomeHandler = isHomeHandler;
    myWithSelection = withSelection;
  }

  @Override
  protected boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
    TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
    if (templateState != null && !templateState.isFinished()) {
      TextRange range = templateState.getCurrentVariableRange();
      int caretOffset = editor.getCaretModel().getOffset();
      if (range != null && range.containsOffset(caretOffset)) return true;
    }
    return myOriginalHandler.isEnabled(editor, caret, dataContext);
  }

  @Override
  protected void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
    TemplateStateImpl templateState = TemplateManagerImpl.getTemplateStateImpl(editor);
    if (templateState != null && !templateState.isFinished()) {
      TextRange range = templateState.getCurrentVariableRange();
      int caretOffset = editor.getCaretModel().getOffset();
      if (range != null && shouldStayInsideVariable(range, caretOffset)) {
        int selectionOffset = editor.getSelectionModel().getLeadSelectionOffset();
        int offsetToMove = myIsHomeHandler ? range.getStartOffset() : range.getEndOffset();
        LogicalPosition logicalPosition = editor.offsetToLogicalPosition(offsetToMove).leanForward(myIsHomeHandler);
        editor.getCaretModel().moveToLogicalPosition(logicalPosition);
        EditorModificationUtil.scrollToCaret(editor);
        if (myWithSelection) {
          editor.getSelectionModel().setSelection(selectionOffset, offsetToMove);
        }
        else {
          editor.getSelectionModel().removeSelection();
        }
        return;
      }
    }
    myOriginalHandler.execute(editor, caret, dataContext);
  }

  private boolean shouldStayInsideVariable(TextRange varRange, int caretOffset) {
    return varRange.containsOffset(caretOffset) && caretOffset != (myIsHomeHandler ? varRange.getStartOffset() : varRange.getEndOffset());
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }
}
